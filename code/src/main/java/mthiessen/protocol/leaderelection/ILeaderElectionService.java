package mthiessen.protocol.leaderelection;

public interface ILeaderElectionService {

  Object leader();

  boolean amLeader(final long startTime, final long endTime);
}
