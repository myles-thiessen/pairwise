package mthiessen.protocol.state.writerecorder;

import mthiessen.protocol.Payloads;

public interface IWriteRecorder {
  void setRequest(final int index, final Payloads.PrepareRequest request);

  Payloads.PrepareRequest waitAndGetRequest(final int index);
}
