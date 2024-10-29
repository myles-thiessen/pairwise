package mthiessen.protocol.algorithms;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IProcess;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.misc.Pair;
import mthiessen.network.BroadcastResponse;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.leaderelection.ILeaderElectionService;
import mthiessen.protocol.reads.IReadOp;
import mthiessen.protocol.replication.IReplicationOp;
import mthiessen.protocol.state.proposer.IProposerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
public abstract class BaseProcess implements IProcess {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BaseProcess.class);

  @NonNull
  protected final IRouter router;

  @NonNull
  protected final IMeasurementCollector measurementCollector;

  @NonNull
  protected final Runnable leaderElectionOp;

  @NonNull
  protected final ILeaderElectionService leaderElectionService;

  @NonNull
  protected final IProposerState proposerState;

  @NonNull
  protected final IReplicationOp replicationOp;

  @NonNull
  protected final IReadOp readOp;

  @Override
  public void write(final Object write) {
    IOperationMeasurement operationMeasurement =
        this.measurementCollector.registerNewOperation(true);
    operationMeasurement.record(Event.INIT);

    this.processReplicationOperation(write, operationMeasurement);
  }

  @Override
  public Object read(final Object readReq) {
    IOperationMeasurement operationMeasurement =
        this.measurementCollector.registerNewOperation(false);
    operationMeasurement.record(Event.INIT);

    return this.readOp.read(readReq, operationMeasurement);
  }

  private void processReplicationOperation(
      final Object write, final IOperationMeasurement operationMeasurement) {
    this.leaderElectionOp.run();

    if (!this.proposerState.isLeader()) {
      // Process is not leader, forward to leader.
      this.forward(write, operationMeasurement);
    } else {
      // Process is leader, do leader work.
      this.replicationOp.replicate(write, operationMeasurement);
    }
  }

  private void forward(final Object write,
                       final IOperationMeasurement operationMeasurement) {
    LOGGER.info("Forwarding write request");

    operationMeasurement.record(Event.PRE_WRITE_FORWARD);

    MetricsAttachedRequest<Object> forwardWriteRequest =
        new MetricsAttachedRequest<>(write,
            operationMeasurement.getOperationIdentifier());

    int broadcastIdentifier =
        this.router.broadcastRequest(
            Collections.singleton(this.leaderElectionService.leader()),
            OPS.FORWARD_WRITE,
            forwardWriteRequest);

    LOGGER.info("Waiting for forwarded response {}", broadcastIdentifier);

    BroadcastResponse response =
        this.router.waitForResponse(OPS.FORWARD_WRITE, broadcastIdentifier);

    LOGGER.info("Receiving forwarded response");

    operationMeasurement.record(Event.POST_WRITE_FORWARD_RESPONSE);
  }

  @Override
  public void setInstrumentation(final boolean active) {
    this.measurementCollector.setInstrumentation(active);
  }

  @Override
  public Map<Pair<Object, Integer>, IOperationMeasurement> getTrace() {
    return this.measurementCollector.getOperationMeasurements();
  }
}
