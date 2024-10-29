package mthiessen.protocol.reads;

import mthiessen.IStateMachine;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.protocol.state.process.IProcessState;

public abstract class ReadSynchronizer {

  protected abstract int stampRead(final Object readReq);

  protected Object read(
      final IProcessState processState,
      final IStateMachine stateMachine,
      final Object readReq,
      final IOperationMeasurement operationMeasurement) {
    operationMeasurement.record(Event.PRE_LOCK_ACQUIRE);

    processState.getLock().readLock().lock();

    operationMeasurement.record(Event.POST_LOCK_ACQUIRE);

    // Need to get this index while holding the lock to ensure that a read is
    // not stamped with an index which is smaller than the committed index.
    int read = this.stampRead(readReq);

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
}
