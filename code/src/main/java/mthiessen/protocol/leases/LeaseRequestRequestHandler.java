package mthiessen.protocol.leases;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.network.IRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class LeaseRequestRequestHandler implements IRequestHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(LeaseRequestRequestHandler.class);

  @NonNull private final LeaseHolderManager leaseHolderManager;

  @Override
  public Object execute(final Object sender, final Object request) {
    LOGGER.info("Handling lease request for sender {}", sender);

    this.leaseHolderManager.addLeaseHolder(sender);

    return request;
  }
}
