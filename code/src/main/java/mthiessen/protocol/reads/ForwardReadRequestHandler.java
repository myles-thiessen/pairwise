package mthiessen.protocol.reads;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.network.IRequestHandler;

@RequiredArgsConstructor
public class ForwardReadRequestHandler implements IRequestHandler {

  @NonNull private final IReadOp readOp;

  @NonNull private final IMeasurementCollector measurementCollector;

  @Override
  public Object execute(final Object sender, final Object request) {
    MetricsAttachedRequest<?> metricsAttachedRequest = MetricsAttachedRequest.check(request);

    IOperationMeasurement operationMeasurement =
        metricsAttachedRequest.getOperationMeasurement(this.measurementCollector, false);

    // pre-op
    operationMeasurement.record(Event.POST_READ_FORWARD_RECEIVE);

    Object readResponse =
        this.readOp.read(metricsAttachedRequest.getRequest(), operationMeasurement);

    // post-op
    operationMeasurement.record(Event.PRE_READ_FORWARD_RESPONSE_SEND);

    return readResponse;
  }
}
