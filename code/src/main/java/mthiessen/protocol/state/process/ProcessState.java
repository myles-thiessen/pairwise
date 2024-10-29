package mthiessen.protocol.state.process;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

public class ProcessState extends BaseProcessState {
  private final Map<Integer, Phaser> waitingToRead = new ConcurrentHashMap<>();
  private final Map<Integer, Phaser> waitingToApply = new ConcurrentHashMap<>();

  // Committer holds rmwReq lock entire time.
  @Override
  public void safeToApply(final int index) {

    int priorIndex = index - 1;

    if (this.waitingToApply.containsKey(priorIndex)) {
      Phaser phaser = this.waitingToApply.get(priorIndex);

      phaser.awaitAdvance(0);

      this.waitingToApply.remove(priorIndex);
    }
  }

  // Committer holds rmwReq lock entire time.
  @Override
  public void setLatestCommittedIndex(final int index) {
    assert super.latestCommittedIndex == index - 1;

    super.latestCommittedIndex = index;

    if (this.waitingToRead.containsKey(index)) {
      Phaser phaser = this.waitingToRead.get(index);

      this.waitingToRead.remove(index);

      this.waitingToApply.put(index, new Phaser(phaser.getRegisteredParties() - 1));

      // This triggers all read threads to start reading.
      phaser.arrive();
    }
  }

  // Caller holds read lock.
  @Override
  public void waitFor(final int index) {
    Phaser phaser = this.waitingToRead.computeIfAbsent(index, (k) -> new Phaser(1));

    phaser.register();
    phaser.arrive();

    super.lock.readLock().unlock();

    phaser.awaitAdvance(0);
  }

  @Override
  public void doneReading(final int index) {
    this.waitingToApply.get(index).arrive();
  }
}
