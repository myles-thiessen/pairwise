package mthiessen.protocol.reads.conflictdetection;

public class NoopConflictDetection implements IConflictDetection {
  @Override
  public boolean doOpsConflict(final Object readReq, final Object rmwReq) {
    return true;
  }
}
