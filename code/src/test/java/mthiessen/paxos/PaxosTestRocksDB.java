package mthiessen.paxos;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import mthiessen.IProcess;
import mthiessen.experiments.ServiceProcess;
import mthiessen.misc.Pair;
import mthiessen.misc.UniqueNumberGenerator;
import mthiessen.network.ILink;
import mthiessen.network.IRouter;
import mthiessen.network.LocalLink;
import mthiessen.network.MessageTimerManager;
import mthiessen.protocol.algorithms.Algorithm;
import mthiessen.protocol.algorithms.AlgorithmFactory;
import mthiessen.protocol.reads.conflictdetection.RocksDBConflictDetection;
import mthiessen.statemachines.RocksDBStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaxosTestRocksDB {
  public static final Algorithm ALGORITHM = Algorithm.EAG;
  public static final int ops = 100000;
  public static final double writePercentage = .1;
  private static final ExecutorService service = Executors.newFixedThreadPool(10);
  private static final Logger LOGGER = LoggerFactory.getLogger(PaxosTestRocksDB.class);
  private static final int n = 3;
  private static final Random random = new Random();
  private static final AtomicInteger opCounter = new AtomicInteger(0);
  private static int writeValue = 0;
  private static long start;

  public static void main(String[] args) {
    List<Integer> nodeIdentifiers = IntStream.range(0, n).boxed().toList();

    Function<IRouter, IProcess> nodeProduce =
        (router) ->
            AlgorithmFactory.getAlgorithm(
                ALGORITHM,
                nodeIdentifiers.get(0),
                router,
                new RocksDBStateMachine("rocksdb/" + router.getRouterState().getIdentifier()),
                new RocksDBConflictDetection());

    List<ServiceProcess> nodes = new LinkedList<>();
    for (Integer nodeIdentifier : nodeIdentifiers) {
      ServiceProcess serviceNode =
          new ServiceProcess(
              nodeIdentifier, new UniqueNumberGenerator(), new MessageTimerManager(), nodeProduce);
      nodes.add(serviceNode);
    }

    for (ServiceProcess serviceProcess : nodes) {
      List<Pair<Object, ILink>> transportLinks = new LinkedList<>();
      for (ServiceProcess serviceNode1 : nodes) {
        Object identifier = serviceNode1.getIdentifier();
        ILink transportLink = new LocalLink(serviceNode1.getRouter());
        transportLinks.add(new Pair<>(identifier, transportLink));
      }
      serviceProcess.registerRoutes(transportLinks);
    }

    nodes.forEach(ServiceProcess::initialize);

    IProcess node = nodes.get(0).getProcess();

    start = System.currentTimeMillis();

    for (int i = 0; i < ops; i++) service.submit(() -> doOp(node));
  }

  private static void doOp(final IProcess process) {
    float coinFlip = random.nextFloat();

    if (coinFlip <= writePercentage) { // rmwReq
      writeValue++;
      int valueToWrite = writeValue;
      LOGGER.info("WRITE with value {}", writeValue);
      RocksDBStateMachine.WriteRequest writeRequest =
          new RocksDBStateMachine.WriteRequest(
              "test", "key", ByteBuffer.allocate(4).putInt(valueToWrite).array());
      process.rmw(writeRequest);
    } else { // read
      long start = System.currentTimeMillis();
      RocksDBStateMachine.ReadRequest readRequest =
          new RocksDBStateMachine.ReadRequest("test", "key");
      Object rawState = process.read(readRequest);
      int state =
          rawState != null && !(rawState instanceof RocksDBStateMachine.Status)
              ? ByteBuffer.wrap((byte[]) rawState).getInt()
              : 0;
      long end = System.currentTimeMillis();
      LOGGER.info("READ with expected value {} {} {}", writeValue, state, end - start);
    }

    if (opCounter.incrementAndGet() == ops) {

      long end = System.currentTimeMillis() - start;

      System.out.println(end + "ms");
      System.out.println(ops / end * 1000 + "ops/sec");
    }
    LOGGER.info("Ops done {}", opCounter.get());
  }
}
