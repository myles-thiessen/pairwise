package mthiessen.network.grpc;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import lombok.Getter;

import java.io.IOException;

public class GRPCServer {

  @Getter private final NetworkService networkService = new NetworkService();

  @Getter private final Server server;

  public GRPCServer(final int port) throws IOException {
    this.server =
        Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
            .addService(this.networkService)
            .build()
            .start();
  }

  public void shutdown() {
    this.server.shutdownNow();
  }
}
