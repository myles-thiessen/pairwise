package mthiessen.protocol.reads;

import mthiessen.network.CountBasedBlockingResponseHandler;

public class ForwardReadResponseHandler extends CountBasedBlockingResponseHandler {

  private Object response;

  public ForwardReadResponseHandler() {
    super(1, 1);
  }

  @Override
  protected void handle(final Object sender, final Object response) {
    this.response = response;
  }

  @Override
  protected Object responseState() {
    return this.response;
  }
}
