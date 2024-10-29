package mthiessen.network;

public interface IResponseHandler {

  void execute(final Object sender, final Object response);

  BroadcastResponse waitForResponse();
}
