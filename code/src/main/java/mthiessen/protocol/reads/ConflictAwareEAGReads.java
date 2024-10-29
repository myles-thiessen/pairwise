package mthiessen.protocol.reads;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.protocol.reads.conflictdetection.IConflictDetection;
import mthiessen.protocol.state.process.IProcessState;
import mthiessen.protocol.state.proposer.IProposerState;

@RequiredArgsConstructor
public class ConflictAwareEAGReads extends ReadSynchronizer implements IReadOp {

  @NonNull private final IProposerState proposerState;

  @NonNull private final IProcessState processState;

  @NonNull private final IStateMachine stateMachine;

  @NonNull private final IConflictDetection conflictDetection;

  @Override
  public Object read(final Object readReq, final IOperationMeasurement operationMeasurement) {
    return super.read(this.processState, this.stateMachine, readReq, operationMeasurement);
  }

  @Override
  protected int stampRead(final Object readReq) {
    int read;

    if (this.proposerState.isLeader()) {
      // If leader, check the latest broadcast index (the index of the last
      // index broadcast for commit).
      read = this.proposerState.getLatestBroadcastIndex();
    } else {
      read = this.processState.getLatestCommittedIndex();

      for (int i = read + 1; i <= this.processState.getLatestVotedForIndex(); i++) {
        Object rmwReq = this.processState.getRMW(i);

        // do not need to observe i if we haven't received prepare for it.
        boolean conflict = false;

        if (rmwReq != null) {
          conflict = this.conflictDetection.doOpsConflict(readReq, rmwReq);
        }

        if (conflict) read = i;
      }
    }

    return read;
  }
}
