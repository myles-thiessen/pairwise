package mthiessen.experiments.clients;

import lombok.RequiredArgsConstructor;
import mthiessen.IProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class WarmupClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(WarmupClient.class);
  private final boolean write;
  private final int totalNumberOfOps;
  private final IProcess node;

  public void start() {
    if (this.write) {
      for (int i = 0; i < this.totalNumberOfOps; i++) {
        this.node.rmw(1);
      }
    }
    for (int i = 0; i < this.totalNumberOfOps; i++) {
      this.node.read(null);
    }
    LOGGER.info("Executed {} ops to warmup JVM", this.totalNumberOfOps);
  }
}
