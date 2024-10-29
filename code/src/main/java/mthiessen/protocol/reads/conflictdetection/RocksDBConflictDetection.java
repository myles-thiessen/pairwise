package mthiessen.protocol.reads.conflictdetection;

import mthiessen.statemachines.RocksDBStateMachine;

import java.util.Objects;

public class RocksDBConflictDetection implements IConflictDetection {
  @Override
  public boolean doOpsConflict(final Object readReq, final Object rmwReq) {
    // by default readReq and ith rmwReq conflict.
    boolean conflict = true;

    // readRead and ith rmwReq do not conflict if either they are for
    // different tables or different keys.
    if (readReq instanceof RocksDBStateMachine.ReadRequest rocksDbReadOp
        && rmwReq instanceof RocksDBStateMachine.WriteRequest rocksDbWriteOp) {
      conflict =
          Objects.equals(rocksDbReadOp.table(), rocksDbWriteOp.table())
              && Objects.equals(rocksDbReadOp.key(), rocksDbWriteOp.key());
    }

    return conflict;
  }
}
