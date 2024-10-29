package mthiessen.protocol.prepare;

import lombok.NonNull;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.process.StartTimeManager;
import mthiessen.protocol.state.writerecorder.IWriteRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PAPrepareRequestHandler extends BasePrepareRequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PAPrepareRequestHandler.class);
  @NonNull private final IRouter router;
  @NonNull private final EventSchedulingPrimitive eventSchedulingPrimitive;
  @NonNull private final FutureWriteProcessState processState;
  @NonNull private final IWriteRecorder writeRecorder;
  @NonNull private final StartTimeManager startTimeManager;

  public PAPrepareRequestHandler(
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull IRouter router,
      final @NonNull FutureWriteProcessState processState,
      final @NonNull EventSchedulingPrimitive eventSchedulingPrimitive,
      final @NonNull IWriteRecorder writeRecorder,
      final @NonNull StartTimeManager startTimeManager) {
    super(measurementCollector);
    this.router = router;
    this.processState = processState;
    this.eventSchedulingPrimitive = eventSchedulingPrimitive;
    this.writeRecorder = writeRecorder;
    this.startTimeManager = startTimeManager;
  }

  @Override
  Object processPrepare(final Object sender, final Payloads.PrepareRequest prepareRequest) {
    this.processState.getLock().writeLock().lock();
    int proposalIndex = prepareRequest.index();

    LOGGER.info("Got rmwReq request for index " + proposalIndex);

    Payloads.PAPayload payload = (Payloads.PAPayload) prepareRequest.payload();

    this.startTimeManager.recordWhoToWaitFor(proposalIndex, payload.leaseHolders());

    long stop = this.eventSchedulingPrimitive.event(sender, payload.stop());

    this.processState.getPendingWrites().put(proposalIndex, stop);

    this.processState.setLatestVotedForIndex(
        Math.max(proposalIndex, this.processState.getLatestVotedForIndex()));

    this.processState.recordRMW(proposalIndex, prepareRequest.globallyUniqueRMW().rmwReq());

    this.writeRecorder.setRequest(proposalIndex, prepareRequest);

    this.processState.getLock().writeLock().unlock();

    // Per neighbouring process send a stop time request.
    this.router.broadcastPersonalizedRequestToAll(
        OPS.STOP_TIME,
        p -> {
          Pair<Long, Integer> event = this.eventSchedulingPrimitive.after(p, stop);

          return new Payloads.StopTimeRequest(prepareRequest.index(), event);
        },
        true);

    Pair<Long, Integer> event = this.eventSchedulingPrimitive.after(sender, stop);

    return new Pair<>(this.router.getRouterState().getIdentifier(), event);
  }
}
