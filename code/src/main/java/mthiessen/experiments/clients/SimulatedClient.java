package mthiessen.experiments.clients;

import lombok.RequiredArgsConstructor;
import mthiessen.IProcess;
import mthiessen.experiments.metrics.MetricsFactory;
import mthiessen.experiments.metrics.MetricsReporter;
import mthiessen.misc.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
public class SimulatedClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedClient.class);
  private static final MetricsReporter METRICS_REPORTER =
      MetricsFactory.getMetricsReporter(SimulatedClient.class);
  private final Random random = new Random();
  private final long delay;
  private final int numberOfOps;
  private final IProcess node;
  private final int outstandingLimit = 10000;
  private final AtomicInteger count = new AtomicInteger(0);
  private final int discard;
  private final float writePercentage;
  private final Lock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();
  private int outstanding = 0;

  public void start() {
    Util.SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(
        this::schedule, this.delay, this.delay, TimeUnit.MICROSECONDS);

    try {
      this.lock.lock();
      this.condition.await();
      this.lock.unlock();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    System.out.println("Done latch " + METRICS_REPORTER.getReportedMetrics().size());
  }

  private void schedule() {
    int op = this.count.getAndIncrement();
    int identifier = METRICS_REPORTER.getUniqueNumberGenerator().getUniqueNumber();

    if (op >= this.numberOfOps) {
      this.lock.lock();
      this.condition.signal();
      this.lock.unlock();
      return;
    }

    boolean write;
    if (this.writePercentage == 0) {
      write = false;
    } else if (this.writePercentage == 1) {
      write = true;
    } else {
      write = this.random.nextFloat() <= this.writePercentage;
    }

    // We only care about outstanding reads.
    if (!write && this.outstandingLimit <= this.outstanding) {
      return;
    }

    Util.CACHED_THREAD_POOL.submit(() -> op(op, identifier, write));
  }

  private void op(final int op, final int identifier, final boolean write) {
    this.outstanding++;

    long start = System.nanoTime();

    if (write) {
      this.node.rmw(1);
    } else {
      this.node.read(null);
    }

    long end = System.nanoTime();

    if (op >= this.discard) {
      String marker = write ? "WRITE" : "READ";
      double measurement = (end - start) / 1_000_000d;
      LOGGER.info("{} OP took {}ms left", marker, measurement);
      METRICS_REPORTER.report(identifier, marker, measurement);
    }

    this.outstanding--;
  }
}
