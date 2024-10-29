package mthiessen;

public interface IStateMachine {
  Object rmw(final Object rmwReq);

  Object read(final Object readReq);
}
