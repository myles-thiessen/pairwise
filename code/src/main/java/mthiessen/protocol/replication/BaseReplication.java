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
import mthiessen.protocol.state.proposer.IProposerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

@RequiredArgsConstructor
public abstract class BaseReplication implements IReplicationOp {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseReplication.class);

  @NonNull protected final IRouter router;

  @NonNull protected final IProposerState proposerState;

  protected abstract Pair<Function<Object, Payloads.PrepareRequest>, Payloads.PrepareRequest>
      broadcast(final int index, final Payloads.GloballyUniqueRMW globallyUniqueRMW);

  protected abstract void commit(
      final Payloads.PrepareRequest prepareRequest,
      final IOperationMeasurement operationMeasurement);

  @Override
  public void replicate(
      final Payloads.GloballyUniqueRMW globallyUniqueRMW,
      final IOperationMeasurement operationMeasurement) {

    int index = this.proposerState.getLatestAssignedIndex().incrementAndGet();

    LOGGER.info("Processing rmwReq with index {}", index);

    Pair<Function<Object, Payloads.PrepareRequest>, Payloads.PrepareRequest> bResponse =
        this.broadcast(index, globallyUniqueRMW);

    Function<Object, Payloads.PrepareRequest> writeRequestFunction = bResponse.k1();
    Payloads.PrepareRequest prepareRequest = bResponse.k2();

    // pre-broadcast
    operationMeasurement.record(Event.PRE_PREPARE_BROADCAST);

    int broadcastIdentifier =
        this.router.broadcastPersonalizedRequestToAll(
            OPS.PREPARE,
            sender ->
                new MetricsAttachedRequest<>(
                    writeRequestFunction.apply(sender),
                    operationMeasurement.getOperationIdentifier()),
            true);

    BroadcastResponse broadcastResponse =
        this.router.waitForResponse(OPS.PREPARE, broadcastIdentifier);

    LOGGER.info("Got response " + broadcastResponse.status());

    // post-response
    operationMeasurement.record(Event.POST_PREPARE_RESPONSE);

    LOGGER.info("Sending commit for index {}", index);

    this.commit(prepareRequest, operationMeasurement);

    // post-commit
    operationMeasurement.record(Event.POST_COMMIT_CAlL);
  }
}
