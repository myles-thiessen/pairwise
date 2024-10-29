package mthiessen.protocol.commit;

import lombok.NonNull;
import lombok.Setter;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.process.IProcessState;

@Setter
public class DELCommitRequestHandler extends CommitRequestHandler {
  private long delta;

  public DELCommitRequestHandler(
      @NonNull final IProcessState processState,
      @NonNull final Object identifier,
      @NonNull final IStateMachine stateMachine,
      @NonNull final CommitCache commitCache,
      @NonNull final IMeasurementCollector measurementCollector,
      final long delta) {
    super(processState, identifier, stateMachine, commitCache, measurementCollector);
    this.delta = delta;
  }

  // Need to do this before taking the lock otherwise thread sleeps while
  // holding the lock.
  @Override
  protected void processCommitRequest(
      Payloads.PrepareRequest prepareRequest,
      MetricsAttachedRequest<?> metricsAttachedRequest,
      IOperationMeasurement operationMeasurement) {
    FutureWriteProcessState futureWriteProcessState =
        ((FutureWriteProcessState) super.processState);
    long waitUntil =
        futureWriteProcessState.getPendingWrites().get(prepareRequest.index())
            + this.delta
            - System.currentTimeMillis();

    if (waitUntil > 0) {
      try {
        Thread.sleep(waitUntil);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    operationMeasurement.record(Event.POST_DEL_COMMIT_WAIT);

    super.processCommitRequest(prepareRequest, metricsAttachedRequest, operationMeasurement);
  }

  @Override
  protected void commit(
      final Payloads.PrepareRequest request, final IOperationMeasurement operationMeasurement) {
    FutureWriteProcessState futureWriteProcessState =
        ((FutureWriteProcessState) super.processState);
    super.commit(request, operationMeasurement);
    futureWriteProcessState.getPendingWrites().remove(request.index());
  }
}
