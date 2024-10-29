package mthiessen.protocol.commit;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.misc.Pair;
import mthiessen.network.IRequestHandler;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.process.IProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class CommitRequestHandler implements IRequestHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(CommitRequestHandler.class);

  @NonNull
  protected final IProcessState processState;

  @NonNull
  private final IStateMachine stateMachine;

  @NonNull
  private final CommitCache commitCache;

  @NonNull
  private final IMeasurementCollector measurementCollector;

  @Override
  public Object execute(final Object sender, final Object request) {
    MetricsAttachedRequest<?> metricsAttachedRequest =
        MetricsAttachedRequest.check(request);

    if (!(metricsAttachedRequest.getRequest() instanceof Payloads.WriteRequest writeRequest)) {
      LOGGER.error("Request was not of type {}", Payloads.WriteRequest.class);
      return null;
    }

    IOperationMeasurement operationMeasurement =
        metricsAttachedRequest.getOperationMeasurement(this.measurementCollector, true);

    this.processCommitRequest(writeRequest, metricsAttachedRequest,
        operationMeasurement);

    return null;
  }

  protected void processCommitRequest(
      final Payloads.WriteRequest writeRequest,
      final MetricsAttachedRequest<?> metricsAttachedRequest,
      final IOperationMeasurement operationMeasurement) {
    operationMeasurement.record(Event.POST_COMMIT_RECEIVE);

    this.processState.getLock().writeLock().lock();

    int providedIndex = writeRequest.index();

    LOGGER.info("Got commit request for {}", providedIndex);

    int requiredIndex = this.processState.getLatestCommittedIndex() + 1;
    if (providedIndex == requiredIndex) {
      // Commit value provided
      this.stampAndCommit(writeRequest,
          metricsAttachedRequest.getOperationIdentifier());

      // Flush cache for all values provided after this value.
      this.commitCache
          .getCachedCommitsFromIndex(requiredIndex)
          .forEach(
              writeRequestTwoTupleTwoTuple ->
                  this.stampAndCommit(
                      writeRequestTwoTupleTwoTuple.k1(),
                      writeRequestTwoTupleTwoTuple.k2()));
    } else if (requiredIndex < providedIndex) {
      LOGGER.info("Learner is behind, next index is {}", requiredIndex);
      this.commitCache.addToCache(
          providedIndex, writeRequest,
          operationMeasurement.getOperationIdentifier());
    } // otherwise commit was already seen

    this.processState.getLock().writeLock().unlock();
  }

  private void stampAndCommit(
      final Payloads.WriteRequest request,
      final Pair<Object, Integer> operationIdentifier) {
    IOperationMeasurement operationMeasurement =
        this.measurementCollector.getExistingOperation(operationIdentifier);

    operationMeasurement.record(Event.POST_COMMIT_INITIATE);

    this.commit(request, operationMeasurement);
  }

  protected void commit(
      final Payloads.WriteRequest request,
      final IOperationMeasurement operationMeasurement) {
    LOGGER.info("Learner is committing index {}", request.index());
    Object write = request.write();
    this.processState.safeToApply(request.index());

    // post safe-to-apply check
    operationMeasurement.record(Event.POST_COMMIT_SAFE_TO_APPLY);

    this.stateMachine.write(write);

    // post write
    operationMeasurement.record(Event.POST_WRITE);

    this.processState.setLatestCommittedIndex(request.index());

    // post notify
    operationMeasurement.record(Event.POST_REFLECT_WRITE_AS_COMMITTED);
  }
}
