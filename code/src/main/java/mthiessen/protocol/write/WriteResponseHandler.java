package mthiessen.protocol.write;

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

public class WriteResponseHandler extends CountBasedBlockingResponseHandler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(WriteResponseHandler.class);
  private final List<Payloads.WriteResponse> acceptedResponse =
      new LinkedList<>();
  @NonNull
  private final IMeasurementCollector measurementCollector;
  private int counter = 0;

  public WriteResponseHandler(
      final int numberOfExpectedResponses,
      final int acceptanceThreshold,
      final int failureThreshold,
      final @NonNull IMeasurementCollector measurementCollector) {
    super(numberOfExpectedResponses, acceptanceThreshold, failureThreshold);
    this.measurementCollector = measurementCollector;
  }

  @Override
  protected boolean acceptance(final Object response) {
    MetricsAttachedRequest<?> metricsAttachedRequest =
        MetricsAttachedRequest.check(response);

    if (!(metricsAttachedRequest.getRequest() instanceof Payloads.WriteResponse writeResponse)) {
      LOGGER.error("Response was not of type {}", Payloads.WriteResponse.class);
      return false;
    }

    IOperationMeasurement operationMeasurement =
        metricsAttachedRequest.getOperationMeasurement(this.measurementCollector, true);

    operationMeasurement.record(Event.POST_WRITE_RESPONSE_RECEIVED,
        "_" + this.counter++);

    boolean decision = writeResponse.decision();

    if (decision) {
      this.acceptedResponse.add(writeResponse);
    }

    return decision;
  }

  @Override
  protected Object responseState() {
    return this.acceptedResponse;
  }
}
