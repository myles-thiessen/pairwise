package mthiessen.network;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
public abstract class BlockingResponseHandler implements IResponseHandler {

  private final ReentrantLock lock = new ReentrantLock();

  private final Condition waitForCondition = this.lock.newCondition();

  private final int numberOfExpectedResponses;

  private int numberOfResponsesReceived = 0;

  private BroadcastResponseStatus result = null;

  @Override
  public void execute(final Object sender, final Object response) {
    this.lock.lock();

    this.numberOfResponsesReceived++;

    Status status = this.process(sender, response);

    if (status == Status.SUCCESS) {
      this.result = BroadcastResponseStatus.SUCCESS;
    } else if (status == Status.FAILURE) {
      this.result = BroadcastResponseStatus.FAILED;
    }

    if (status != Status.INCONCLUSIVE
        || this.numberOfResponsesReceived == this.numberOfExpectedResponses) {
      this.waitForCondition.signal();
    }

    this.lock.unlock();
  }

  protected abstract Status process(final Object sender, final Object o);

  protected abstract Object responseState();

  @Override
  public BroadcastResponse waitForResponse() {
    this.lock.lock();

    if (this.result == null) {
      try {
        this.waitForCondition.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    BroadcastResponseStatus responseStatus = this.result;

    this.lock.unlock();

    return new BroadcastResponse(responseStatus, this.responseState());
  }

  public enum Status {
    SUCCESS,
    FAILURE,
    INCONCLUSIVE
  }
}
