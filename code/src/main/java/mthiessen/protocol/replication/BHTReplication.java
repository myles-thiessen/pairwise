package mthiessen.protocol.replication;

import lombok.NonNull;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.proposer.IProposerState;

import java.util.function.Function;

public class BHTReplication extends BaseReplication {

  // Promise time of BHT
  private final int alpha;

  private final int delta;

  public BHTReplication(
      final @NonNull IRouter router,
      final @NonNull IProposerState proposerState,
      final int alpha,
      final int delta) {
    super(router, proposerState);
    this.alpha = alpha;
    this.delta = delta;
  }

  @Override
  protected Pair<Function<Object, Payloads.WriteRequest>,
      Payloads.WriteRequest> broadcast(
      final int index, final Object write) {
    Payloads.WriteRequest writeRequest =
        new Payloads.WriteRequest(index, write,
            System.currentTimeMillis() + this.alpha);
    return new Pair<>(sender -> writeRequest, writeRequest);
  }

  @Override
  protected void commit(
      final Payloads.WriteRequest writeRequest,
      final IOperationMeasurement operationMeasurement) {
    MetricsAttachedRequest<Payloads.WriteRequest> commitRequest =
        new MetricsAttachedRequest<>(writeRequest,
            operationMeasurement.getOperationIdentifier());

    this.router.broadcastRequestToAll(OPS.COMMIT, commitRequest, true);

    // post-commit
    operationMeasurement.record(Event.POST_BHT_COMMIT_BROADCAST);

    long time = (long) writeRequest.payload();

    long timeLeft = time + this.delta - System.currentTimeMillis();

    if (timeLeft > 0)
      try {
        Thread.sleep(timeLeft);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
  }
}
