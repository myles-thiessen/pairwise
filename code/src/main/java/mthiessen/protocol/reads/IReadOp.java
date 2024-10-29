package mthiessen.protocol.reads;

import mthiessen.instrumentation.IOperationMeasurement;

public interface IReadOp {
  Object read(final Object readReq, final IOperationMeasurement operationMeasurement);
}
