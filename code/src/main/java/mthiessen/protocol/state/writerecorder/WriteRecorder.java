package mthiessen.protocol.state.writerecorder;

import mthiessen.protocol.Payloads;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WriteRecorder implements IWriteRecorder {
  private final Lock lock = new ReentrantLock();
  private final Map<Integer, Phaser> waitingToRead = new ConcurrentHashMap<>();
  private final Map<Integer, Payloads.PrepareRequest> requests = new ConcurrentHashMap<>();

  @Override
  public void setRequest(final int index, final Payloads.PrepareRequest request) {
    this.lock.lock();

    this.requests.put(index, request);

    if (this.waitingToRead.containsKey(index)) {
      Phaser phaser = this.waitingToRead.get(index);

      phaser.arrive();
    }

    this.lock.unlock();
  }

  @Override
  public Payloads.PrepareRequest waitAndGetRequest(final int index) {
    this.lock.lock();

    if (!this.requests.containsKey(index)) {

      Phaser phaser = this.waitingToRead.computeIfAbsent(index, (k) -> new Phaser(1));

      phaser.register();
      phaser.arrive();

      this.lock.unlock();

      phaser.awaitAdvance(0);
    } else {
      this.lock.unlock();
    }

    Payloads.PrepareRequest request = this.requests.get(index);

    this.requests.remove(index);

    return request;
  }
}
