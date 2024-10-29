package mthiessen.protocol.state.proposer;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class ProposerState implements IProposerState {
  private final AtomicLong leadershipStart = new AtomicLong(0);
  private final AtomicInteger latestAssignedIndex = new AtomicInteger(0);
  @Setter private volatile int latestBroadcastIndex = 0;

  public boolean isLeader() {
    return this.leadershipStart.get() != 0;
  }
}
