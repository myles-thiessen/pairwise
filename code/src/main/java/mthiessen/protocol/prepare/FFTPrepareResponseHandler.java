package mthiessen.protocol.prepare;

import lombok.NonNull;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.misc.Pair;
import mthiessen.network.CountBasedBlockingResponseHandler;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFTPrepareResponseHandler extends CountBasedBlockingResponseHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(FFTPrepareResponseHandler.class);
  @NonNull private final IMeasurementCollector measurementCollector;
  @NonNull private final EventSchedulingPrimitive eventSchedulingPrimitive;
  private final FFTWait fftWait = new FFTWait();
  private int counter = 0;

  public FFTPrepareResponseHandler(
      final int numberOfExpectedResponses,
      final int acceptanceThreshold,
      final @NonNull IMeasurementCollector measurementCollector,
      final @NonNull EventSchedulingPrimitive eventSchedulingPrimitive) {
    super(numberOfExpectedResponses, acceptanceThreshold);
    this.measurementCollector = measurementCollector;
    this.eventSchedulingPrimitive = eventSchedulingPrimitive;
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

    long time = 0;
    if (prepareResponse.response() != null) {
      Pair<Object, Pair<Long, Integer>> payload =
          (Pair<Object, Pair<Long, Integer>>) prepareResponse.response();
      Object follower = payload.k1();
      Pair<Long, Integer> event = payload.k2();

      time = this.eventSchedulingPrimitive.event(follower, event);
    }

    this.fftWait.record(sender, time);
  }

  @Override
  protected Object responseState() {
    return this.fftWait;
  }
}
