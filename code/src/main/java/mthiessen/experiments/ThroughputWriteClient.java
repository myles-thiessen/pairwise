package mthiessen.experiments;

import lombok.RequiredArgsConstructor;
import mthiessen.IProcess;
import mthiessen.misc.Util;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ThroughputWriteClient {
  private final IProcess node;
  private final int delay;
  private Future<?> future;

  public void start() {
    this.future =
        Util.SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(
            this::schedule, 0, this.delay, TimeUnit.MILLISECONDS);
  }

  private void schedule() {
    Util.CACHED_THREAD_POOL.submit(() -> this.node.write(1));
  }

  public void stop() {
    if (this.future != null) this.future.cancel(false);
  }
}
