package mthiessen.protocol.state.process;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class FutureWriteProcessState extends ProcessState {
  private final Map<Integer, Long> pendingWrites = new HashMap<>();
}
