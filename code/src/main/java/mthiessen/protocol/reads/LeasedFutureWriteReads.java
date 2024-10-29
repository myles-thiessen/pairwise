package mthiessen.protocol.reads;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.protocol.leases.LeaseManager;
import mthiessen.protocol.state.process.FutureWriteProcessState;
import mthiessen.protocol.state.process.IProcessState;

@RequiredArgsConstructor
public class LeasedFutureWriteReads implements IReadOp {
  @NonNull private final LeaseManager leaseManager;

  @NonNull private final FutureWriteProcessState processState;

  @NonNull private final IStateMachine stateMachine;

  @Override
  public Object read(final Object readReq, final IOperationMeasurement operationMeasurement) {
    return this.read(this.processState, this.stateMachine, readReq, operationMeasurement);
  }

  private Object read(
      final IProcessState processState,
      final IStateMachine stateMachine,
      final Object readReq,
      final IOperationMeasurement operationMeasurement) {
    operationMeasurement.record(Event.PRE_LOCK_ACQUIRE);

    long t = System.currentTimeMillis();

    int minimum = this.leaseManager.checkLease(t);

    processState.getLock().readLock().lock();

    operationMeasurement.record(Event.POST_LOCK_ACQUIRE);

    // Need to get this index while holding the lock to ensure that a read is
    // not stamped with an index which is smaller than the committed index.
    int read = this.stampRead(t, minimum);

    int currentCommitted = processState.getLatestCommittedIndex();

    assert currentCommitted <= read;

    // record pre-wait
    operationMeasurement.record(Event.PRE_READ_WAIT);

    boolean notify = false;

    if (currentCommitted < read) {
      notify = true;

      processState.waitFor(read);
    }

    // record post-wait
    operationMeasurement.record(Event.POST_READ_WAIT);

    Object value = stateMachine.read(readReq);

    // record post-read
    operationMeasurement.record(Event.POST_READ);

    if (notify) {
      processState.doneReading(read);
    } else {
      // Need to hold lock the entire time (even while reading) to ensure
      // that a commit operation doesn't take place before the read is done.
      processState.getLock().readLock().unlock();
    }

    // record completion
    operationMeasurement.record(Event.READ_COMPLETE);

    return value;
  }

  protected int stampRead(final long t, final int minimum) {

    int read = Math.max(this.processState.getLatestCommittedIndex(), minimum);

    for (int i = read + 1; i <= this.processState.getLatestVotedForIndex(); i++) {

      if (this.processState.getPendingWrites().containsKey(i)) {
        long promise = this.processState.getPendingWrites().get(i);

        if (promise <= t) read = i;
      }
    }

    return read;
  }
}
