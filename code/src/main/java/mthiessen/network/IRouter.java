package mthiessen.network;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

public interface IRouter {
  IRouterState getRouterState();

  IMessageTimerManager getMessageTimerManager();

  Set<Object> getAllRegisteredRoutes();

  void registerRouter(final Object identifier, final ILink linkToRouter);

  default int broadcastRequestToAll(final Object operation, final Object request) {
    return this.broadcastRequestToAll(operation, request, false);
  }

  default int broadcastRequestToAll(
      final Object operation, final Object request, final boolean async) {
    return this.broadcastRequest(this.getAllRegisteredRoutes(), operation, request, async);
  }

  default int broadcastRequest(
      final Collection<?> processes, final Object operation, final Object request) {
    return this.broadcastRequest(processes, operation, request, false);
  }

  int broadcastRequest(
      final Collection<?> processes,
      final Object operation,
      final Object request,
      final boolean async);

  default int broadcastPersonalizedRequestToAll(
      final Object operation, final Function<Object, Object> requestFactory) {
    return this.broadcastPersonalizedRequestToAll(operation, requestFactory, false);
  }

  default int broadcastPersonalizedRequestToAll(
      final Object operation, final Function<Object, Object> requestFactory, final boolean async) {
    return this.broadcastPersonalizedRequest(
        this.getAllRegisteredRoutes(), operation, requestFactory, async);
  }

  int broadcastPersonalizedRequest(
      final Collection<?> processes,
      final Object operation,
      final Function<Object, Object> requestFactory,
      final boolean async);

  void handleRequest(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object request);

  Object handleRequestWithResponse(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object request);

  void handleResponse(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object response);

  BroadcastResponse waitForResponse(final Object operation, final int broadcastRequestNumber);
}
