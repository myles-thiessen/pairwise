package mthiessen.protocol.state.writerecorder;

import mthiessen.protocol.Payloads;

public class NoopWriteRecorder implements IWriteRecorder {
  public static final NoopWriteRecorder INSTANCE = new NoopWriteRecorder();

  @Override
  public void setRequest(int index, Payloads.PrepareRequest request) {}

  @Override
  public Payloads.PrepareRequest waitAndGetRequest(int index) {
    return null;
  }
}
