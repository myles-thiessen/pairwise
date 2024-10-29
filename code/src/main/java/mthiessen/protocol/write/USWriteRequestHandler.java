package mthiessen.protocol.write;

import lombok.NonNull;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.process.StartTimeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class USWriteRequestHandler extends BaseWriteRequestHandler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(USWriteRequestHandler.class);
  @NonNull
  protected final EventSchedulingPrimitive eventSchedulingPrimitive;
  @NonNull
  private final FutureWriteProcessState processState;
  @NonNull
  private final StartTimeManager startTimeManager;

  public USWriteRequestHandler(
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
  boolean processWrite(final Object sender,
                       final Payloads.WriteRequest writeRequest) {
    this.processState.getLock().writeLock().lock();
    int proposalIndex = writeRequest.index();

    LOGGER.info("Got write request for index " + proposalIndex);

    Payloads.USPayload payload = (Payloads.USPayload) writeRequest.payload();

    long stop = this.eventSchedulingPrimitive.event(sender, payload.stop());

    this.processState.getPendingWrites().put(proposalIndex, stop);

    long start = this.eventSchedulingPrimitive.event(sender, payload.start());

    this.startTimeManager.record(sender, proposalIndex, start);

    this.processState.setMaxPendingIndex(
        Math.max(proposalIndex, this.processState.getMaxPendingIndex()));

    this.processState.setLatestVotedForIndex(
        Math.max(proposalIndex, this.processState.getLatestVotedForIndex()));

    this.processState.getLock().writeLock().unlock();

    return true;
  }
}
