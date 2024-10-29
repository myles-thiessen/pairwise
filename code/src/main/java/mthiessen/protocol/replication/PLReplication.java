package mthiessen.protocol.replication;

import lombok.NonNull;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.state.proposer.IProposerState;

import java.util.function.Function;

public class PLReplication extends BaseReplication {

  protected final EventSchedulingPrimitive eventSchedulingPrimitive;

  // Promise time
  protected final int alpha;

  public PLReplication(
      final @NonNull IRouter router,
      final @NonNull IProposerState proposerState,
      final @NonNull EventSchedulingPrimitive eventSchedulingPrimitive,
      final int alpha) {
    super(router, proposerState);
    this.eventSchedulingPrimitive = eventSchedulingPrimitive;
    this.alpha = alpha;
  }

  @Override
  protected Pair<Function<Object, Payloads.PrepareRequest>, Payloads.PrepareRequest> broadcast(
      final int index, final Payloads.GloballyUniqueRMW globallyUniqueRMW) {

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

    return new Pair<>(messageFunction, new Payloads.PrepareRequest(index, globallyUniqueRMW, 0));
  }

  @Override
  protected void commit(
      final Payloads.PrepareRequest prepareRequest,
      final IOperationMeasurement operationMeasurement) {
    // post-sleep
    operationMeasurement.record(Event.POST_PL_RMW_WAIT);

    // Safety hear is based on the fast the writes and reads are atomic to
    // integers.
    super.proposerState.setLatestBroadcastIndex(prepareRequest.index());

    MetricsAttachedRequest<Payloads.PrepareRequest> commitRequest =
        new MetricsAttachedRequest<>(prepareRequest, operationMeasurement.getOperationIdentifier());

    this.router.broadcastRequestToAll(OPS.COMMIT, commitRequest, true);
  }
}
