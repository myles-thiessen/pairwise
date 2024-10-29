package mthiessen.protocol.leaderelection;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.protocol.state.proposer.IProposerState;

@RequiredArgsConstructor
public class LeaderElectionOp implements Runnable {

  @NonNull private final ILeaderElectionService leaderElectionService;

  @NonNull private final IProposerState proposerState;

  @Override
  public void run() {
    // Processes believes they are not the leader
    if (this.proposerState.getLeadershipStart().get() == 0) {
      long time = System.currentTimeMillis();

      if (this.leaderElectionService.amLeader(time, time)) {
        // Process is the leader
        boolean success = this.proposerState.getLeadershipStart().compareAndSet(0, time);

        if (!success) {
          run();
        }
      }
    }
  }
}
