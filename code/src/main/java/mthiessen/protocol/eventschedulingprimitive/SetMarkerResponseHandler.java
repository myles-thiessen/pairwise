package mthiessen.protocol.eventschedulingprimitive;

import lombok.NonNull;
import mthiessen.network.CountBasedBlockingResponseHandler;
import mthiessen.protocol.Payloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetMarkerResponseHandler extends CountBasedBlockingResponseHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SetMarkerResponseHandler.class);

  @NonNull private final MarkerManager markerManager;

  public SetMarkerResponseHandler(
      final int numberOfProcesses, final @NonNull MarkerManager markerManager) {
    super(numberOfProcesses, numberOfProcesses);
    this.markerManager = markerManager;
  }

  @Override
  protected Status process(final Object sender, final Object response) {
    if (!(response instanceof Payloads.MarkerRequest markerRequest)) {
      LOGGER.error("Request was not of type {}", Payloads.MarkerRequest.class);
      return null;
    }

    LOGGER.info("Setting granter marker for sender {}", sender);

    this.markerManager.setGrantedMarker(sender, markerRequest.time(), markerRequest.version());

    return super.process(sender, response);
  }

  @Override
  protected void handle(final Object sender, final Object response) {}

  @Override
  protected Object responseState() {
    return null;
  }
}
