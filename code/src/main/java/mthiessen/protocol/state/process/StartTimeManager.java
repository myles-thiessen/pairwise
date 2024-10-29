package mthiessen.protocol.state.process;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class StartTimeManager {
  private final Map<Integer, Set<Object>> whoToWaitForMap = new ConcurrentHashMap<>();
  private final Map<Integer, StarTime> startTimeMap = new ConcurrentHashMap<>();

  public void recordWhoToWaitFor(final int index, final Set<Object> whoToWaitFor) {
    this.whoToWaitForMap.put(index, whoToWaitFor);
  }

  public boolean record(final Object sender, final int index, final long startTime) {
    StarTime starTime = this.startTimeMap.computeIfAbsent(index, t -> new StarTime());

    starTime.recipients.add(sender);

    if (startTime > starTime.maxTime) starTime.maxTime = startTime;

    Set<Object> whoToWaitFor = this.whoToWaitForMap.get(index);

    if (whoToWaitFor == null) {
      return false;
    }

    return starTime.recipients.equals(whoToWaitFor);
  }

  public long getMaxTime(final int index) {

    StarTime starTime = this.startTimeMap.get(index);

    if (starTime == null) {
      return -1;
    }

    return starTime.maxTime;
  }

  public long extractMaxTime(final int index) {

    StarTime starTime = this.startTimeMap.get(index);

    if (starTime == null) {
      return -1;
    }

    this.whoToWaitForMap.remove(index);

    this.startTimeMap.remove(index);

    return starTime.maxTime;
  }

  private static class StarTime {
    private final Set<Object> recipients = ConcurrentHashMap.newKeySet();

    private long maxTime = 0;
  }
}
