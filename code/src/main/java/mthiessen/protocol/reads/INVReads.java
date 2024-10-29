package mthiessen.protocol.reads;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.protocol.state.process.IProcessState;
import mthiessen.protocol.state.proposer.IProposerState;

@RequiredArgsConstructor
public class INVReads implements IReadOp {

  @NonNull private final IProposerState proposerState;

  @NonNull private final IProcessState processState;

  @NonNull private final IStateMachine stateMachine;

  @Override
  public Object read(final Object readReq, final IOperationMeasurement operationMeasurement) {

    this.processState.getLock().readLock().lock();

    boolean notify = false;

    // pre-wait
    operationMeasurement.record(Event.PRE_READ_WAIT);

    if (!this.proposerState.isLeader()) {
      if (this.processState.getLatestVotedForIndex()
          != this.processState.getLatestCommittedIndex()) {
        // some rmwReq is outstanding

        notify = true;

        this.processState.waitFor(0);
      }
    }

    // post-wait
    operationMeasurement.record(Event.POST_READ_WAIT);

    Object value = this.stateMachine.read(readReq);

    // post read
    operationMeasurement.record(Event.POST_READ);

    if (notify) {
      this.processState.doneReading(0);
    } else {
      this.processState.getLock().readLock().unlock();
    }

    // op complete
    operationMeasurement.record(Event.READ_COMPLETE);

    return value;
  }
}
