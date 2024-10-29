package mthiessen.protocol.replication;

import lombok.NonNull;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.state.process.IProcessState;
import mthiessen.protocol.state.proposer.IProposerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class US2Replication extends BaseReplication {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(US2Replication.class);

  protected final EventSchedulingPrimitive eventSchedulingPrimitive;
  // Promise time
  protected final int alpha;
  private final IProcessState processState;

  public US2Replication(
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

    this.eventSchedulingPrimitive.initialize();

    long t = System.currentTimeMillis();

    long localTime = t + this.alpha;

    Function<Object, Payloads.WriteRequest> messageFunction =
        follower -> {
          Pair<Long, Integer> stop = this.eventSchedulingPrimitive.at(follower
              , localTime);
          Payloads.US2Payload payload = new Payloads.US2Payload(stop);

          return new Payloads.WriteRequest(index, write, payload);
        };

    LOGGER.info("Replicating");

    return new Pair<>(
        messageFunction, new Payloads.WriteRequest(index, write, 0));
  }

  @Override
  protected void commit(
      final Payloads.WriteRequest writeRequest,
      final IOperationMeasurement operationMeasurement) {
    this.processState.getLock().readLock().lock();

    int index = writeRequest.index();

    if (this.processState.getLatestCommittedIndex() >= index) {
      this.processState.getLock().readLock().unlock();
    } else {
      this.processState.leaderWait(index);
    }
  }
}
