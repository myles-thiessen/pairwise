package mthiessen.protocol.write;

import lombok.NonNull;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.process.IProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteRequestHandler extends BaseWriteRequestHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(WriteRequestHandler.class);

  @NonNull
  private final IProcessState processState;

  public WriteRequestHandler(
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull IProcessState processState) {
    super(measurementCollector);
    this.processState = processState;
  }

  @Override
  boolean processWrite(final Object sender,
                       final Payloads.WriteRequest writeRequest) {
    this.processState.getLock().writeLock().lock();

    int proposalIndex = writeRequest.index();

    LOGGER.info("Got write request for index " + proposalIndex);

    this.processState.setLatestVotedForIndex(
        Math.max(proposalIndex, this.processState.getLatestVotedForIndex()));

    this.processState.getLock().writeLock().unlock();

    return true;
  }
}
