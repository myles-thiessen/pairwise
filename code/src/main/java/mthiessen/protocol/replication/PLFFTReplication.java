package mthiessen.protocol.replication;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.misc.Pair;
import mthiessen.network.BroadcastResponse;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.leases.LeaseHolderManager;
import mthiessen.protocol.prepare.FFTWait;
import mthiessen.protocol.state.proposer.IProposerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PLFFTReplication implements IReplicationOp {

  private static final long GRACE = 1;

  private static final Logger LOGGER = LoggerFactory.getLogger(PLFFTReplication.class);

  @NonNull private final LeaseHolderManager leaseHolderManager;
  @NonNull private final EventSchedulingPrimitive eventSchedulingPrimitive;
  @NonNull private final IRouter router;
  @NonNull private final IProposerState proposerState;
  // Promise time
  private final int alpha;

  @Override
  public void replicate(
      final Payloads.GloballyUniqueRMW globallyUniqueRMW,
      final IOperationMeasurement operationMeasurement) {

    int index = this.proposerState.getLatestAssignedIndex().incrementAndGet();

    LOGGER.info("Processing rmwReq with index {}", index);

    // Ensure markers are established.
    this.eventSchedulingPrimitive.initialize();

    long t = System.currentTimeMillis();

    long localTime = t + this.alpha;

    Function<Object, Payloads.PrepareRequest> messageFunction =
        follower -> {
          Pair<Long, Integer> stop = this.eventSchedulingPrimitive.before(follower, localTime);
          Pair<Long, Integer> start = this.eventSchedulingPrimitive.after(follower, localTime);
          Payloads.PLPayload payload = new Payloads.PLPayload(stop, start);

          return new Payloads.PrepareRequest(index, globallyUniqueRMW, payload);
        };

    // pre-broadcast
    operationMeasurement.record(Event.PRE_PREPARE_BROADCAST);

    Set<Object> leaseHolders = this.leaseHolderManager.getLeaseHolders();

    long send = System.currentTimeMillis();

    int broadcastIdentifier =
        this.router.broadcastPersonalizedRequestToAll(
            OPS.PREPARE,
            sender ->
                new MetricsAttachedRequest<>(
                    messageFunction.apply(sender), operationMeasurement.getOperationIdentifier()),
            true);

    BroadcastResponse broadcastResponse =
        this.router.waitForResponse(OPS.PREPARE, broadcastIdentifier);

    LOGGER.info("Got response " + broadcastResponse.status());

    FFTWait fftWait = (FFTWait) broadcastResponse.responsePayload();

    fftWait.waitOrTimeout(leaseHolders, send + GRACE * 1000);

    Set<Object> acceptors = fftWait.getAcceptors();

    Set<Object> dif =
        leaseHolders.stream()
            .filter(leaseHolder -> !acceptors.contains(leaseHolder))
            .collect(Collectors.toSet());

    if (!dif.isEmpty()) {
      this.leaseHolderManager.revokeLeases(dif);
    }

    // post-response
    operationMeasurement.record(Event.POST_PREPARE_RESPONSE);

    LOGGER.info("Sending commit for index {}", index);

    this.commit(new Payloads.PrepareRequest(index, globallyUniqueRMW, 0), operationMeasurement);

    // post-commit
    operationMeasurement.record(Event.POST_COMMIT_CAlL);
  }

  protected void commit(
      final Payloads.PrepareRequest prepareRequest,
      final IOperationMeasurement operationMeasurement) {
    // post-sleep
    operationMeasurement.record(Event.POST_PL_RMW_WAIT);

    // Safety hear is based on the fast the writes and reads are atomic to
    // integers.
    this.proposerState.setLatestBroadcastIndex(prepareRequest.index());

    MetricsAttachedRequest<Payloads.PrepareRequest> commitRequest =
        new MetricsAttachedRequest<>(prepareRequest, operationMeasurement.getOperationIdentifier());

    this.router.broadcastRequestToAll(OPS.COMMIT, commitRequest, true);
  }
}
