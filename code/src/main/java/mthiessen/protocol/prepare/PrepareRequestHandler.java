package mthiessen.protocol.prepare;

import lombok.NonNull;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.process.IProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrepareRequestHandler extends BasePrepareRequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrepareRequestHandler.class);

  @NonNull private final IProcessState processState;

  public PrepareRequestHandler(
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull IProcessState processState) {
    super(measurementCollector);
    this.processState = processState;
  }

  @Override
  Object processPrepare(final Object sender, final Payloads.PrepareRequest prepareRequest) {
    this.processState.getLock().writeLock().lock();

    int proposalIndex = prepareRequest.index();

    LOGGER.info("Got rmwReq request for index " + proposalIndex);

    this.processState.setLatestVotedForIndex(
        Math.max(proposalIndex, this.processState.getLatestVotedForIndex()));

    this.processState.recordRMW(proposalIndex, prepareRequest.globallyUniqueRMW().rmwReq());

    this.processState.getLock().writeLock().unlock();

    return null;
  }
}
