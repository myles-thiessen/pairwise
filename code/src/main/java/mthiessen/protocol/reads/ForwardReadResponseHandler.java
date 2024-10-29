package mthiessen.protocol.reads;

import mthiessen.network.CountBasedBlockingResponseHandler;

public class ForwardReadResponseHandler extends CountBasedBlockingResponseHandler {

  private Object response;

  public ForwardReadResponseHandler() {
    super(1, 1, 0);
  }

  @Override
  protected boolean acceptance(final Object response) {
    this.response = response;

    return true;
  }

  @Override
  protected Object responseState() {
    return this.response;
  }
}
