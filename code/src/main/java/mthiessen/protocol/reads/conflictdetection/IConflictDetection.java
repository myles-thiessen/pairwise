package mthiessen.protocol.reads.conflictdetection;

public interface IConflictDetection {

  IConflictDetection NOOP = new NoopConflictDetection();

  boolean doOpsConflict(final Object readReq, final Object rmwReq);
}
