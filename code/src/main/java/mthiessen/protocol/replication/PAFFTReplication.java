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
public class PAFFTReplication implements IReplicationOp {

  private static final long GRACE = 1;

  private static final Logger LOGGER = LoggerFactory.getLogger(PAFFTReplication.class);

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

    long start = System.currentTimeMillis();

    int index = this.proposerState.getLatestAssignedIndex().incrementAndGet();

    LOGGER.info("Processing rmwReq with index {}", index);

    // Ensure markers are established.
    this.eventSchedulingPrimitive.initialize();

    Set<Object> leaseHolders = this.leaseHolderManager.getLeaseHolders();

    long t = System.currentTimeMillis();

    long localTime = t + this.alpha;

    Function<Object, Payloads.PrepareRequest> messageFunction =
        follower -> {
          Pair<Long, Integer> stop = this.eventSchedulingPrimitive.at(follower, localTime);
          Payloads.PAPayload payload = new Payloads.PAPayload(leaseHolders, stop);

          return new Payloads.PrepareRequest(index, globallyUniqueRMW, payload);
        };

    // pre-broadcast
    operationMeasurement.record(Event.PRE_PREPARE_BROADCAST);

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

    long stopped = System.currentTimeMillis();

    if (stopped - start > 100)
      System.out.println(index + " " + (stopped - start));

    // post-response
    operationMeasurement.record(Event.POST_PREPARE_RESPONSE);

    LOGGER.info("Sending commit for index {}", index);

    Set<Long> stopTimes = fftWait.getStopTimes();

    long time = localTime;

    for (long stopTime : stopTimes) {
      time = Math.max(time, stopTime);
    }

    this.commit(
        time, new Payloads.PrepareRequest(index, globallyUniqueRMW, 0), operationMeasurement);

    // post-commit
    operationMeasurement.record(Event.POST_COMMIT_CAlL);
  }

  protected void commit(
      final long time,
      final Payloads.PrepareRequest prepareRequest,
      final IOperationMeasurement operationMeasurement) {

    long now = System.currentTimeMillis();

    long waitFor = time - now;

    if (waitFor > 0) {
      try {
        Thread.sleep(waitFor);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    MetricsAttachedRequest<Payloads.PrepareRequest> commitRequest =
        new MetricsAttachedRequest<>(prepareRequest, operationMeasurement.getOperationIdentifier());

    this.router.broadcastRequestToAll(OPS.COMMIT, commitRequest, true);
  }
}
