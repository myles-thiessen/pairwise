package mthiessen.protocol.state.process;

import java.util.concurrent.locks.ReadWriteLock;

public interface IProcessState {
  ReadWriteLock getLock();

  int getLatestCommittedIndex();

  // It is assumed that the write lock is held before invoking.
  void setLatestCommittedIndex(final int index);

  int getLatestVotedForIndex();

  // It is assumed that the write lock is held before invoking.
  void setLatestVotedForIndex(final int index);

  // It is assumed that the write lock is held before invoking.
  void safeToApply(final int index);

  // It is assumed that the read lock is held before invoking. Furthermore,
  // the read lock will be released before this function returns.
  void waitFor(final int index);

  void doneReading(final int index);

  void leaderWait(final int index);
}
