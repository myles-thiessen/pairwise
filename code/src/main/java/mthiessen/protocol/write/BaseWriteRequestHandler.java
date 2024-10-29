package mthiessen.protocol.write;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.network.IRequestHandler;
import mthiessen.protocol.Payloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public abstract class BaseWriteRequestHandler implements IRequestHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(BaseWriteRequestHandler.class);

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

    // pre-op
    operationMeasurement.record(Event.POST_WRITE_BROADCAST_RECEIVE);

    boolean decision = this.processWrite(sender, writeRequest);

    // post-op
    operationMeasurement.record(Event.PRE_WRITE_RESPONSE_SEND);

    return new MetricsAttachedRequest<>(
        new Payloads.WriteResponse(decision),
        operationMeasurement.getOperationIdentifier());
  }

  abstract boolean processWrite(final Object sender,
                                final Payloads.WriteRequest writeRequest);
}
