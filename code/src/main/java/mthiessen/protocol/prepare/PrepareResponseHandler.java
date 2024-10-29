package mthiessen.protocol.prepare;

import lombok.NonNull;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.network.CountBasedBlockingResponseHandler;
import mthiessen.protocol.Payloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class PrepareResponseHandler extends CountBasedBlockingResponseHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(PrepareResponseHandler.class);
  private final List<Payloads.PrepareResponse> acceptedResponse = new LinkedList<>();
  @NonNull private final IMeasurementCollector measurementCollector;
  private int counter = 0;

  public PrepareResponseHandler(
      final int numberOfExpectedResponses,
      final int acceptanceThreshold,
      final @NonNull IMeasurementCollector measurementCollector) {
    super(numberOfExpectedResponses, acceptanceThreshold);
    this.measurementCollector = measurementCollector;
  }

  @Override
  protected void handle(final Object sender, final Object response) {
    MetricsAttachedRequest<?> metricsAttachedRequest = MetricsAttachedRequest.check(response);

    if (!(metricsAttachedRequest.getRequest()
        instanceof Payloads.PrepareResponse prepareResponse)) {
      LOGGER.error("Response was not of type {}", Payloads.PrepareResponse.class);
      return;
    }

    IOperationMeasurement operationMeasurement =
        metricsAttachedRequest.getOperationMeasurement(this.measurementCollector, true);

    operationMeasurement.record(Event.POST_PREPARE_RESPONSE_RECEIVED, "_" + this.counter++);

    this.acceptedResponse.add(prepareResponse);
  }

  @Override
  protected Object responseState() {
    return this.acceptedResponse;
  }
}
