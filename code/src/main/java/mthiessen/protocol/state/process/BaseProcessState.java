package mthiessen.protocol.state.process;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Getter
public abstract class BaseProcessState implements IProcessState {
  protected final ReadWriteLock lock = new ReentrantReadWriteLock();

  @Setter protected int latestVotedForIndex = 0;
  protected int latestCommittedIndex = 0;

  private final Map<Integer, Object> rmwReqs = new HashMap<>();

  private final Map<Integer, Phaser> responseWaiting = new ConcurrentHashMap<>();
  private final Map<Integer, Object> responses = new ConcurrentHashMap<>();

  @Override
  public void recordRMW(final int index, final Object rmwReq) {
    this.rmwReqs.put(index, rmwReq);
  }

  @Override
  public Object getRMW(final int index) {
    return this.rmwReqs.get(index);
  }

  @Override
  public void removeRMW(final int index) {
    this.rmwReqs.remove(index);
  }

  @Override
  public void setResponse(final int localIdentifier, final Object response) {
    this.responses.put(localIdentifier, response);

    if (this.responseWaiting.containsKey(localIdentifier)) {
      Phaser phaser = this.responseWaiting.get(localIdentifier);

      this.responseWaiting.remove(localIdentifier);

      phaser.arrive();
    }
  }

  @Override
  public Object getResponse(final int localIdentifier) {
    this.lock.readLock().lock();

    if (this.responses.containsKey(localIdentifier)) {
      this.lock.readLock().unlock();
    } else {
      Phaser phaser = this.responseWaiting.computeIfAbsent(localIdentifier, (k) -> new Phaser(1));

      phaser.register();
      phaser.arrive();

      this.lock.readLock().unlock();

      phaser.awaitAdvance(0);
    }

    Object response = this.responses.get(localIdentifier);

    this.responses.remove(localIdentifier);

    return response;
  }
}
