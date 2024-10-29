package mthiessen.protocol.prepare;

import lombok.NonNull;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.process.StartTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PLPrepareRequestHandler extends BasePrepareRequestHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(PLPrepareRequestHandler.class);
  @NonNull protected final EventSchedulingPrimitive eventSchedulingPrimitive;
  @NonNull private final FutureWriteProcessState processState;
  @NonNull private final StartTimeManager startTimeManager;

  public PLPrepareRequestHandler(
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull FutureWriteProcessState processState,
      final @NonNull EventSchedulingPrimitive eventSchedulingPrimitive,
      final @NonNull StartTimeManager startTimeManager) {
    super(measurementCollector);
    this.processState = processState;
    this.eventSchedulingPrimitive = eventSchedulingPrimitive;
    this.startTimeManager = startTimeManager;
  }

  @Override
  Object processPrepare(final Object sender,
                   final Payloads.PrepareRequest prepareRequest) {
    this.processState.getLock().writeLock().lock();
    int proposalIndex = prepareRequest.index();

    LOGGER.info("Got rmwReq request for index " + proposalIndex);

    Payloads.PLPayload payload = (Payloads.PLPayload) prepareRequest.payload();

    long stop = this.eventSchedulingPrimitive.event(sender, payload.stop());

    this.processState.getPendingWrites().put(proposalIndex, stop);

    long start = this.eventSchedulingPrimitive.event(sender, payload.start());

    this.startTimeManager.record(sender, proposalIndex, start);

    this.processState.setLatestVotedForIndex(
        Math.max(proposalIndex, this.processState.getLatestVotedForIndex()));

    this.processState.recordRMW(proposalIndex, prepareRequest.globallyUniqueRMW().rmwReq());

    this.processState.getLock().writeLock().unlock();

    return null;
  }
}
