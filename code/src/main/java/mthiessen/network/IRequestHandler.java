package mthiessen.network;

public interface IRequestHandler {
  Object execute(final Object sender, final Object request);
}
