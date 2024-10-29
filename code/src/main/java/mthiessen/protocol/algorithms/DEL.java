package mthiessen.protocol.algorithms;

import lombok.Getter;
import lombok.NonNull;
import mthiessen.IProcess;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.inmemory.MeasurementCollector;
import mthiessen.misc.UniqueNumberGenerator;
import mthiessen.network.IRequestHandler;
import mthiessen.network.IRouter;
import mthiessen.network.IRouterState;
import mthiessen.protocol.OPS;
import mthiessen.protocol.commit.CommitCache;
import mthiessen.protocol.commit.DELCommitRequestHandler;
import mthiessen.protocol.forward.ForwardRMWRequestHandler;
import mthiessen.protocol.leaderelection.ILeaderElectionService;
import mthiessen.protocol.leaderelection.LeaderElectionOp;
import mthiessen.protocol.leaderelection.StaticLeaderElectionService;
import mthiessen.protocol.prepare.DELPrepareRequestHandler;
import mthiessen.protocol.prepare.PrepareResponseHandler;
import mthiessen.protocol.reads.ConflictAwareFutureWriteReads;
import mthiessen.protocol.reads.FutureWriteReads;
import mthiessen.protocol.reads.IReadOp;
import mthiessen.protocol.reads.conflictdetection.IConflictDetection;
import mthiessen.protocol.replication.DELReplication;
import mthiessen.protocol.replication.IReplicationOp;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.proposer.IProposerState;
import mthiessen.protocol.state.proposer.ProposerState;

public class DEL extends BaseProcess {

  @NonNull private final FutureWriteProcessState processState;
  @NonNull private final IStateMachine stateMachine;
  private final int delta;
  @Getter private DELCommitRequestHandler commitRequestHandler;

  public DEL(
      final @NonNull IRouter router,
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull Runnable leaderElectionOp,
      final @NonNull ILeaderElectionService leaderElectionService,
      final @NonNull IProposerState proposerState,
      final @NonNull IReplicationOp replicationOp,
      final @NonNull IReadOp readOp,
      final @NonNull FutureWriteProcessState processState,
      final @NonNull IStateMachine stateMachine,
      final int delta) {
    super(
        router,
        measurementCollector,
        leaderElectionOp,
        leaderElectionService,
        proposerState,
        processState,
        replicationOp,
        readOp);
    this.processState = processState;
    this.stateMachine = stateMachine;
    this.delta = delta;
  }

  public static IProcess generate(
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final IConflictDetection conflictDetection,
      final int alpha,
      final int delta) {
    ILeaderElectionService leaderElectionService = new StaticLeaderElectionService(leader, router);
    IProposerState proposerState = new ProposerState();
    FutureWriteProcessState processState = new FutureWriteProcessState();
    Runnable leaderElectionOp = new LeaderElectionOp(leaderElectionService, proposerState);
    IReplicationOp replicationOp = new DELReplication(router, proposerState, alpha, delta);

    IReadOp readOp;
    if (conflictDetection != IConflictDetection.NOOP) {
      readOp = new ConflictAwareFutureWriteReads(processState, stateMachine, conflictDetection);
    } else {
      readOp = new FutureWriteReads(processState, stateMachine);
    }

    IMeasurementCollector measurementCollector =
        new MeasurementCollector(
            router.getRouterState().getIdentifier(), new UniqueNumberGenerator());

    return new DEL(
        router,
        measurementCollector,
        leaderElectionOp,
        leaderElectionService,
        proposerState,
        replicationOp,
        readOp,
        processState,
        stateMachine,
        delta);
  }

  @Override
  public void initialize() {
    IRouterState routerState = super.router.getRouterState();

    IRequestHandler writeRequestHandler =
        new DELPrepareRequestHandler(this.measurementCollector, this.processState);
    routerState.registerRequestHandlerForOp(OPS.PREPARE, () -> writeRequestHandler);

    int numProcesses = super.router.getAllRegisteredRoutes().size();
    // Needs to be unique per broadcast id.
    routerState.registerResponseHandleForOp(
        OPS.PREPARE,
        () -> new PrepareResponseHandler(numProcesses, numProcesses, this.measurementCollector));

    this.commitRequestHandler =
        new DELCommitRequestHandler(
            this.processState,
            routerState.getIdentifier(),
            this.stateMachine,
            new CommitCache(),
            this.measurementCollector,
            this.delta);
    routerState.registerRequestHandlerForOp(OPS.COMMIT, () -> commitRequestHandler);

    IRequestHandler forwardWriteRequestHandler =
        new ForwardRMWRequestHandler(super.replicationOp, super.measurementCollector);
    routerState.registerRequestHandlerForOp(OPS.FORWARD_RMW, () -> forwardWriteRequestHandler);
  }
}
