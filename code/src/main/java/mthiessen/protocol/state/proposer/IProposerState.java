package mthiessen.protocol.state.proposer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public interface IProposerState {
  AtomicLong getLeadershipStart();

  boolean isLeader();

  AtomicInteger getLatestAssignedIndex();

  int getLatestBroadcastIndex();

  void setLatestBroadcastIndex(final int index);
}
