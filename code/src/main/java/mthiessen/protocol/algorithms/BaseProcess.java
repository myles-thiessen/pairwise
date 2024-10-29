package mthiessen.protocol.algorithms;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IProcess;
import mthiessen.IUniqueNumberGenerator;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.MetricsAttachedRequest;
import mthiessen.misc.Pair;
import mthiessen.misc.UniqueNumberGenerator;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.leaderelection.ILeaderElectionService;
import mthiessen.protocol.reads.IReadOp;
import mthiessen.protocol.replication.IReplicationOp;
import mthiessen.protocol.state.process.IProcessState;
import mthiessen.protocol.state.proposer.IProposerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
public abstract class BaseProcess implements IProcess {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseProcess.class);

  @NonNull protected final IRouter router;

  @NonNull protected final IMeasurementCollector measurementCollector;

  @NonNull protected final Runnable leaderElectionOp;

  @NonNull protected final ILeaderElectionService leaderElectionService;

  @NonNull protected final IProposerState proposerState;

  @NonNull protected final IProcessState processState;

  @NonNull protected final IReplicationOp replicationOp;

  @NonNull protected final IReadOp readOp;

  private final IUniqueNumberGenerator writeIdGenerator = new UniqueNumberGenerator();

  @Override
  public Object rmw(final Object rmwReq) {
    IOperationMeasurement operationMeasurement =
        this.measurementCollector.registerNewOperation(true);
    operationMeasurement.record(Event.INIT);

    return this.processReplicationOperation(rmwReq, operationMeasurement);
  }

  @Override
  public Object read(final Object readReq) {
    IOperationMeasurement operationMeasurement =
        this.measurementCollector.registerNewOperation(false);
    operationMeasurement.record(Event.INIT);

    return this.readOp.read(readReq, operationMeasurement);
  }

  private Object processReplicationOperation(
      final Object write, final IOperationMeasurement operationMeasurement) {

    Object processId = this.router.getRouterState().getIdentifier();
    int locallyUniqueId = this.writeIdGenerator.getUniqueNumber();

    Payloads.GloballyUniqueRMW globallyUniqueRMW =
        new Payloads.GloballyUniqueRMW(processId, locallyUniqueId, write);

    this.leaderElectionOp.run();

    if (!this.proposerState.isLeader()) {
      // Process is not leader, forward to leader.
      this.forward(globallyUniqueRMW, operationMeasurement);
    } else {
      // Process is leader, do leader work.
      this.replicationOp.replicate(globallyUniqueRMW, operationMeasurement);
    }

    Object response = this.processState.getResponse(locallyUniqueId);

    LOGGER.info("Got response");

    operationMeasurement.record(Event.POST_RMW_FORWARD_RESPONSE);

    return response;
  }

  private void forward(
      final Payloads.GloballyUniqueRMW globallyUniqueRMW,
      final IOperationMeasurement operationMeasurement) {
    LOGGER.info("Forwarding rmwReq request");

    operationMeasurement.record(Event.PRE_RMW_FORWARD);

    MetricsAttachedRequest<Object> forwardWriteRequest =
        new MetricsAttachedRequest<>(
            globallyUniqueRMW, operationMeasurement.getOperationIdentifier());

    this.router.broadcastRequest(
        Collections.singleton(this.leaderElectionService.leader()),
        OPS.FORWARD_RMW,
        forwardWriteRequest,
        true);

    LOGGER.info("Waiting for response");
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
