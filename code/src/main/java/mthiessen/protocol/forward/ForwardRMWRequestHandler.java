package mthiessen.protocol.forward;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.network.IRequestHandler;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.replication.IReplicationOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class ForwardRMWRequestHandler implements IRequestHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ForwardRMWRequestHandler.class);

  @NonNull private final IReplicationOp replicationOp;

  @NonNull private final IMeasurementCollector measurementCollector;

  @Override
  public Object execute(final Object sender, final Object request) {
    MetricsAttachedRequest<?> metricsAttachedRequest = MetricsAttachedRequest.check(request);

    if (!(metricsAttachedRequest.getRequest()
        instanceof Payloads.GloballyUniqueRMW globallyUniqueRMW)) {
      LOGGER.error("Request was not of type {}", Payloads.PrepareRequest.class);
      return null;
    }

    IOperationMeasurement operationMeasurement =
        metricsAttachedRequest.getOperationMeasurement(this.measurementCollector, true);

    operationMeasurement.record(Event.POST_RMW_FORWARD_RECEIVE);

    this.replicationOp.replicate(globallyUniqueRMW, operationMeasurement);

    operationMeasurement.record(Event.PRE_RMW_FORWARD_RESPONSE_SEND);

    return null;
  }
}
