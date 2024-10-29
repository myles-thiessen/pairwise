package mthiessen.network;

public interface ILink {
  void handleRequest(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object request,
      final boolean async);

  void handleResponse(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object response);

  default void shutdown() {}
}
