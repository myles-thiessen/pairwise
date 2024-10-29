package mthiessen.protocol.replication;

import lombok.NonNull;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.state.proposer.IProposerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class PAReplication extends BaseReplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(PAReplication.class);

  protected final EventSchedulingPrimitive eventSchedulingPrimitive;
  // Promise time
  protected final int alpha;

  public PAReplication(
      final @NonNull IRouter router,
      final @NonNull IProposerState proposerState,
      final @NonNull EventSchedulingPrimitive eventSchedulingPrimitive,
      final int alpha) {
    super(router, proposerState);
    this.eventSchedulingPrimitive = eventSchedulingPrimitive;
    this.alpha = alpha;
  }

  @Override
  protected Pair<Function<Object, Payloads.PrepareRequest>, Payloads.PrepareRequest> broadcast(
      final int index, final Payloads.GloballyUniqueRMW globallyUniqueRMW) {

    this.eventSchedulingPrimitive.initialize();

    long t = System.currentTimeMillis();

    long localTime = t + this.alpha;

    Function<Object, Payloads.PrepareRequest> messageFunction =
        follower -> {
          Pair<Long, Integer> stop = this.eventSchedulingPrimitive.at(follower, localTime);
          Payloads.PAPayload payload =
              new Payloads.PAPayload(this.router.getAllRegisteredRoutes(), stop);

          return new Payloads.PrepareRequest(index, globallyUniqueRMW, payload);
        };

    LOGGER.info("Replicating");

    return new Pair<>(messageFunction, new Payloads.PrepareRequest(index, globallyUniqueRMW, 0));
  }

  @Override
  protected void commit(
      final Payloads.PrepareRequest prepareRequest,
      final IOperationMeasurement operationMeasurement) {}
}
