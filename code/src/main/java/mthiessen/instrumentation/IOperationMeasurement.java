package mthiessen.instrumentation;

import mthiessen.misc.Pair;

import java.util.Map;

public interface IOperationMeasurement {
  Pair<Object, Integer> getOperationIdentifier();

  boolean isWrite();

  void record(final Event event, final String suffix);

  default void record(final Event event) {
    record(event, "");
  }

  Map<String, Long> getStageTimes();
}
