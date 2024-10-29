package mthiessen.instrumentation;

import mthiessen.misc.Pair;

import java.util.Map;

public interface IMeasurementCollector {
  IOperationMeasurement registerNewOperation(final boolean write);

  IOperationMeasurement getExistingOperation(final Pair<Object, Integer> operationIdentifier);

  IOperationMeasurement getOrRegisterExistingOperation(
      final Pair<Object, Integer> operationIdentifier, final boolean write);

  Map<Pair<Object, Integer>, IOperationMeasurement> getOperationMeasurements();

  void setInstrumentation(final boolean active);
}
