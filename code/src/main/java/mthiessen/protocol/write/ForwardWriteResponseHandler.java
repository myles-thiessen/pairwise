package mthiessen.protocol.write;

import mthiessen.network.CountBasedBlockingResponseHandler;

public class ForwardWriteResponseHandler extends CountBasedBlockingResponseHandler {
  private Object response;

  public ForwardWriteResponseHandler() {
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
