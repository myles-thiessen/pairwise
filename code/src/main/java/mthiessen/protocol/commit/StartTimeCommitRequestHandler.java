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

  @NonNull private final StartTimeManager startTimeManager;

  public StartTimeCommitRequestHandler(
      @NonNull final IProcessState processState,
      @NonNull final Object identifier,
      @NonNull final IStateMachine stateMachine,
      @NonNull final CommitCache commitCache,
      @NonNull final IMeasurementCollector measurementCollector,
      @NonNull final StartTimeManager startTimeManager) {
    super(processState, identifier, stateMachine, commitCache, measurementCollector);
    this.startTimeManager = startTimeManager;
  }

  // Before taking lock need to check if all stop acks have arrived. If not,
  // need to wait for them to arrive. Otherwise, need to wait for their
  // promise times to expire before invoking.
  @Override
  protected void processCommitRequest(
      final Payloads.PrepareRequest prepareRequest,
      final MetricsAttachedRequest<?> metricsAttachedRequest,
      final IOperationMeasurement operationMeasurement) {

    long waitUntil = this.startTimeManager.extractMaxTime(prepareRequest.index());

    if (waitUntil != -1) {
      long waitFor = waitUntil - System.currentTimeMillis();

      if (waitFor > 0) {
        try {
          Thread.sleep(waitFor);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    super.processCommitRequest(prepareRequest, metricsAttachedRequest, operationMeasurement);
  }

  @Override
  protected void commit(
      final Payloads.PrepareRequest request, final IOperationMeasurement operationMeasurement) {
    super.commit(request, operationMeasurement);
    ((FutureWriteProcessState) super.processState).getPendingWrites().remove(request.index());
  }
}
