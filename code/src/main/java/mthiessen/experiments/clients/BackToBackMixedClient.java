package mthiessen.experiments.clients;

import lombok.RequiredArgsConstructor;
import mthiessen.IProcess;
import mthiessen.experiments.metrics.MetricsFactory;
import mthiessen.experiments.metrics.MetricsReporter;
import mthiessen.misc.Util;

import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class BackToBackMixedClient {
  private static final MetricsReporter METRICS_REPORTER =
      MetricsFactory.getMetricsReporter(BackToBackMixedClient.class);
  private final Random random = new Random();
  private final IProcess node;
  private final float rmwPercentage;
  private final int seconds;
  private final boolean measure;
  private double rmwTime = 0;
  private double rmwCompleted = 0;
  private double readTime = 0;
  private double readCompleted = 0;

  public void start() {
    ScheduledFuture<?> scheduledFuture =
        Util.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::status, 5, 5, TimeUnit.SECONDS);

    this.clientWork();

    scheduledFuture.cancel(false);
  }

  public void status() {
    System.out.println("Ops done: " + (this.rmwCompleted + this.readCompleted));
  }

  private void clientWork() {
    System.out.println("Starting Back to Back Mixed Client " + this.rmwPercentage);

    long end = System.currentTimeMillis() + this.seconds * 1000L;

    while (System.currentTimeMillis() < end) {
      this.op();
    }

    if (this.measure) {

      METRICS_REPORTER.report(
          METRICS_REPORTER.getUniqueNumberGenerator().getUniqueNumber(), "RMW_TIME", this.rmwTime);
      METRICS_REPORTER.report(
          METRICS_REPORTER.getUniqueNumberGenerator().getUniqueNumber(),
          "RMW_OPS",
          this.rmwCompleted);
      METRICS_REPORTER.report(
          METRICS_REPORTER.getUniqueNumberGenerator().getUniqueNumber(),
          "READ_TIME",
          this.readTime);
      METRICS_REPORTER.report(
          METRICS_REPORTER.getUniqueNumberGenerator().getUniqueNumber(),
          "READ_OPS",
          this.readCompleted);
    }

    System.out.println("Finished with size " + METRICS_REPORTER.getReportedMetrics().size());
  }

  private void op() {
    boolean rmw;
    if (this.rmwPercentage == 0) {
      rmw = false;
    } else if (this.rmwPercentage == 1) {
      rmw = true;
    } else {
      rmw = this.random.nextFloat() <= this.rmwPercentage;
    }

    long start = System.currentTimeMillis();
    if (rmw) {
      this.node.rmw(1);
    } else {
      this.node.read(null);
    }
    long end = System.currentTimeMillis();
    long time = end - start;
    if (rmw) {
      this.rmwTime += time;
      this.rmwCompleted++;
    } else {
      this.readTime += time;
      this.readCompleted++;
    }
  }
}
