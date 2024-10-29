package mthiessen.protocol.replication;

import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.protocol.Payloads;

public interface IReplicationOp {
  void replicate(
      final Payloads.GloballyUniqueRMW globallyUniqueRMW,
      final IOperationMeasurement operationMeasurement);
}
