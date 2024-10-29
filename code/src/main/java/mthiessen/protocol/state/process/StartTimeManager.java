package mthiessen.protocol.state.process;

import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class StartTimeManager {
  private final Set<Object> processes;
  private final Map<Integer, StarTime> startTimeMap = new ConcurrentHashMap<>();

  private StarTime getStopTime(final int index) {
    return this.startTimeMap.computeIfAbsent(index, t -> new StarTime());
  }

  public boolean record(final Object sender, final int index,
                        final long startTime) {
    StarTime starTime = this.getStopTime(index);

    starTime.recipients.add(sender);

    if (startTime > starTime.maxTime) starTime.maxTime = startTime;

    return starTime.recipients.equals(this.processes);
  }

  public long getMaxTime(final int index) {

    StarTime starTime = this.getStopTime(index);

    this.startTimeMap.remove(index);

    return starTime.maxTime;
  }

  private static class StarTime {
    private final Set<Object> recipients =
        Collections.newSetFromMap(new ConcurrentHashMap<>());

    private long maxTime = 0;
  }
}
