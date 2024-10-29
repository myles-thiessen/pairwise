package mthiessen.instrumentation.noop;

import mthiessen.instrumentation.Event;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.Pair;

import java.util.Map;

public class NoopOperationMeasurement implements IOperationMeasurement {

  public static final NoopOperationMeasurement INSTANCE = new NoopOperationMeasurement();

  private static final Pair<Object, Integer> IDENTIFIER = new Pair<>(0, 0);

  @Override
  public Pair<Object, Integer> getOperationIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public boolean isWrite() {
    return false;
  }

  @Override
  public void record(final Event event, final String suffix) {}

  @Override
  public Map<String, Long> getStageTimes() {
    return null;
  }
}
