package mthiessen.protocol.reads;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.protocol.state.process.FutureWriteProcessState;

@RequiredArgsConstructor
public class FutureWriteReads extends ReadSynchronizer implements IReadOp {

  @NonNull private final FutureWriteProcessState processState;

  @NonNull private final IStateMachine stateMachine;

  @Override
  public Object read(final Object readReq, final IOperationMeasurement operationMeasurement) {
    return super.read(this.processState, this.stateMachine, readReq, operationMeasurement);
  }

  @Override
  protected int stampRead(final Object readReq) {

    int read;

    if (this.processState.getLatestVotedForIndex() == this.processState.getLatestCommittedIndex()) {
      read = this.processState.getLatestVotedForIndex();
    } else {

      long t = System.currentTimeMillis();

      read = this.processState.getLatestCommittedIndex();

      for (int i = read + 1; i <= this.processState.getLatestVotedForIndex(); i++) {

        if (this.processState.getPendingWrites().containsKey(i)) {
          long promise = this.processState.getPendingWrites().get(i);

          if (promise <= t) read = i;
        }
      }
    }

    return read;
  }
}
