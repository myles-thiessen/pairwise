package mthiessen.experiments;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import lombok.Getter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ExperimentGRPCServer {
  @Getter
  private final Server server;

  public ExperimentGRPCServer(final int port) throws IOException {
    this.server =
        Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
            .keepAliveTime(Integer.MAX_VALUE, TimeUnit.MILLISECONDS)
            .addService(new ExperimentService())
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .build()
            .start();
  }
}
