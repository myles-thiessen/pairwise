package mthiessen.network;

import lombok.RequiredArgsConstructor;
import mthiessen.protocol.OPS;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class LocalLink implements ILink {

  private final IRouter router;

  private final ExecutorService forwardService = Executors.newSingleThreadExecutor();
  private final ExecutorService commitService = Executors.newSingleThreadExecutor();
  private final ExecutorService regularService = Executors.newSingleThreadExecutor();

  private ExecutorService getService(final Object operations) {
    ExecutorService service;
    switch ((String) operations) {
      case OPS.FORWARD_RMW, OPS.FORWARD_READ -> service = this.forwardService;
      case OPS.COMMIT -> service = this.commitService;
      default -> service = this.regularService;
    }
    return service;
  }

  @Override
  public void handleRequest(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object request,
      final boolean async) {
    this.getService(operation)
        .submit(
            () ->
                this.router.handleRequest(
                    sender, operation, broadcastRequestNumber, time, request));
  }

  @Override
  public void handleResponse(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object response) {
    this.getService(operation)
        .submit(
            () ->
                this.router.handleResponse(
                    sender, operation, broadcastRequestNumber, time, response));
  }
}
