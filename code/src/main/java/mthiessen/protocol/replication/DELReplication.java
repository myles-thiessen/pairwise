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

public class DELReplication extends BaseReplication {

  // Promise time of DEL
  private final int alpha;

  private final int delta;

  public DELReplication(
      final @NonNull IRouter router,
      final @NonNull IProposerState proposerState,
      final int alpha,
      final int delta) {
    super(router, proposerState);
    this.alpha = alpha;
    this.delta = delta;
  }

  @Override
  protected Pair<Function<Object, Payloads.PrepareRequest>, Payloads.PrepareRequest> broadcast(
      final int index, final Payloads.GloballyUniqueRMW globallyUniqueRMW) {
    Payloads.PrepareRequest prepareRequest =
        new Payloads.PrepareRequest(
            index, globallyUniqueRMW, System.currentTimeMillis() + this.alpha);
    return new Pair<>(sender -> prepareRequest, prepareRequest);
  }

  @Override
  protected void commit(
      final Payloads.PrepareRequest prepareRequest,
      final IOperationMeasurement operationMeasurement) {
    MetricsAttachedRequest<Payloads.PrepareRequest> commitRequest =
        new MetricsAttachedRequest<>(prepareRequest, operationMeasurement.getOperationIdentifier());

    this.router.broadcastRequestToAll(OPS.COMMIT, commitRequest, true);

    // post-commit
    operationMeasurement.record(Event.POST_DEL_COMMIT_BROADCAST);

    long time = (long) prepareRequest.payload();

    long timeLeft = time + this.delta - System.currentTimeMillis();

    if (timeLeft > 0)
      try {
        Thread.sleep(timeLeft);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
  }
}
