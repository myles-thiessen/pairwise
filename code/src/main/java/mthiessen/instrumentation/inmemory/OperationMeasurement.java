package mthiessen.instrumentation.inmemory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.Pair;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public class OperationMeasurement implements IOperationMeasurement, Serializable {

  private final Pair<Object, Integer> operationIdentifier;

  private final boolean write;

  private final Map<String, Long> stageTimes = new ConcurrentHashMap<>();

  @Override
  public void record(final Event event, final String suffix) {
    this.stageTimes.put(event.toString() + suffix, System.currentTimeMillis());
  }
}
