package mthiessen.protocol.prepare;

import lombok.NonNull;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DELPrepareRequestHandler extends BasePrepareRequestHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(DELPrepareRequestHandler.class);

  @NonNull private final FutureWriteProcessState processState;

  public DELPrepareRequestHandler(
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull FutureWriteProcessState processState) {
    super(measurementCollector);
    this.processState = processState;
  }

  @Override
  Object processPrepare(final Object sender, final Payloads.PrepareRequest prepareRequest) {
    this.processState.getLock().writeLock().lock();

    int proposalIndex = prepareRequest.index();

    LOGGER.info("Got rmwReq request for index " + proposalIndex);

    this.processState.getPendingWrites().put(proposalIndex, (Long) prepareRequest.payload());

    this.processState.setLatestVotedForIndex(
        Math.max(proposalIndex, this.processState.getLatestVotedForIndex()));

    this.processState.recordRMW(proposalIndex, prepareRequest.globallyUniqueRMW().rmwReq());

    this.processState.getLock().writeLock().unlock();

    return null;
  }
}
