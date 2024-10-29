package mthiessen.protocol;

import mthiessen.misc.Pair;

import java.io.Serializable;

public class Payloads {
  // Write

  public record WriteRequest(int index, Object write,
                             Object payload) implements Serializable {
  }

  public record WriteResponse(boolean decision) implements Serializable {
  }

  // US Payload

  public record USPayload(Pair<Long, Integer> stop, Pair<Long, Integer> start)
      implements Serializable {
  }

  public record US2Payload(Pair<Long, Integer> stop) implements Serializable {
  }

  // US2 Stop Time

  public record StopTimeRequest(int index,
                                Pair<Long, Integer> stopped) implements Serializable {
  }

  // Markers

  public record MarkerRequest(int version, long time) implements Serializable {
  }
}
