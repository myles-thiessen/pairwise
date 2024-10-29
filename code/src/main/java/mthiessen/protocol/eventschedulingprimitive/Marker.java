package mthiessen.protocol.eventschedulingprimitive;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Marker {
  private final int version;
  private final long time;
  private final long roundTrip;
}
