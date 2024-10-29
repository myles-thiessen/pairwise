package mthiessen.protocol.commit;

import lombok.NonNull;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.process.IProcessState;

public class BHTCommitRequestHandler extends CommitRequestHandler {
  private final long delta;

  public BHTCommitRequestHandler(
      @NonNull final IProcessState processState,
      @NonNull final IStateMachine stateMachine,
      @NonNull final CommitCache commitCache,
      @NonNull final IMeasurementCollector measurementCollector,
      final long delta) {
    super(processState, stateMachine, commitCache, measurementCollector);
    this.delta = delta;
  }

  // Need to do this before taking the lock otherwise thread sleeps while
  // holding the lock.
  @Override
  protected void processCommitRequest(
      Payloads.WriteRequest writeRequest,
      MetricsAttachedRequest<?> metricsAttachedRequest,
      IOperationMeasurement operationMeasurement) {
    FutureWriteProcessState futureWriteProcessState =
        ((FutureWriteProcessState) super.processState);
    long waitUntil =
        futureWriteProcessState.getPendingWrites().get(writeRequest.index())
            + this.delta
            - System.currentTimeMillis();

    if (waitUntil > 0) {
      try {
        Thread.sleep(waitUntil);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    operationMeasurement.record(Event.POST_BHT_COMMIT_WAIT);

    super.processCommitRequest(writeRequest, metricsAttachedRequest,
        operationMeasurement);
  }

  @Override
  protected void commit(
      final Payloads.WriteRequest request,
      final IOperationMeasurement operationMeasurement) {
    FutureWriteProcessState futureWriteProcessState =
        ((FutureWriteProcessState) super.processState);
    super.commit(request, operationMeasurement);
    futureWriteProcessState.getPendingWrites().remove(request.index());
  }
}
