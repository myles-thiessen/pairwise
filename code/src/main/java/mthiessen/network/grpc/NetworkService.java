package mthiessen.network.grpc;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.Setter;
import mthiessen.grpc.NetworkGrpc;
import mthiessen.grpc.NetworkOuterClass;
import mthiessen.network.IRouter;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class NetworkService extends NetworkGrpc.NetworkImplBase {

  @Setter private IRouter router;

  @Override
  public void handleRequest(
      final NetworkOuterClass.Message request,
      final StreamObserver<NetworkOuterClass.Message> responseObserver) {

    Object sender = SerializationUtils.deserialize(request.getSender().toByteArray());
    Object operation = SerializationUtils.deserialize(request.getOperation().toByteArray());
    int broadcastRequestNumber = request.getBroadcastRequestNumber();
    long time = request.getTime();
    Object requestPayload = SerializationUtils.deserialize(request.getPayload().toByteArray());

    Object responsePayload =
        this.router.handleRequestWithResponse(
            sender, operation, broadcastRequestNumber, time, requestPayload);
    ByteString responsePayloadBytes =
        ByteString.copyFrom(SerializationUtils.serialize((Serializable) responsePayload));

    Object thisNode = this.router.getRouterState().getIdentifier();
    ByteString thisNodeBytes =
        ByteString.copyFrom(SerializationUtils.serialize((Serializable) thisNode));

    NetworkOuterClass.Message response =
        NetworkOuterClass.Message.newBuilder()
            .setSender(thisNodeBytes)
            .setOperation(request.getOperation())
            .setBroadcastRequestNumber(broadcastRequestNumber)
            .setTime(time)
            .setPayload(responsePayloadBytes)
            .build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
