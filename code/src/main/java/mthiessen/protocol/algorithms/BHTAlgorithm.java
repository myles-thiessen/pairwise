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
import mthiessen.protocol.commit.BHTCommitRequestHandler;
import mthiessen.protocol.commit.CommitCache;
import mthiessen.protocol.leaderelection.ILeaderElectionService;
import mthiessen.protocol.leaderelection.LeaderElectionOp;
import mthiessen.protocol.leaderelection.StaticLeaderElectionService;
import mthiessen.protocol.reads.IReadOp;
import mthiessen.protocol.reads.PromiseReads;
import mthiessen.protocol.replication.BHTReplication;
import mthiessen.protocol.replication.IReplicationOp;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.proposer.IProposerState;
import mthiessen.protocol.state.proposer.ProposerState;
import mthiessen.protocol.write.BHTWriteRequestHandler;
import mthiessen.protocol.write.ForwardWriteRequestHandler;
import mthiessen.protocol.write.ForwardWriteResponseHandler;
import mthiessen.protocol.write.WriteResponseHandler;

public class BHTAlgorithm extends BaseProcess {

  @NonNull
  private final FutureWriteProcessState processState;
  @NonNull
  private final IStateMachine stateMachine;

  private final int delta;

  public BHTAlgorithm(
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
      final int alpha,
      final int delta) {
    ILeaderElectionService leaderElectionService =
        new StaticLeaderElectionService(leader, router);
    IProposerState proposerState = new ProposerState();
    FutureWriteProcessState processState = new FutureWriteProcessState();
    Runnable leaderElectionOp = new LeaderElectionOp(leaderElectionService,
        proposerState);
    IReplicationOp replicationOp = new BHTReplication(router, proposerState,
        alpha, delta);

    IReadOp readOp = new PromiseReads(processState, stateMachine);

    IMeasurementCollector measurementCollector =
        new MeasurementCollector(
            router.getRouterState().getIdentifier(),
            new UniqueNumberGenerator());

    return new BHTAlgorithm(
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
        new BHTWriteRequestHandler(this.measurementCollector,
            this.processState);
    routerState.registerRequestHandlerForOp(OPS.WRITE,
        () -> writeRequestHandler);

    int numProcesses = super.router.getAllRegisteredRoutes().size();
    // Needs to be unique per broadcast id.
    routerState.registerResponseHandleForOp(
        OPS.WRITE,
        () -> new WriteResponseHandler(numProcesses, numProcesses, 0,
            this.measurementCollector));

    IRequestHandler commitRequestHandler =
        new BHTCommitRequestHandler(
            this.processState,
            this.stateMachine,
            new CommitCache(),
            this.measurementCollector,
            this.delta);
    routerState.registerRequestHandlerForOp(OPS.COMMIT,
        () -> commitRequestHandler);

    IRequestHandler forwardWriteRequestHandler =
        new ForwardWriteRequestHandler(super.replicationOp,
            super.measurementCollector);
    routerState.registerRequestHandlerForOp(OPS.FORWARD_WRITE,
        () -> forwardWriteRequestHandler);

    // Needs to be unique per broadcast id.
    routerState.registerResponseHandleForOp(OPS.FORWARD_WRITE,
        ForwardWriteResponseHandler::new);
  }
}
