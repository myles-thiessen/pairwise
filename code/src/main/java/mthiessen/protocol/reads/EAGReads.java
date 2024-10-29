package mthiessen.protocol.reads;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.IStateMachine;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.protocol.state.process.IProcessState;
import mthiessen.protocol.state.proposer.IProposerState;

@RequiredArgsConstructor
public class EAGReads extends ReadSynchronizer implements IReadOp {

  @NonNull private final IProposerState proposerState;

  @NonNull private final IProcessState processState;

  @NonNull private final IStateMachine stateMachine;

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
      // If not leader, choose index of last voted for (accepted).
      read = this.processState.getLatestVotedForIndex();
    }

    return read;
  }
}
