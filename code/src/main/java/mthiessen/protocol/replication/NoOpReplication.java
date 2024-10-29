package mthiessen.protocol.replication;

import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.protocol.Payloads;

public class NoOpReplication implements IReplicationOp {
  @Override
  public void replicate(
      final Payloads.GloballyUniqueRMW globallyUniqueRMW,
      final IOperationMeasurement operationMeasurement) {}
}
