package mthiessen.protocol.stopped;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.misc.Pair;
import mthiessen.network.IRequestHandler;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.state.process.StartTimeManager;
import mthiessen.protocol.state.writerecorder.IWriteRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class StoppedRequestHandler implements IRequestHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(StoppedRequestHandler.class);

  @NonNull private final IWriteRecorder writeRecorder;

  @NonNull private final EventSchedulingPrimitive eventSchedulingPrimitive;

  @NonNull private final StartTimeManager stoptimeManager;

  @NonNull private final IRequestHandler commitRequestHandler;

  @Override
  public Object execute(final Object sender, final Object request) {
    if (!(request instanceof Payloads.StopTimeRequest stopTimeRequest)) {
      LOGGER.error("Request is not of type " + Payloads.StopTimeRequest.class);
      return null;
    }

    LOGGER.info("Got stop ack request for index " + stopTimeRequest.index() + " from " + sender);

    long stopped = this.eventSchedulingPrimitive.event(sender, stopTimeRequest.stopped());

    boolean done = this.stoptimeManager.record(sender, stopTimeRequest.index(), stopped);

    if (done) {
      int index = stopTimeRequest.index();

      Payloads.PrepareRequest prepareRequest = this.writeRecorder.waitAndGetRequest(index);

      MetricsAttachedRequest<Payloads.PrepareRequest> metricsAttachedRequest =
          new MetricsAttachedRequest<>(prepareRequest, new Pair<>(0, 0));

      this.commitRequestHandler.execute(sender, metricsAttachedRequest);
    }

    return request;
  }
}
