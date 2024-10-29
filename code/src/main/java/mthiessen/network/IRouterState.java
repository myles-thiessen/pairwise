package mthiessen.network;

import java.util.function.Supplier;

public interface IRouterState {
  Object getIdentifier();

  void registerRequestHandlerForOp(
      final Object operation, final Supplier<IRequestHandler> handlerFactory);

  void registerResponseHandleForOp(
      final Object operation, final Supplier<IResponseHandler> handlerFactory);

  IRequestHandler getRequestHandlerForOp(final Object operation);

  IResponseHandler getResponseHandlerForOp(final Object operation, final int broadcastIdentifier);
}
