package mthiessen.protocol.eventschedulingprimitive;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.network.IRequestHandler;
import mthiessen.protocol.Payloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class SetMarkerRequestHandler implements IRequestHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(SetMarkerRequestHandler.class);

  @NonNull private final MarkerManager markerManager;

  @Override
  public Object execute(final Object sender, final Object request) {
    if (!(request instanceof Payloads.MarkerRequest markerRequest)) {
      LOGGER.error("Request was not of type {}", Payloads.MarkerRequest.class);
      return null;
    }

    LOGGER.info("Setting held marker for sender {}", sender);

    this.markerManager.setHeldMarker(sender, markerRequest.version());
    return request;
  }
}
