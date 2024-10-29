package mthiessen.protocol.replication;

import mthiessen.instrumentation.IOperationMeasurement;

public interface IReplicationOp {
  void replicate(final Object write,
                 final IOperationMeasurement operationMeasurement);
}
