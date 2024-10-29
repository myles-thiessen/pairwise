package mthiessen.instrumentation;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.misc.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

@Getter
@RequiredArgsConstructor
public class MetricsAttachedRequest<R> implements Serializable {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsAttachedRequest.class);

  // May be null.
  private final R request;

  @NonNull private final Pair<Object, Integer> operationIdentifier;

  public static MetricsAttachedRequest<?> check(final Object request) {
    if (!(request instanceof MetricsAttachedRequest<?>)) {
      LOGGER.error("Request was not of type {}", MetricsAttachedRequest.class);
      System.exit(-1);
    }

    return (MetricsAttachedRequest<?>) request;
  }

  public IOperationMeasurement getOperationMeasurement(
      final IMeasurementCollector measurementCollector, final boolean write) {
    return measurementCollector.getOrRegisterExistingOperation(this.operationIdentifier, write);
  }
}
