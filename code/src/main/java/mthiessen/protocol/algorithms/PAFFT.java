package mthiessen.protocol.algorithms;

import lombok.NonNull;
import mthiessen.IProcess;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.inmemory.MeasurementCollector;
import mthiessen.misc.UniqueNumberGenerator;
import mthiessen.misc.Util;
import mthiessen.network.IRequestHandler;
import mthiessen.network.IRouter;
import mthiessen.network.IRouterState;
import mthiessen.protocol.OPS;
import mthiessen.protocol.commit.CommitCache;
import mthiessen.protocol.commit.CommitRequestHandler;
import mthiessen.protocol.commit.StartTimeCommitRequestHandler;
import mthiessen.protocol.eventschedulingprimitive.*;
import mthiessen.protocol.forward.ForwardRMWRequestHandler;
import mthiessen.protocol.leaderelection.ILeaderElectionService;
import mthiessen.protocol.leaderelection.LeaderElectionOp;
import mthiessen.protocol.leaderelection.StaticLeaderElectionService;
import mthiessen.protocol.leases.LeaseGrantRequestHandler;
import mthiessen.protocol.leases.LeaseHolderManager;
import mthiessen.protocol.leases.LeaseManager;
import mthiessen.protocol.leases.LeaseRequestRequestHandler;
import mthiessen.protocol.prepare.FFTPrepareResponseHandler;
import mthiessen.protocol.prepare.PAPrepareRequestHandler;
import mthiessen.protocol.reads.IReadOp;
import mthiessen.protocol.reads.LeasedFutureWriteReads;
import mthiessen.protocol.reads.conflictdetection.IConflictDetection;
import mthiessen.protocol.replication.IReplicationOp;
import mthiessen.protocol.replication.NoOpReplication;
import mthiessen.protocol.replication.PAFFTReplication;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.process.StartTimeManager;
import mthiessen.protocol.state.proposer.IProposerState;
import mthiessen.protocol.state.proposer.ProposerState;
import mthiessen.protocol.state.writerecorder.IWriteRecorder;
import mthiessen.protocol.state.writerecorder.WriteRecorder;
import mthiessen.protocol.stopped.StoppedRequestHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PAFFT extends BaseProcess {
  private static final ScheduledExecutorService leaseGranting = Executors.newScheduledThreadPool(1);
  private static final ScheduledExecutorService markerRenewal = Executors.newScheduledThreadPool(1);

  @NonNull private final FutureWriteProcessState processState;
  @NonNull private final IStateMachine stateMachine;
  @NonNull private final MarkerManager markerManager;
  @NonNull private final EventSchedulingPrimitive eventSchedulingPrimitive;
  private final LeaseHolderManager leaseHolderManager;
  @NonNull private final LeaseManager leaseManager;
  @NonNull private final StartTimeManager startTimeManager;

  public PAFFT(
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
      final @NonNull EventSchedulingPrimitive eventSchedulingPrimitive,
      final LeaseHolderManager leaseHolderManager,
      final @NonNull LeaseManager leaseManager,
      final @NonNull StartTimeManager startTimeManager) {
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
    this.markerManager = markerManager;
    this.eventSchedulingPrimitive = eventSchedulingPrimitive;
    this.leaseHolderManager = leaseHolderManager;
    this.leaseManager = leaseManager;
    this.startTimeManager = startTimeManager;
  }

  public static IProcess generate(
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final IConflictDetection conflictDetection,
      final LowerBounds lowerBounds,
      final int alpha) {
    Object me = router.getRouterState().getIdentifier();

    ILeaderElectionService leaderElectionService = new StaticLeaderElectionService(leader, router);
    IProposerState proposerState = new ProposerState();
    FutureWriteProcessState processState = new FutureWriteProcessState();
    Runnable leaderElectionOp = new LeaderElectionOp(leaderElectionService, proposerState);
    MarkerManager markerManager = new MarkerManager(router);
    EventSchedulingPrimitive eventSchedulingPrimitive =
        new EventSchedulingPrimitive(
            markerManager, lowerBounds, router.getRouterState().getIdentifier());

    LeaseHolderManager leaseHolderManager = null;
    if (me.equals(leader)) {
      leaseHolderManager = new LeaseHolderManager(proposerState, eventSchedulingPrimitive, router);
    }

    LeaseManager leaseManager = new LeaseManager(eventSchedulingPrimitive);

    StartTimeManager startTimeManager = new StartTimeManager();

    IReadOp readOp = new LeasedFutureWriteReads(leaseManager, processState, stateMachine);

    IReplicationOp replicationOp;

    if (me.equals(leader)) {
      replicationOp =
          new PAFFTReplication(
              leaseHolderManager, eventSchedulingPrimitive, router, proposerState, alpha);
    } else {
      replicationOp = new NoOpReplication();
    }

    IMeasurementCollector measurementCollector =
        new MeasurementCollector(
            router.getRouterState().getIdentifier(), new UniqueNumberGenerator());

    return new PAFFT(
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
        eventSchedulingPrimitive,
        leaseHolderManager,
        leaseManager,
        startTimeManager);
  }

  @Override
  public void initialize() {
    IRouterState routerState = super.router.getRouterState();

    IWriteRecorder writeRecorder = new WriteRecorder();
    IRequestHandler writeRequestHandler =
        new PAPrepareRequestHandler(
            this.measurementCollector,
            this.router,
            this.processState,
            this.eventSchedulingPrimitive,
            writeRecorder,
            startTimeManager);
    routerState.registerRequestHandlerForOp(OPS.PREPARE, () -> writeRequestHandler);

    int numProcesses = super.router.getAllRegisteredRoutes().size();
    int majority = Util.majority(numProcesses);
    // Needs to be unique per broadcast id.
    routerState.registerResponseHandleForOp(
        OPS.PREPARE,
        () ->
            new FFTPrepareResponseHandler(
                numProcesses, majority, this.measurementCollector, this.eventSchedulingPrimitive));

    CommitRequestHandler commitRequestHandler =
        new StartTimeCommitRequestHandler(
            this.processState,
            routerState.getIdentifier(),
            this.stateMachine,
            new CommitCache(),
            this.measurementCollector,
            startTimeManager);
    routerState.registerRequestHandlerForOp(OPS.COMMIT, () -> commitRequestHandler);

    IRequestHandler forwardWriteRequestHandler =
        new ForwardRMWRequestHandler(this.replicationOp, this.measurementCollector);
    routerState.registerRequestHandlerForOp(OPS.FORWARD_RMW, () -> forwardWriteRequestHandler);

    IRequestHandler stopTimeRequestHandler =
        new StoppedRequestHandler(
            writeRecorder, this.eventSchedulingPrimitive, startTimeManager, commitRequestHandler);
    routerState.registerRequestHandlerForOp(OPS.STOP_TIME, () -> stopTimeRequestHandler);

    IRequestHandler setMarkerRequestHandler = new SetMarkerRequestHandler(this.markerManager);
    routerState.registerRequestHandlerForOp(OPS.SET_MARKER, () -> setMarkerRequestHandler);

    // Needs to be unique per broadcast id.
    routerState.registerResponseHandleForOp(
        OPS.SET_MARKER, () -> new SetMarkerResponseHandler(numProcesses, this.markerManager));

    IRequestHandler leaseGrantRequestHandler =
        new LeaseGrantRequestHandler(this.leaseManager, this.router, routerState.getIdentifier());
    routerState.registerRequestHandlerForOp(OPS.LEASE_GRANT, () -> leaseGrantRequestHandler);

    if (this.leaseHolderManager != null) {
      IRequestHandler leaseRequestRequestHandler =
          new LeaseRequestRequestHandler(this.leaseHolderManager);
      routerState.registerRequestHandlerForOp(OPS.LEASE_REQUEST, () -> leaseRequestRequestHandler);
    }

    markerRenewal.scheduleAtFixedRate(
        this.markerManager::establishMarkers, 0, 500, TimeUnit.MILLISECONDS);

    if (this.leaseHolderManager != null) {
      leaseGranting.scheduleAtFixedRate(
          this.leaseHolderManager::grantLeases, 0, 100, TimeUnit.MILLISECONDS);
    }
  }
}
