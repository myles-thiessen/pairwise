package mthiessen.protocol.leases;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;
import mthiessen.protocol.state.proposer.IProposerState;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LeaseHolderManager {
  // seconds
  private static final double DURATION = 2;

  private final ReentrantLock revokeLock = new ReentrantLock();

  private final LinkedList<Pair<Long, Set<Object>>> onGoingRevocations = new LinkedList<>();

  @NonNull private final IProposerState proposerState;

  @NonNull private final EventSchedulingPrimitive eventSchedulingPrimitive;

  @NonNull private final IRouter router;

  private final Set<Object> leaseHolders = ConcurrentHashMap.newKeySet();

  private long currentExpirationTime = 0;

  public void grantLeases() {
    Set<Object> currentLeaseHolders = Set.copyOf(this.leaseHolders);

    int minimumIndex = this.proposerState.getLatestAssignedIndex().get();

    this.currentExpirationTime = System.currentTimeMillis() + (long) (DURATION * 1000);

    Function<Object, Payloads.LeaseGrant> messageFunction =
        follower -> {
          Pair<Long, Integer> expiration =
              this.eventSchedulingPrimitive.before(follower, this.currentExpirationTime);

          return new Payloads.LeaseGrant(currentLeaseHolders, minimumIndex, expiration);
        };

    this.router.broadcastPersonalizedRequestToAll(OPS.LEASE_GRANT, messageFunction::apply, true);
  }

  public void revokeLeases(final Set<Object> toRemove) {
    this.revokeLock.lock();

    boolean remove = false;

    long time = -1;
    Set<Object> pending = new HashSet<>();

    for (Pair<Long, Set<Object>> onGoingRevocation : this.onGoingRevocations) {
      pending.addAll(onGoingRevocation.k2());
      if (pending.containsAll(toRemove)) {
        time = onGoingRevocation.k1();
        break;
      }
    }

    Set<Object> toActuallyRemove = null;

    if (time == -1) {
      remove = true;

      time = this.currentExpirationTime;

      toActuallyRemove =
          toRemove.stream()
              .filter(leaseHolder -> !pending.contains(leaseHolder))
              .collect(Collectors.toSet());

      this.onGoingRevocations.addLast(new Pair<>(time, toActuallyRemove));

      this.leaseHolders.removeAll(toActuallyRemove);
    }

    long diff = time - System.currentTimeMillis();

    this.revokeLock.unlock();

    try {
      Thread.sleep(diff);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    this.revokeLock.lock();

    if (remove) {
      this.onGoingRevocations.remove(new Pair<>(time, toActuallyRemove));
    }

    this.revokeLock.unlock();
  }

  public void addLeaseHolder(final Object newLeaseHolder) {
    this.leaseHolders.add(newLeaseHolder);
  }

  public Set<Object> getLeaseHolders() {
    return Set.copyOf(this.leaseHolders);
  }
}
