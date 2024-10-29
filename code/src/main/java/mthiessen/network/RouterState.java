package mthiessen.network;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class RouterState implements IRouterState {

  private final Map<Object, Supplier<IRequestHandler>> requestHandlerFactoryMap = new HashMap<>();

  private final Map<Object, Supplier<IResponseHandler>> responseHandlerFactoryMap = new HashMap<>();

  private final Map<Integer, IResponseHandler> responseHandlerMap = new ConcurrentHashMap<>();

  @Getter private final Object identifier;

  @Override
  public void registerRequestHandlerForOp(
      final Object operation, final Supplier<IRequestHandler> handlerFactory) {
    this.requestHandlerFactoryMap.put(operation, handlerFactory);
  }

  @Override
  public void registerResponseHandleForOp(
      final Object operation, final Supplier<IResponseHandler> handlerFactory) {
    this.responseHandlerFactoryMap.put(operation, handlerFactory);
  }

  @Override
  public IRequestHandler getRequestHandlerForOp(final Object operation) {
    return this.requestHandlerFactoryMap.get(operation).get();
  }

  @Override
  public IResponseHandler getResponseHandlerForOp(
      final Object operation, final int broadcastIdentifier) {
    Supplier<IResponseHandler> responseHandlerSupplier =
        this.responseHandlerFactoryMap.get(operation);
    if (responseHandlerSupplier != null)
      return this.responseHandlerMap.computeIfAbsent(
          broadcastIdentifier, t -> responseHandlerSupplier.get());
    else return null;
  }
}
