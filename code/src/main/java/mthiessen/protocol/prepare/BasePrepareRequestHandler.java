package mthiessen.protocol.prepare;

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
public abstract class BasePrepareRequestHandler implements IRequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(BasePrepareRequestHandler.class);

  @NonNull private final IMeasurementCollector measurementCollector;

  @Override
  public Object execute(final Object sender, final Object request) {
    MetricsAttachedRequest<?> metricsAttachedRequest = MetricsAttachedRequest.check(request);

    if (!(metricsAttachedRequest.getRequest() instanceof Payloads.PrepareRequest prepareRequest)) {
      LOGGER.error("Request was not of type {}", Payloads.PrepareRequest.class);
      return null;
    }

    IOperationMeasurement operationMeasurement =
        metricsAttachedRequest.getOperationMeasurement(this.measurementCollector, true);

    // pre-op
    operationMeasurement.record(Event.POST_PREPARE_BROADCAST_RECEIVE);

    Object response = this.processPrepare(sender, prepareRequest);

    // post-op
    operationMeasurement.record(Event.PRE_PREPARE_RESPONSE_SEND);

    return new MetricsAttachedRequest<>(
        new Payloads.PrepareResponse(response),
        operationMeasurement.getOperationIdentifier());
  }

  abstract Object processPrepare(
      final Object sender, final Payloads.PrepareRequest prepareRequest);
}
