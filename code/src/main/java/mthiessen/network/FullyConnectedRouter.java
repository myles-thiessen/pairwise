package mthiessen.network;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IUniqueNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

@RequiredArgsConstructor
public class FullyConnectedRouter implements IRouter {

  private static final Logger LOGGER = LoggerFactory.getLogger(FullyConnectedRouter.class);

  private final Map<Object, ILink> links = new HashMap<>();

  @NonNull private final IUniqueNumberGenerator broadcastIdentifierManager;
  @Getter @NonNull private final IRouterState routerState;

  @Getter @NonNull private final IMessageTimerManager messageTimerManager;

  @Override
  public Set<Object> getAllRegisteredRoutes() {
    return Set.copyOf(this.links.keySet());
  }

  @Override
  public void registerRouter(final Object identifier, final ILink linkToRouter) {
    this.links.put(identifier, linkToRouter);
  }

  private Optional<ILink> getRouterByIdentifier(final Object identifier) {
    return Optional.of(this.links.get(identifier));
  }

  @Override
  public int broadcastRequest(
      final Collection<?> processes,
      final Object operation,
      final Object request,
      final boolean async) {
    return this.broadcastPersonalizedRequest(processes, operation, process -> request, async);
  }

  @Override
  public int broadcastPersonalizedRequest(
      final Collection<?> processes,
      final Object operation,
      final Function<Object, Object> requestFactory,
      final boolean async) {
    int broadcastIdentifier = this.broadcastIdentifierManager.getUniqueNumber();

    for (Object identifier : processes) {
      Optional<ILink> optionalIRouterProcess = this.getRouterByIdentifier(identifier);

      if (optionalIRouterProcess.isEmpty()) {
        LOGGER.error("Could not find rounder with identifier {}", identifier);
        continue;
      }

      ILink destinationRouter = optionalIRouterProcess.get();
      Object senderIdentity = this.getRouterState().getIdentifier();

      destinationRouter.handleRequest(
          senderIdentity,
          operation,
          broadcastIdentifier,
          System.currentTimeMillis(),
          requestFactory.apply(identifier),
          async);
    }

    return broadcastIdentifier;
  }

  @Override
  public void handleRequest(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object request) {

    Optional<ILink> optionalSenderRouter = this.getRouterByIdentifier(sender);

    if (optionalSenderRouter.isEmpty()) {
      LOGGER.error("Sender rounder cannot be found with identifier {}", sender);
      return;
    }

    ILink senderRouter = optionalSenderRouter.get();

    Object response =
        this.handleRequestWithResponse(sender, operation, broadcastRequestNumber, time, request);
    Object senderIdentity = this.getRouterState().getIdentifier();

    senderRouter.handleResponse(senderIdentity, operation, broadcastRequestNumber, time, response);
  }

  @Override
  public Object handleRequestWithResponse(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object request) {
    IRequestHandler requestHandler = this.getRouterState().getRequestHandlerForOp(operation);

    return requestHandler.execute(sender, request);
  }

  @Override
  public void handleResponse(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object response) {
    this.messageTimerManager.recordMessageTime(
        sender, operation, System.currentTimeMillis() - time);

    IResponseHandler responseHandler =
        this.getRouterState().getResponseHandlerForOp(operation, broadcastRequestNumber);

    if (responseHandler != null) responseHandler.execute(sender, response);
  }

  @Override
  public BroadcastResponse waitForResponse(
      final Object operation, final int broadcastRequestNumber) {
    return this.getRouterState()
        .getResponseHandlerForOp(operation, broadcastRequestNumber)
        .waitForResponse();
  }
}
