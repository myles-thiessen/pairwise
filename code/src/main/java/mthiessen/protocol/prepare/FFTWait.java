package mthiessen.protocol.prepare;

import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class FFTWait {
  private static final ScheduledExecutorService EXECUTOR =
      Executors.newSingleThreadScheduledExecutor();

  private final Set<Object> acceptors = ConcurrentHashMap.newKeySet();
  private final Set<Long> stopTimes = ConcurrentHashMap.newKeySet();
  private final Phaser phaser = new Phaser(1);
  private Set<Object> toWaitFor = null;

  public void record(final Object acceptor, final long time) {
    this.stopTimes.add(time);
    this.acceptors.add(acceptor);

    if (this.toWaitFor != null) {
      Set<Object> dif =
          this.toWaitFor.stream()
              .filter(a -> !this.acceptors.contains(a))
              .collect(Collectors.toSet());

      if (dif.isEmpty()) {
        this.phaser.arrive();
      }
    }
  }

  public void waitOrTimeout(final Set<Object> leaseHolders, final long timeout) {

    Set<Object> dif =
        leaseHolders.stream().filter(a -> !this.acceptors.contains(a)).collect(Collectors.toSet());

    if (dif.isEmpty()) return;

    this.toWaitFor = leaseHolders;

    long diff = timeout - System.currentTimeMillis();

    EXECUTOR.schedule(
        () -> {
          this.phaser.arrive();
        },
        diff,
        TimeUnit.MILLISECONDS);

    this.phaser.awaitAdvance(0);
  }

  public Set<Object> getAcceptors() {
    return Set.copyOf(this.acceptors);
  }

  public Set<Long> getStopTimes() {
    return Set.copyOf(this.stopTimes);
  }
}
