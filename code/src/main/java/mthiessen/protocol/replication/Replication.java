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

  public Replication(final @NonNull IRouter router,
                     final @NonNull IProposerState proposerState) {
    super(router, proposerState);
  }

  @Override
  protected Pair<Function<Object, Payloads.WriteRequest>,
      Payloads.WriteRequest> broadcast(
      final int index, final Object write) {
    Payloads.WriteRequest writeRequest = new Payloads.WriteRequest(index,
        write, 0);
    return new Pair<>(sender -> writeRequest, writeRequest);
  }

  @Override
  protected void commit(
      final Payloads.WriteRequest writeRequest,
      final IOperationMeasurement operationMeasurement) {

    // Safety hear is based on the fast the writes and reads are atomic to
    // integers.
    super.proposerState.setLatestBroadcastIndex(writeRequest.index());

    MetricsAttachedRequest<Payloads.WriteRequest> commitRequest =
        new MetricsAttachedRequest<>(writeRequest,
            operationMeasurement.getOperationIdentifier());

    this.router.broadcastRequestToAll(OPS.COMMIT, commitRequest, true);
  }
}
