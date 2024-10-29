package mthiessen.protocol.write;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.network.IRequestHandler;
import mthiessen.protocol.replication.IReplicationOp;

@RequiredArgsConstructor
public class ForwardWriteRequestHandler implements IRequestHandler {

  @NonNull
  private final IReplicationOp replicationOp;

  @NonNull
  private final IMeasurementCollector measurementCollector;

  @Override
  public Object execute(final Object sender, final Object request) {
    MetricsAttachedRequest<?> metricsAttachedRequest =
        MetricsAttachedRequest.check(request);

    IOperationMeasurement operationMeasurement =
        metricsAttachedRequest.getOperationMeasurement(this.measurementCollector, true);

    operationMeasurement.record(Event.POST_WRITE_FORWARD_RECEIVE);

    this.replicationOp.replicate(metricsAttachedRequest.getRequest(),
        operationMeasurement);

    operationMeasurement.record(Event.PRE_WRITE_FORWARD_RESPONSE_SEND);

    return null;
  }
}
