package mthiessen.statemachines;

import mthiessen.IStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegerStateMachine implements IStateMachine {

  private static final Logger LOGGER = LoggerFactory.getLogger(IntegerStateMachine.class);

  private Integer state = null;

  @Override
  public Object rmw(final Object rmwReq) {
    if (!(rmwReq instanceof Integer value)) {
      LOGGER.info("Response was not of expected type {}", Integer.class);
      return -1;
    }

    this.state = value;

    return value;
  }

  @Override
  public Object read(final Object readReq) {
    return this.state;
  }
}
