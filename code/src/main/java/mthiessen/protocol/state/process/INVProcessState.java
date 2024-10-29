package mthiessen.protocol.state.process;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

public class INVProcessState extends BaseProcessState {
  private final Map<Integer, Phaser> waitingToRead = new ConcurrentHashMap<>();
  private final Map<Integer, Phaser> waitingToApply = new ConcurrentHashMap<>();

  // Committer holds rmwReq lock entire time.
  @Override
  public void safeToApply(final int index) {

    // safe to apply so long as no read is concurrently going on.
    // Sense all reads take locks,

    if (this.waitingToApply.containsKey(0)) {
      Phaser phaser = this.waitingToApply.get(0);

      phaser.awaitAdvance(0);

      this.waitingToApply.remove(0);
    }
  }

  // Committer holds rmwReq lock entire time.
  @Override
  public void setLatestCommittedIndex(final int index) {
    assert super.latestCommittedIndex == index - 1;

    super.latestCommittedIndex = index;

    if (this.waitingToRead.containsKey(0)) {
      // Implies some process is waiting to read.

      if (index == super.latestVotedForIndex) {
        // This rmwReq is the last outstanding rmwReq.

        Phaser phaser = this.waitingToRead.get(0);

        this.waitingToRead.remove(0);

        this.waitingToApply.put(0, new Phaser(phaser.getRegisteredParties() - 1));

        phaser.arrive();
      }
    }
  }

  // Caller holds read lock.
  @Override
  public void waitFor(final int index) {
    Phaser phaser = this.waitingToRead.computeIfAbsent(0, (k) -> new Phaser(1));

    phaser.register();
    phaser.arrive();

    super.lock.readLock().unlock();

    phaser.awaitAdvance(0);
  }

  @Override
  public void doneReading(final int index) {
    Phaser phaser = this.waitingToApply.get(0);

    phaser.arrive();
  }
}
