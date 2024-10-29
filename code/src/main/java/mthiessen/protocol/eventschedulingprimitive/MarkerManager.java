package mthiessen.protocol.eventschedulingprimitive;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import mthiessen.network.IRouter;
import mthiessen.protocol.OPS;
import mthiessen.protocol.Payloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class MarkerManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(MarkerManager.class);

  private final AtomicInteger versionCounter = new AtomicInteger(0);

  private final Map<Object, Integer> latestEstablishedVersion = new ConcurrentHashMap<>();

  private final Map<Object, Map<Integer, Marker>> grantedMarker = new ConcurrentHashMap<>();

  private final Map<Object, Map<Integer, Marker>> heldMarker = new ConcurrentHashMap<>();

  @NonNull private final IRouter router;

  private final Phaser phaser = new Phaser(1);
  private final AtomicBoolean initialize = new AtomicBoolean(false);

  public void initialize() {
    if (this.initialize.compareAndSet(false, true)) {
      int broadcastIdentifier = this.establishMarkers();

      this.router.waitForResponse(OPS.SET_MARKER, broadcastIdentifier);

      LOGGER.info("Markers are established");

      this.phaser.arrive();
      this.phaser.forceTermination();
    } else {
      if (!this.phaser.isTerminated()) {
        this.phaser.register();
        this.phaser.arrive();
        this.phaser.awaitAdvance(0);
      }
    }
  }

  public int establishMarkers() {

    int V = this.versionCounter.getAndIncrement();

    Payloads.MarkerRequest markerRequest =
        new Payloads.MarkerRequest(V, System.currentTimeMillis());

    return this.router.broadcastRequestToAll(OPS.SET_MARKER, markerRequest);
  }

  public void setGrantedMarker(final Object holder, final long time, final int version) {
    long t = System.currentTimeMillis();

    Map<Integer, Marker> markerMap =
        this.grantedMarker.computeIfAbsent(holder, (k) -> new ConcurrentHashMap<>());

    Marker m = new Marker(version, t, t - time);

    markerMap.put(version, m);

    this.latestEstablishedVersion.compute(
        holder, (k, v) -> v != null ? Math.max(v, version) : version);
  }

  public Marker getGrantedMarker(final Object holder) {
    Integer latest = this.latestEstablishedVersion.get(holder);

    if (latest == null) return null;

    return this.grantedMarker.get(holder).get(latest);
  }

  public void setHeldMarker(final Object granter, final int version) {
    long t = System.currentTimeMillis();
    Map<Integer, Marker> markerMap =
        this.heldMarker.computeIfAbsent(granter, (k) -> new ConcurrentHashMap<>());
    markerMap.put(version, new Marker(version, t, 0));
  }

  public Marker getHeldMarker(final Object granter, final int version) {
    return this.heldMarker.get(granter).get(version);
  }
}
