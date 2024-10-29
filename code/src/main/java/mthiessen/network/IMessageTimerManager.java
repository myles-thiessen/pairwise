package mthiessen.network;

import java.util.List;

public interface IMessageTimerManager {
  void recordMessageTime(final Object process, final Object operation, final long time);

  List<Long> getMessagesTimes(final Object process, final Object operation);
}
