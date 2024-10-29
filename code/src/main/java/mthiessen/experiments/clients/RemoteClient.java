package mthiessen.experiments.clients;

import com.google.protobuf.ByteString;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import mthiessen.grpc.ExperimentGrpc;
import mthiessen.grpc.NetworkOuterClass;
import mthiessen.statemachines.RocksDBStateMachine;

public class RemoteClient {

  private final ExperimentGrpc.ExperimentBlockingStub stub;

  public RemoteClient(final String ip, final int port) {
    String format = String.format("%s:%d", ip, port);
    ManagedChannel channel =
        Grpc.newChannelBuilder(format, InsecureChannelCredentials.create())
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .maxInboundMetadataSize(Integer.MAX_VALUE)
            .build();
    this.stub = ExperimentGrpc.newBlockingStub(channel);
  }

  public void insert(final RocksDBStateMachine.WriteRequest writeRequest) {
    NetworkOuterClass.InsertRequest insertRequest =
        NetworkOuterClass.InsertRequest.newBuilder()
            .setTable(writeRequest.table())
            .setKey(writeRequest.key())
            .setValue(ByteString.copyFrom(writeRequest.value()))
            .build();

    this.stub.insert(insertRequest);
  }

  public byte[] read(final RocksDBStateMachine.ReadRequest readRequest) {
    NetworkOuterClass.ReadRequest readRequest1 =
        NetworkOuterClass.ReadRequest.newBuilder()
            .setTable(readRequest.table())
            .setKey(readRequest.key())
            .build();

    NetworkOuterClass.ReadResponse response = this.stub.read(readRequest1);

    return response.hasValue() ? response.getValue().toByteArray() : null;
  }
}
