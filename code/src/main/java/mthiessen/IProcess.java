package mthiessen;

import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.Pair;

import java.util.Map;

public interface IProcess {
  void initialize();

  void write(final Object write);

  Object read(final Object readReq);

  void setInstrumentation(final boolean active);

  Map<Pair<Object, Integer>, IOperationMeasurement> getTrace();
}
