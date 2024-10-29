package mthiessen;

public interface IStateMachine {
  void write(final Object write);

  Object read(final Object request);
}
