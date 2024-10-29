package mthiessen.protocol.write;

import lombok.NonNull;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BHTWriteRequestHandler extends BaseWriteRequestHandler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BHTWriteRequestHandler.class);

  @NonNull
  private final FutureWriteProcessState processState;

  public BHTWriteRequestHandler(
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull FutureWriteProcessState processState) {
    super(measurementCollector);
    this.processState = processState;
  }

  @Override
  boolean processWrite(final Object sender,
                       final Payloads.WriteRequest writeRequest) {
    this.processState.getLock().writeLock().lock();

    int proposalIndex = writeRequest.index();

    LOGGER.info("Got write request for index " + proposalIndex);

    this.processState.getPendingWrites().put(proposalIndex,
        (Long) writeRequest.payload());

    this.processState.setMaxPendingIndex(
        Math.max(proposalIndex, this.processState.getMaxPendingIndex()));

    this.processState.setLatestVotedForIndex(
        Math.max(proposalIndex, this.processState.getLatestVotedForIndex()));

    this.processState.getLock().writeLock().unlock();

    return true;
  }
}
