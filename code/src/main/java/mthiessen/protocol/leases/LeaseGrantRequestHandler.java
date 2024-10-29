package mthiessen.protocol.leases;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.network.IRequestHandler;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@RequiredArgsConstructor
public class LeaseGrantRequestHandler implements IRequestHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(LeaseGrantRequestHandler.class);

  @NonNull private final LeaseManager leaseManager;

  @NonNull private final IRouter router;

  @NonNull private final Object me;

  @Override
  public Object execute(final Object sender, final Object request) {
    if (!(request instanceof Payloads.LeaseGrant leaseGrantRequest)) {
      LOGGER.error("Request was not of type {}", Payloads.LeaseGrant.class);
      return null;
    }

    LOGGER.info("Granting lease for sender {}", sender);

    if (leaseGrantRequest.leaseHolders().contains(this.me)) {
      this.leaseManager.addLease(sender, leaseGrantRequest);
    } else {
      this.router.broadcastRequest(
          Collections.singleton(sender), OPS.LEASE_REQUEST, new Payloads.LeaseRequest(), true);
    }

    return request;
  }
}
