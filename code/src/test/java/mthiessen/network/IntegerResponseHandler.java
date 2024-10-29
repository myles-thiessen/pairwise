package mthiessen.network;

public class IntegerResponseHandler implements IResponseHandler {

  private int count = 0;

  @Override
  public void execute(final Object sender, final Object response) {
    System.out.println("Got " + response + " as a response");
    System.out.println(++count);
  }

  @Override
  public BroadcastResponse waitForResponse() {
    BroadcastResponseStatus responseStatus = BroadcastResponseStatus.TIMED_OUT;
    return new BroadcastResponse(responseStatus, null);
  }
}
