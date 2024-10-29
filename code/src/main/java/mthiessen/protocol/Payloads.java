package mthiessen.protocol;

import mthiessen.misc.Pair;

import java.io.Serializable;
import java.util.Set;

public class Payloads {

  public record GloballyUniqueRMW(Object processID, int rmwID, Object rmwReq)
      implements Serializable {}

  // Prepare
  public record PrepareRequest(int index, GloballyUniqueRMW globallyUniqueRMW, Object payload)
      implements Serializable {}

  public record PrepareResponse(Object response) implements Serializable {}

  // US Payload

  public record PLPayload(Pair<Long, Integer> stop, Pair<Long, Integer> start)
      implements Serializable {}

  public record PAPayload(Set<Object> leaseHolders, Pair<Long, Integer> stop)
      implements Serializable {}

  // PA Stop Time

  public record StopTimeRequest(int index, Pair<Long, Integer> stopped) implements Serializable {}

  // Markers

  public record MarkerRequest(int version, long time) implements Serializable {}

  // Leases

  public record LeaseGrant(
      Set<Object> leaseHolders, int minimumIndex, Pair<Long, Integer> expiration)
      implements Serializable {}

  public record LeaseRequest() implements Serializable {}
}
