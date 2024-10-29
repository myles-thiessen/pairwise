package mthiessen.protocol.state.process;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Getter
public class BaseProcessState {
  protected final ReadWriteLock lock = new ReentrantReadWriteLock();
  @Setter
  protected int latestVotedForIndex = 0;
  protected int latestCommittedIndex = 0;
}
