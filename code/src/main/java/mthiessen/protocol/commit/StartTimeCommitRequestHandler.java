package mthiessen.protocol.commit;

import lombok.NonNull;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.process.IProcessState;
import mthiessen.protocol.state.process.StartTimeManager;

public class StartTimeCommitRequestHandler extends CommitRequestHandler {

  @NonNull
  private final StartTimeManager startTimeManager;

  public StartTimeCommitRequestHandler(
      @NonNull final IProcessState processState,
      @NonNull final IStateMachine stateMachine,
      @NonNull final CommitCache commitCache,
      @NonNull final IMeasurementCollector measurementCollector,
      @NonNull final StartTimeManager startTimeManager) {
    super(processState, stateMachine, commitCache, measurementCollector);
    this.startTimeManager = startTimeManager;
  }

  // Before taking lock need to check if all stop acks have arrived. If not,
  // need to wait for them to arrive. Otherwise, need to wait for their
  // promise times to expire before invoking.
  @Override
  protected void processCommitRequest(
      final Payloads.WriteRequest writeRequest,
      final MetricsAttachedRequest<?> metricsAttachedRequest,
      final IOperationMeasurement operationMeasurement) {

    long waitUntil = this.startTimeManager.getMaxTime(writeRequest.index());

    long waitFor = waitUntil - System.currentTimeMillis();

    if (waitFor > 0) {
      try {
        Thread.sleep(waitFor);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    super.processCommitRequest(writeRequest, metricsAttachedRequest,
        operationMeasurement);
  }

  @Override
  protected void commit(
      final Payloads.WriteRequest request,
      final IOperationMeasurement operationMeasurement) {
    super.commit(request, operationMeasurement);
    ((FutureWriteProcessState) super.processState).getPendingWrites().remove(request.index());
  }
}
