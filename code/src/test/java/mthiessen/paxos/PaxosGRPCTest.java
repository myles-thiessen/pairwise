package mthiessen.paxos;

import lombok.SneakyThrows;
import mthiessen.IProcess;
import mthiessen.experiments.GRPCServiceProcess;
import mthiessen.misc.Util;
import mthiessen.network.IRouter;
import mthiessen.network.grpc.GRPCServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

public class PaxosGRPCTest {
  public static final int ops = 1000;
  public static final double writePercentage = .5;
  private static final Function<IRouter, IProcess> nodeProduce = null;
  private static final Logger LOGGER = LoggerFactory.getLogger(PaxosGRPCTest.class);
  private static final int n = 3;
  private static final Random random = new Random();
  private static int writeValue = 0;

  @SneakyThrows
  public static void main(String[] args) {
    List<UUID> nodeIdentifiers = IntStream.range(0, n).mapToObj(i -> UUID.randomUUID()).toList();
    String[] targets = new String[n];
    for (int index = 0; index < nodeIdentifiers.size(); index++) {
      Object nodeIdentifier = nodeIdentifiers.get(index);
      String target = String.format("localhost:%d:0:%s", 50000 + index, nodeIdentifier);
      targets[index] = target;
    }
    List<GRPCServer> grpcServers = new LinkedList<>();
    List<GRPCServiceProcess> grpcServiceNodes = new LinkedList<>();
    for (int i = 0; i < n; i++) {
      GRPCServer grpcServer = new GRPCServer(Util.split(targets[i]).dataPlanePort());
      grpcServers.add(grpcServer);
      GRPCServiceProcess grpcServiceProcess = new GRPCServiceProcess(targets[i], nodeProduce);
      grpcServiceNodes.add(grpcServiceProcess);
      grpcServer.getNetworkService().setRouter(grpcServiceProcess.getServiceProcess().getRouter());
    }

    grpcServiceNodes.forEach(node -> node.initializeRouters(targets));
    grpcServiceNodes.forEach(GRPCServiceProcess::initializeServiceNode);

    IProcess node = grpcServiceNodes.get(0).getServiceProcess().getProcess();

    List<Long> times = new LinkedList<>();
    for (int i = 0; i < ops; i++) {
      long start = System.currentTimeMillis();
      doOp(node);
      long end = System.currentTimeMillis();
      times.add(end - start);
    }

    LOGGER.info(
        "Avg {}ms Max {}ms Min {}ms", Util.average(times), Util.max(times), Util.min(times));

    Util.SCHEDULED_EXECUTOR_SERVICE.shutdownNow();
    Util.CACHED_THREAD_POOL.shutdownNow();
    grpcServiceNodes.forEach(GRPCServiceProcess::shutdown);
    grpcServers.forEach(GRPCServer::shutdown);
  }

  private static void doOp(final IProcess node) {
    float coinFlip = random.nextFloat();

    if (coinFlip < writePercentage) { // rmwReq
      writeValue++;
      int valueToWrite = writeValue;
      LOGGER.info("WRITE with value {}", writeValue);
      node.rmw(valueToWrite);
    } else { // read
      LOGGER.info("READ with expected value {}", writeValue);
      int expectedValue = writeValue;
      Object state = node.read(null);
      if (state == null) return;
      if ((Integer) state != expectedValue) {
        LOGGER.error("MISMATCH");
        System.exit(-1);
      }
    }
  }
}
