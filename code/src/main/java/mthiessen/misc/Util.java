package mthiessen.misc;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Util {

  public static final ExecutorService CACHED_THREAD_POOL = Executors.newCachedThreadPool();
  public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE =
      Executors.newSingleThreadScheduledExecutor();

  public static void delegateToCachedThreadPool(
      final Runnable runnable, final long duration, final TimeUnit timeUnit) {
    SCHEDULED_EXECUTOR_SERVICE.schedule(
        () -> CACHED_THREAD_POOL.submit(runnable), duration, timeUnit);
  }

  public static int majority(final int n) {
    return n / 2 + 1;
  }

  public static Double average(final List<Long> history) {
    return history.stream().mapToLong(i -> i).average().orElse(0D);
  }

  public static Long max(final List<Long> history) {
    return history.stream().mapToLong(i -> i).max().orElse(0L);
  }

  public static Long min(final List<Long> history) {
    return history.stream().mapToLong(i -> i).min().orElse(0L);
  }

  public static Node split(final String target) {
    int firstSplit = target.indexOf(':');
    int secondSplit = target.indexOf(':', firstSplit + 1);
    int thirdSplit = target.indexOf(':', secondSplit + 1);
    String ip = target.substring(0, firstSplit);
    int experimentPlanePort = Integer.parseInt(target.substring(firstSplit + 1, secondSplit));
    int dataPlanePort = Integer.parseInt(target.substring(secondSplit + 1, thirdSplit));
    String identifier = target.substring(thirdSplit + 1);
    return new Node(ip, experimentPlanePort, dataPlanePort, identifier);
  }

  public record Node(String ip, int experimentPlanePort, int dataPlanePort, String identifier) {}
}
