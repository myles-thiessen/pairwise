package mthiessen.protocol.leaderelection;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.network.IRouter;

@RequiredArgsConstructor
public class StaticLeaderElectionService implements ILeaderElectionService {

  @NonNull private final Object leader;

  @NonNull private final IRouter router;

  @Override
  public Object leader() {
    return this.leader;
  }

  @Override
  public boolean amLeader(final long startTime, final long endTime) {
    return this.leader.equals(this.router.getRouterState().getIdentifier());
  }
}
