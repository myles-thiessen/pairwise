package mthiessen.paxos;

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
import mthiessen.protocol.reads.conflictdetection.IConflictDetection;
import mthiessen.statemachines.IntegerStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;

public class PaxosTest {
  public static final Algorithm ALGORITHM = Algorithm.PLFFT;
  public static final int ops = 100000;
  public static final double writePercentage = .1;
  private static final ExecutorService service = Executors.newFixedThreadPool(10);
  private static final Logger LOGGER = LoggerFactory.getLogger(PaxosTest.class);
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
                new IntegerStateMachine(),
                IConflictDetection.NOOP);

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
      process.rmw(valueToWrite);
    } else { // read
      long start = System.currentTimeMillis();
      Object state = process.read(null);
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
