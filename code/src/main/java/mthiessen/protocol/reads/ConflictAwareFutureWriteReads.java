package mthiessen.protocol.reads;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.protocol.reads.conflictdetection.IConflictDetection;
import mthiessen.protocol.state.process.FutureWriteProcessState;

/** Small modification of {@link FutureWriteReads} for RocksDB implementation. */
@RequiredArgsConstructor
public class ConflictAwareFutureWriteReads extends ReadSynchronizer implements IReadOp {

  @NonNull private final FutureWriteProcessState processState;

  @NonNull private final IStateMachine stateMachine;

  @NonNull private final IConflictDetection conflictDetection;

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

        // do not need to observe i if we haven't received prepare for it.
        long promise = t + 1;

        if (this.processState.getPendingWrites().containsKey(i)) {
          promise = this.processState.getPendingWrites().get(i);
        }

        Object rmwReq = this.processState.getRMW(i);

        // do not need to observe i if we haven't received prepare for it.
        boolean conflict = false;

        if (rmwReq != null) {
          conflict = this.conflictDetection.doOpsConflict(readReq, rmwReq);
        }

        // Must observe i if both the promise has passed and readReq
        // conflicts with rmwReq
        if (promise <= t && conflict) read = i;
      }
    }

    return read;
  }
}
