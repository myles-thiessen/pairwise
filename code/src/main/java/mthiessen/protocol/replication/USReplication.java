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
import mthiessen.protocol.state.process.IProcessState;
import mthiessen.protocol.state.proposer.IProposerState;

import java.util.function.Function;

public class USReplication extends BaseReplication {

  protected final EventSchedulingPrimitive eventSchedulingPrimitive;
  // Promise time
  protected final int alpha;
  private final IProcessState processState;

  public USReplication(
      final @NonNull IRouter router,
      final @NonNull IProposerState proposerState,
      final @NonNull EventSchedulingPrimitive eventSchedulingPrimitive,
      final @NonNull IProcessState processState,
      final int alpha) {
    super(router, proposerState);
    this.eventSchedulingPrimitive = eventSchedulingPrimitive;
    this.processState = processState;
    this.alpha = alpha;
  }

  @Override
  protected Pair<Function<Object, Payloads.WriteRequest>,
      Payloads.WriteRequest> broadcast(
      final int index, final Object write) {

    // Ensure markers are established.
    this.eventSchedulingPrimitive.initialize();

    long t = System.currentTimeMillis();

    long localTime = t + this.alpha;

    Function<Object, Payloads.WriteRequest> messageFunction =
        follower -> {
          Pair<Long, Integer> stop =
              this.eventSchedulingPrimitive.before(follower, localTime);
          Pair<Long, Integer> start =
              this.eventSchedulingPrimitive.after(follower, localTime);
          Payloads.USPayload payload = new Payloads.USPayload(stop, start);

          return new Payloads.WriteRequest(index, write, payload);
        };

    return new Pair<>(messageFunction, new Payloads.WriteRequest(index, write
        , 0));
  }

  @Override
  protected void commit(
      final Payloads.WriteRequest writeRequest,
      final IOperationMeasurement operationMeasurement) {
    // post-sleep
    operationMeasurement.record(Event.POST_US_WRITE_WAIT);

    // Safety hear is based on the fast the writes and reads are atomic to
    // integers.
    super.proposerState.setLatestBroadcastIndex(writeRequest.index());

    MetricsAttachedRequest<Payloads.WriteRequest> commitRequest =
        new MetricsAttachedRequest<>(writeRequest,
            operationMeasurement.getOperationIdentifier());

    this.router.broadcastRequestToAll(OPS.COMMIT, commitRequest, true);

    this.processState.getLock().readLock().lock();

    int index = writeRequest.index();

    if (this.processState.getLatestCommittedIndex() >= index) {
      this.processState.getLock().readLock().unlock();
    } else {
      this.processState.leaderWait(index);
    }
  }
}
