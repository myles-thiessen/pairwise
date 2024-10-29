package mthiessen.protocol.state.writerecorder;

import mthiessen.protocol.Payloads;

public interface IWriteRecorder {
  void setRequest(final int index, final Payloads.WriteRequest request);

  Payloads.WriteRequest waitAndGetRequest(final int index);
}
