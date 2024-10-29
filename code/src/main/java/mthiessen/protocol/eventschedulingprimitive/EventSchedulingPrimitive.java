package mthiessen.protocol.eventschedulingprimitive;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.misc.Pair;

@RequiredArgsConstructor
public class EventSchedulingPrimitive {
  // 200 ppm
  private static final double epsilon = 0.0002;

  @NonNull private final MarkerManager markerManager;

  @NonNull private final LowerBounds lowerBounds;

  @NonNull private final Object me;

  public void initialize() {
    this.markerManager.initialize();
  }

  public Pair<Long, Integer> before(final Object receiver, final long T) {
    Marker marker = this.markerManager.getGrantedMarker(receiver);

    if (marker == null) return null;

    long D =
        receiver.equals(this.me)
            ? T - marker.getTime()
            : (long)
                ((1 - epsilon)
                    * ((T - marker.getTime()) / (1 + epsilon)
                        + this.lowerBounds.getLowerBound(this.me, receiver)));

    int V = marker.getVersion();

    return new Pair<>(D, V);
  }

  public Pair<Long, Integer> at(final Object receiver, final long T) {
    Marker marker = this.markerManager.getGrantedMarker(receiver);

    long D = T - marker.getTime() + marker.getRoundTrip() / 2;

    int V = marker.getVersion();

    return new Pair<>(D, V);
  }

  public Pair<Long, Integer> after(final Object receiver, final long T) {
    long now = System.currentTimeMillis();

    long D =
        receiver.equals(this.me)
            ? T - now
            : (long)
                ((1 + epsilon)
                    * ((T - now) / (1 - epsilon)
                        - this.lowerBounds.getLowerBound(this.me, receiver)));

    int V = -1;

    return new Pair<>(D, V);
  }

  public long event(final Object sender, final Pair<Long, Integer> event) {

    if (event == null) {
      return System.currentTimeMillis() - 1;
    }

    long D = event.k1();
    int V = event.k2();

    if (V >= 0) {
      Marker marker = this.markerManager.getHeldMarker(sender, V);

      return marker.getTime() + D;
    } else { // After event
      return System.currentTimeMillis() + D;
    }
  }
}
