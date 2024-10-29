package mthiessen.protocol.algorithms;

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
import mthiessen.protocol.commit.CommitRequestHandler;
import mthiessen.protocol.forward.ForwardRMWRequestHandler;
import mthiessen.protocol.leaderelection.ILeaderElectionService;
import mthiessen.protocol.leaderelection.LeaderElectionOp;
import mthiessen.protocol.leaderelection.StaticLeaderElectionService;
import mthiessen.protocol.prepare.PrepareRequestHandler;
import mthiessen.protocol.prepare.PrepareResponseHandler;
import mthiessen.protocol.reads.ConflictAwareEAGReads;
import mthiessen.protocol.reads.EAGReads;
import mthiessen.protocol.reads.IReadOp;
import mthiessen.protocol.reads.conflictdetection.IConflictDetection;
import mthiessen.protocol.replication.IReplicationOp;
import mthiessen.protocol.replication.Replication;
import mthiessen.protocol.state.process.IProcessState;
import mthiessen.protocol.state.process.ProcessState;
import mthiessen.protocol.state.proposer.IProposerState;
import mthiessen.protocol.state.proposer.ProposerState;

public class EAG extends BaseProcess {

  @NonNull private final IProcessState processState;

  @NonNull private final IStateMachine stateMachine;

  public EAG(
      final @NonNull IRouter router,
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull Runnable leaderElectionOp,
      final @NonNull ILeaderElectionService leaderElectionService,
      final @NonNull IProposerState proposerState,
      final @NonNull IReplicationOp replicationOp,
      final @NonNull IReadOp readOp,
      final @NonNull IProcessState processState,
      final @NonNull IStateMachine stateMachine) {
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
  }

  public static IProcess generate(
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final IConflictDetection conflictDetection) {
    ILeaderElectionService leaderElectionService = new StaticLeaderElectionService(leader, router);
    IProposerState proposerState = new ProposerState();
    IProcessState processState = new ProcessState();
    Runnable leaderElectionOp = new LeaderElectionOp(leaderElectionService, proposerState);
    IReplicationOp replicationOp = new Replication(router, proposerState);

    IReadOp readOp;
    if (conflictDetection != IConflictDetection.NOOP) {
      readOp =
          new ConflictAwareEAGReads(proposerState, processState, stateMachine, conflictDetection);
    } else {
      readOp = new EAGReads(proposerState, processState, stateMachine);
    }

    IMeasurementCollector measurementCollector =
        new MeasurementCollector(
            router.getRouterState().getIdentifier(), new UniqueNumberGenerator());

    return new EAG(
        router,
        measurementCollector,
        leaderElectionOp,
        leaderElectionService,
        proposerState,
        replicationOp,
        readOp,
        processState,
        stateMachine);
  }

  @Override
  public void initialize() {
    IRouterState routerState = super.router.getRouterState();

    IRequestHandler writeRequestHandler =
        new PrepareRequestHandler(this.measurementCollector, this.processState);
    routerState.registerRequestHandlerForOp(OPS.PREPARE, () -> writeRequestHandler);

    int numProcesses = super.router.getAllRegisteredRoutes().size();
    // Needs to be unique per broadcast id.
    routerState.registerResponseHandleForOp(
        OPS.PREPARE,
        () -> new PrepareResponseHandler(numProcesses, numProcesses, this.measurementCollector));

    IRequestHandler commitRequestHandler =
        new CommitRequestHandler(
            this.processState,
            routerState.getIdentifier(),
            this.stateMachine,
            new CommitCache(),
            this.measurementCollector);
    routerState.registerRequestHandlerForOp(OPS.COMMIT, () -> commitRequestHandler);

    IRequestHandler forwardWriteRequestHandler =
        new ForwardRMWRequestHandler(super.replicationOp, super.measurementCollector);
    routerState.registerRequestHandlerForOp(OPS.FORWARD_RMW, () -> forwardWriteRequestHandler);
  }
}
