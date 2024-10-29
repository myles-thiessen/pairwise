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
import mthiessen.protocol.commit.StartTimeCommitRequestHandler;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.eventschedulingprimitive.MarkerManager;
import mthiessen.protocol.eventschedulingprimitive.SetMarkerRequestHandler;
import mthiessen.protocol.eventschedulingprimitive.SetMarkerResponseHandler;
import mthiessen.protocol.leaderelection.ILeaderElectionService;
import mthiessen.protocol.leaderelection.LeaderElectionOp;
import mthiessen.protocol.leaderelection.StaticLeaderElectionService;
import mthiessen.protocol.reads.IReadOp;
import mthiessen.protocol.reads.PromiseReads;
import mthiessen.protocol.replication.IReplicationOp;
import mthiessen.protocol.replication.US2Replication;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.process.StartTimeManager;
import mthiessen.protocol.state.proposer.IProposerState;
import mthiessen.protocol.state.proposer.ProposerState;
import mthiessen.protocol.state.writerecorder.IWriteRecorder;
import mthiessen.protocol.state.writerecorder.WriteRecorder;
import mthiessen.protocol.stopped.StoppedRequestHandler;
import mthiessen.protocol.write.ForwardWriteRequestHandler;
import mthiessen.protocol.write.ForwardWriteResponseHandler;
import mthiessen.protocol.write.US2WriteRequestHandler;
import mthiessen.protocol.write.WriteResponseHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class US2Algorithm extends BaseProcess {
  private static final ScheduledExecutorService markerRenewal =
      Executors.newScheduledThreadPool(1);

  @NonNull
  private final FutureWriteProcessState processState;
  @NonNull
  private final IStateMachine stateMachine;
  @NonNull
  private final MarkerManager markerManager;
  @NonNull
  private final EventSchedulingPrimitive eventSchedulingPrimitive;

  public US2Algorithm(
      final @NonNull IRouter router,
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull Runnable leaderElectionOp,
      final @NonNull ILeaderElectionService leaderElectionService,
      final @NonNull IProposerState proposerState,
      final @NonNull IReplicationOp replicationOp,
      final @NonNull IReadOp readOp,
      final @NonNull FutureWriteProcessState processState,
      final @NonNull IStateMachine stateMachine,
      final @NonNull MarkerManager markerManager,
      final @NonNull EventSchedulingPrimitive eventSchedulingPrimitive) {
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
    this.markerManager = markerManager;
    this.eventSchedulingPrimitive = eventSchedulingPrimitive;
  }

  public static IProcess generate(
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final LowerBounds lowerBounds,
      final int alpha) {
    ILeaderElectionService leaderElectionService =
        new StaticLeaderElectionService(leader, router);
    IProposerState proposerState = new ProposerState();
    FutureWriteProcessState processState = new FutureWriteProcessState();
    Runnable leaderElectionOp = new LeaderElectionOp(leaderElectionService,
        proposerState);
    MarkerManager markerManager = new MarkerManager(router);
    EventSchedulingPrimitive eventSchedulingPrimitive =
        new EventSchedulingPrimitive(
            markerManager, lowerBounds,
            router.getRouterState().getIdentifier());
    IReplicationOp replicationOp =
        new US2Replication(router, proposerState, eventSchedulingPrimitive,
            processState, alpha);

    IReadOp readOp = new PromiseReads(processState, stateMachine);

    IMeasurementCollector measurementCollector =
        new MeasurementCollector(
            router.getRouterState().getIdentifier(),
            new UniqueNumberGenerator());

    return new US2Algorithm(
        router,
        measurementCollector,
        leaderElectionOp,
        leaderElectionService,
        proposerState,
        replicationOp,
        readOp,
        processState,
        stateMachine,
        markerManager,
        eventSchedulingPrimitive);
  }

  @Override
  public void initialize() {
    IRouterState routerState = super.router.getRouterState();

    IWriteRecorder writeRecorder = new WriteRecorder();
    IRequestHandler writeRequestHandler =
        new US2WriteRequestHandler(
            this.measurementCollector,
            this.router,
            this.processState,
            this.eventSchedulingPrimitive,
            writeRecorder);
    routerState.registerRequestHandlerForOp(OPS.WRITE,
        () -> writeRequestHandler);

    int numProcesses = super.router.getAllRegisteredRoutes().size();
    // Needs to be unique per broadcast id.
    routerState.registerResponseHandleForOp(
        OPS.WRITE,
        () -> new WriteResponseHandler(numProcesses, numProcesses, 0,
            this.measurementCollector));

    StartTimeManager startTimeManager =
        new StartTimeManager(this.router.getAllRegisteredRoutes());
    CommitRequestHandler commitRequestHandler =
        new StartTimeCommitRequestHandler(
            this.processState,
            this.stateMachine,
            new CommitCache(),
            this.measurementCollector,
            startTimeManager);
    routerState.registerRequestHandlerForOp(OPS.COMMIT,
        () -> commitRequestHandler);

    IRequestHandler forwardWriteRequestHandler =
        new ForwardWriteRequestHandler(this.replicationOp,
            this.measurementCollector);
    routerState.registerRequestHandlerForOp(OPS.FORWARD_WRITE,
        () -> forwardWriteRequestHandler);

    // Needs to be unique per broadcast id.
    routerState.registerResponseHandleForOp(OPS.FORWARD_WRITE,
        ForwardWriteResponseHandler::new);

    IRequestHandler setMarkerRequestHandler =
        new SetMarkerRequestHandler(this.markerManager);
    routerState.registerRequestHandlerForOp(OPS.SET_MARKER,
        () -> setMarkerRequestHandler);

    // Needs to be unique per broadcast id.
    routerState.registerResponseHandleForOp(
        OPS.SET_MARKER, () -> new SetMarkerResponseHandler(numProcesses,
            this.markerManager));

    IRequestHandler stopTimeRequestHandler =
        new StoppedRequestHandler(
            writeRecorder, this.eventSchedulingPrimitive, startTimeManager,
            commitRequestHandler);
    routerState.registerRequestHandlerForOp(OPS.STOP_TIME,
        () -> stopTimeRequestHandler);

    markerRenewal.scheduleAtFixedRate(
        this.markerManager::establishMarkers, 0, 500, TimeUnit.MILLISECONDS);
  }
}
