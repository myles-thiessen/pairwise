package mthiessen.network.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Context;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import mthiessen.grpc.NetworkGrpc;
import mthiessen.grpc.NetworkOuterClass;
import mthiessen.network.ILink;
import mthiessen.network.IRouter;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class GRPCLink implements ILink {

  private final IRouter router;

  private final ManagedChannel channel;

  private final NetworkGrpc.NetworkStub stub;

  public GRPCLink(final IRouter router, final String ip, final int port) {
    this.router = router;
    String target = String.format("%s:%d", ip, port);
    this.channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
    this.stub = NetworkGrpc.newStub(this.channel);
  }

  @Override
  public void handleRequest(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object request,
      final boolean async) {
    NetworkOuterClass.Message message =
        NetworkOuterClass.Message.newBuilder()
            .setSender(ByteString.copyFrom(SerializationUtils.serialize((Serializable) sender)))
            .setOperation(
                ByteString.copyFrom(SerializationUtils.serialize((Serializable) operation)))
            .setBroadcastRequestNumber(broadcastRequestNumber)
            .setTime(time)
            .setPayload(ByteString.copyFrom(SerializationUtils.serialize((Serializable) request)))
            .build();

    if (async) {
      Context.current().fork().run(() -> run(operation, message));
    } else {
      run(operation, message);
    }
  }

  private void run(final Object operation, final NetworkOuterClass.Message message) {
    if (this.channel.isTerminated() || this.channel.isShutdown()) return;

    this.stub.handleRequest(
        message,
        new StreamObserver<>() {
          @Override
          public void onNext(final NetworkOuterClass.Message message) {
            Object sender = SerializationUtils.deserialize(message.getSender().toByteArray());
            Object operation = SerializationUtils.deserialize(message.getOperation().toByteArray());
            int broadcastRequestNumber = message.getBroadcastRequestNumber();
            long time = message.getTime();
            Object response = SerializationUtils.deserialize(message.getPayload().toByteArray());
            handleResponse(sender, operation, broadcastRequestNumber, time, response);
          }

          @Override
          public void onError(Throwable throwable) {
            channel.shutdown();
            // throw new RuntimeException(operation.toString(), throwable);
          }

          @Override
          public void onCompleted() {}
        });
  }

  @Override
  public void handleResponse(
      final Object sender,
      final Object operation,
      final int broadcastRequestNumber,
      final long time,
      final Object response) {
    this.router.handleResponse(sender, operation, broadcastRequestNumber, time, response);
  }

  @Override
  public void shutdown() {
    this.channel.shutdown();
  }
}
