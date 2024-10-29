package mthiessen.protocol.reads;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.network.BroadcastResponse;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.leaderelection.ILeaderElectionService;
import mthiessen.protocol.state.process.IProcessState;
import mthiessen.protocol.state.proposer.IProposerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@RequiredArgsConstructor
public class LRReads extends ReadSynchronizer implements IReadOp {

  private static final Logger LOGGER = LoggerFactory.getLogger(LRReads.class);

  @NonNull private final IRouter router;

  @NonNull private final Runnable leaderElectionOp;

  @NonNull private final ILeaderElectionService leaderElectionService;

  @NonNull private final IProposerState proposerState;

  @NonNull private final IProcessState processState;

  @NonNull private final IStateMachine stateMachine;

  @Override
  public Object read(final Object readReq, final IOperationMeasurement operationMeasurement) {

    this.leaderElectionOp.run();

    Object value;

    if (!this.proposerState.isLeader()) {
      value = this.forwardRead(readReq, operationMeasurement);
    } else {
      value = super.read(this.processState, this.stateMachine, readReq, operationMeasurement);
    }

    return value;
  }

  private Object forwardRead(
      final Object readReq, final IOperationMeasurement operationMeasurement) {

    LOGGER.info("Forwarding read request");

    operationMeasurement.record(Event.PRE_READ_FORWARD);

    MetricsAttachedRequest<Object> forwardReadRequest =
        new MetricsAttachedRequest<>(readReq, operationMeasurement.getOperationIdentifier());

    int broadcastIdentifier =
        this.router.broadcastRequest(
            Collections.singleton(this.leaderElectionService.leader()),
            OPS.FORWARD_READ,
            forwardReadRequest,
            true);

    // TODO handle failure
    BroadcastResponse response = this.router.waitForResponse(OPS.FORWARD_READ, broadcastIdentifier);

    operationMeasurement.record(Event.POST_READ_FORWARD_RESPONSE);

    LOGGER.info("Got forwarded read response");

    return response.responsePayload();
  }

  @Override
  protected int stampRead(final Object readReq) {
    return this.proposerState.getLatestBroadcastIndex();
  }
}
