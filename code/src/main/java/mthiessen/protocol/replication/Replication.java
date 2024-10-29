package mthiessen.protocol.replication;

import lombok.NonNull;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.state.proposer.IProposerState;

import java.util.function.Function;

public class Replication extends BaseReplication {

  public Replication(final @NonNull IRouter router, final @NonNull IProposerState proposerState) {
    super(router, proposerState);
  }

  @Override
  protected Pair<Function<Object, Payloads.PrepareRequest>, Payloads.PrepareRequest> broadcast(
      final int index, final Payloads.GloballyUniqueRMW globallyUniqueRMW) {
    Payloads.PrepareRequest prepareRequest =
        new Payloads.PrepareRequest(index, globallyUniqueRMW, 0);
    return new Pair<>(sender -> prepareRequest, prepareRequest);
  }

  @Override
  protected void commit(
      final Payloads.PrepareRequest prepareRequest,
      final IOperationMeasurement operationMeasurement) {

    // Safety hear is based on the fast the writes and reads are atomic to
    // integers.
    super.proposerState.setLatestBroadcastIndex(prepareRequest.index());

    MetricsAttachedRequest<Payloads.PrepareRequest> commitRequest =
        new MetricsAttachedRequest<>(prepareRequest, operationMeasurement.getOperationIdentifier());

    this.router.broadcastRequestToAll(OPS.COMMIT, commitRequest, true);
  }
}
