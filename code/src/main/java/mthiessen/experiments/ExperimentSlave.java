package mthiessen.experiments;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ExperimentSlave {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentSlave.class);

  public static void main(String[] args) throws IOException, InterruptedException {
    int port = Integer.parseInt(args[0]);
    LOGGER.info("Starting experiment slave with experimentPlanePort {}", port);

    Server server =
        Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
            .keepAliveTime(Integer.MAX_VALUE, TimeUnit.MILLISECONDS)
            .addService(new ExperimentService())
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .build()
            .start();

    server.awaitTermination();
  }
}
