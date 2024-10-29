package mthiessen.experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ExperimentSlave {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExperimentSlave.class);

  public static void main(String[] args) throws IOException,
      InterruptedException {
    int port = Integer.parseInt(args[0]);
    LOGGER.info("Starting experiment slave with experimentPlanePort {}", port);

    ExperimentGRPCServer server = new ExperimentGRPCServer(port);
    server.getServer().awaitTermination();
  }
}
