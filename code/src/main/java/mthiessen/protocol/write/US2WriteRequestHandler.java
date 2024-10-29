package mthiessen.protocol.write;

import lombok.NonNull;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.writerecorder.IWriteRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class US2WriteRequestHandler extends BaseWriteRequestHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(US2WriteRequestHandler.class);
  @NonNull
  private final IRouter router;
  @NonNull
  private final EventSchedulingPrimitive eventSchedulingPrimitive;
  @NonNull
  private final FutureWriteProcessState processState;
  @NonNull
  private final IWriteRecorder writeRecorder;

  public US2WriteRequestHandler(
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull IRouter router,
      final @NonNull FutureWriteProcessState processState,
      final @NonNull EventSchedulingPrimitive eventSchedulingPrimitive,
      final @NonNull IWriteRecorder writeRecorder) {
    super(measurementCollector);
    this.router = router;
    this.processState = processState;
    this.eventSchedulingPrimitive = eventSchedulingPrimitive;
    this.writeRecorder = writeRecorder;
  }

  @Override
  boolean processWrite(final Object sender,
                       final Payloads.WriteRequest writeRequest) {
    this.processState.getLock().writeLock().lock();
    int proposalIndex = writeRequest.index();

    LOGGER.info("Got write request for index " + proposalIndex);

    Payloads.US2Payload payload = (Payloads.US2Payload) writeRequest.payload();

    long stop = this.eventSchedulingPrimitive.event(sender, payload.stop());

    this.processState.getPendingWrites().put(proposalIndex, stop);

    this.processState.setMaxPendingIndex(
        Math.max(proposalIndex, this.processState.getMaxPendingIndex()));

    this.processState.setLatestVotedForIndex(
        Math.max(proposalIndex, this.processState.getLatestVotedForIndex()));

    this.writeRecorder.setRequest(proposalIndex, writeRequest);

    this.processState.getLock().writeLock().unlock();

    // Per neighbouring process send a stop time request.
    this.router.broadcastPersonalizedRequestToAll(
        OPS.STOP_TIME,
        p -> {
          Pair<Long, Integer> event = this.eventSchedulingPrimitive.after(p,
              stop);

          return new Payloads.StopTimeRequest(writeRequest.index(), event);
        },
        true);

    return true;
  }
}
