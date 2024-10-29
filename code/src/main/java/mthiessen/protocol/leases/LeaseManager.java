package mthiessen.protocol.leases;

import lombok.RequiredArgsConstructor;
import mthiessen.protocol.Payloads;
import mthiessen.protocol.eventschedulingprimitive.EventSchedulingPrimitive;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

@RequiredArgsConstructor
public class LeaseManager {
  private final EventSchedulingPrimitive eventSchedulingPrimitive;

  private final Set<Lease> leases = ConcurrentHashMap.newKeySet();

  private final Phaser phaser = new Phaser(1);

  public void addLease(final Object sender, final Payloads.LeaseGrant leaseGrantRequest) {
    long expirationTime =
        this.eventSchedulingPrimitive.event(sender, leaseGrantRequest.expiration());

    Lease lease = new Lease(leaseGrantRequest.minimumIndex(), expirationTime);

    this.leases.add(lease);

    this.phaser.arrive();
  }

  public int checkLease(final long readTime) {
    int best = Integer.MAX_VALUE;

    Set<Lease> toRemove = new HashSet<>();

    Set<Lease> leasesCopy = Set.copyOf(this.leases);

    for (Lease lease : leasesCopy) {
      if (readTime <= lease.expiration && lease.minimumIndex <= best) {
        best = lease.minimumIndex;
      }

      if (readTime > lease.expiration) {
        toRemove.add(lease);
      }
    }

    for (Lease lease : toRemove) {
      this.leases.remove(lease);
    }

    if (best == Integer.MAX_VALUE) {
      // Wait until next lease is added before checking again
      int phase = this.phaser.getPhase();
      this.phaser.awaitAdvance(phase);
      return checkLease(readTime);
    } else {
      return best;
    }
  }

  public record Lease(int minimumIndex, long expiration) {}
}
