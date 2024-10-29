package mthiessen.experiments.clients;

import lombok.RequiredArgsConstructor;
import mthiessen.IProcess;
import mthiessen.experiments.metrics.MetricsFactory;
import mthiessen.experiments.metrics.MetricsReporter;
import mthiessen.misc.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ThroughputReadClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(ThroughputReadClient.class);
  private static final MetricsReporter METRICS_REPORTER =
      MetricsFactory.getMetricsReporter(ThroughputReadClient.class);
  private final int numberOfWindows;
  private final IProcess node;
  private final int window;
  private final boolean measure;
  private final int discard;
  private int opsCompleted = 0;
  private int windowsCompleted = 0;

  private int count = 0;

  public void start() {
    if (this.window <= 0 || this.window > 1000) {
      LOGGER.info("Bad window size, must be in the range (0, 1000]");
      return;
    }

    ScheduledFuture<?> scheduledFuture =
        Util.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::status, 5, 5, TimeUnit.SECONDS);

    this.clientWork();

    scheduledFuture.cancel(false);
  }

  public void status() {
    System.out.println("Windows done: " + this.windowsCompleted);
  }

  private void clientWork() {
    System.out.println("Starting Throughput Read Client " + this.numberOfWindows);

    // One-second window.
    long windowStartTime = System.currentTimeMillis();

    while (this.windowsCompleted < this.numberOfWindows) {
      this.op();

      if (this.windowsCompleted >= this.discard) this.opsCompleted++;

      if (System.currentTimeMillis() > windowStartTime + this.window) {
        // Window has ended, wait then start next window

        this.windowsCompleted++;

        while (System.currentTimeMillis() < windowStartTime + 1000)
          ;

        windowStartTime = System.currentTimeMillis();
      }
    }

    if (this.measure)
      METRICS_REPORTER.report(
          METRICS_REPORTER.getUniqueNumberGenerator().getUniqueNumber(), "OPS", this.opsCompleted);

    System.out.println("Finished with size " + METRICS_REPORTER.getReportedMetrics().size());
  }

  private void op() {
    long start = System.currentTimeMillis();
    this.node.read(null);
    long end = System.currentTimeMillis();
    long time = end - start;
    if (this.measure && time > 1) {
      METRICS_REPORTER.report(
          METRICS_REPORTER.getUniqueNumberGenerator().getUniqueNumber(), "READ_B", this.count);
      METRICS_REPORTER.report(
          METRICS_REPORTER.getUniqueNumberGenerator().getUniqueNumber(), "READ", time);
      this.count = 0;
    } else {
      this.count++;
    }
  }
}
