package mthiessen.protocol.state.writerecorder;

import mthiessen.protocol.Payloads;

public class NoopWriteRecorder implements IWriteRecorder {
  public static final NoopWriteRecorder INSTANCE = new NoopWriteRecorder();

  @Override
  public void setRequest(int index, Payloads.WriteRequest request) {
  }

  @Override
  public Payloads.WriteRequest waitAndGetRequest(int index) {
    return null;
  }
}
