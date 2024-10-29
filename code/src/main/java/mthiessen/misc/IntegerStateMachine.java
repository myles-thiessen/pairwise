package mthiessen.misc;

import mthiessen.IStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegerStateMachine implements IStateMachine {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(IntegerStateMachine.class);

  private Integer state = null;

  @Override
  public void write(final Object write) {
    if (!(write instanceof Integer value)) {
      LOGGER.info("Response was not of expected type {}", Integer.class);
      return;
    }

    this.state = value;
  }

  @Override
  public Object read(final Object request) {
    return this.state;
  }
}
