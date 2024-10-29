package mthiessen.network;

import mthiessen.misc.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageTimerManager implements IMessageTimerManager {
  private final Map<Pair<Object, Object>, List<Long>> messageTimes = new HashMap<>();

  @Override
  public void recordMessageTime(final Object process, final Object operation, final long time) {}

  @Override
  public List<Long> getMessagesTimes(final Object process, final Object operation) {
    Pair<Object, Object> pair = new Pair<>(process, operation);
    return this.messageTimes.get(pair);
  }
}
