package mthiessen.instrumentation.inmemory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mthiessen.IUniqueNumberGenerator;
import mthiessen.instrumentation.IMeasurementCollector;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.instrumentation.noop.NoopOperationMeasurement;
import mthiessen.misc.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
// One instance per process. Collects measurements for each operation.
public class MeasurementCollector implements IMeasurementCollector {
  private final Lock lock = new ReentrantLock();
  private final Object identifier;
  private final IUniqueNumberGenerator uniqueNumberGenerator;

  // Getter reads will occur without holding a lock. This is fine as we only
  // fetch this list after all operations have completed.
  @Getter
  private final Map<Pair<Object, Integer>, IOperationMeasurement> operationMeasurements =
      new HashMap<>();

  private boolean active = false;

  @Override
  public IOperationMeasurement registerNewOperation(final boolean write) {
    if (!this.active) {
      return NoopOperationMeasurement.INSTANCE;
    }

    this.lock.lock();

    Pair<Object, Integer> operationIdentifier =
        new Pair<>(this.identifier, this.uniqueNumberGenerator.getUniqueNumber());

    OperationMeasurement operationMeasurement =
        new OperationMeasurement(operationIdentifier, write);
    this.operationMeasurements.put(operationIdentifier, operationMeasurement);

    this.lock.unlock();

    return operationMeasurement;
  }

  @Override
  public IOperationMeasurement getExistingOperation(
      final Pair<Object, Integer> operationIdentifier) {

    if (!this.active) {
      return NoopOperationMeasurement.INSTANCE;
    }

    this.lock.lock();

    IOperationMeasurement operationMeasurement =
        this.operationMeasurements.get(operationIdentifier);

    this.lock.unlock();

    return operationMeasurement == null ? NoopOperationMeasurement.INSTANCE : operationMeasurement;
  }

  @Override
  public IOperationMeasurement getOrRegisterExistingOperation(
      final Pair<Object, Integer> operationIdentifier, final boolean write) {

    if (!this.active) {
      return NoopOperationMeasurement.INSTANCE;
    }

    IOperationMeasurement operationMeasurement;

    this.lock.lock();

    if (this.operationMeasurements.containsKey(operationIdentifier)) {
      operationMeasurement = this.operationMeasurements.get(operationIdentifier);
    } else {
      operationMeasurement = new OperationMeasurement(operationIdentifier, write);
      this.operationMeasurements.put(operationIdentifier, operationMeasurement);
    }

    this.lock.unlock();

    return operationMeasurement;
  }

  @Override
  public void setInstrumentation(final boolean active) {
    this.active = active;
  }
}
