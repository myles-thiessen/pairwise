package mthiessen.misc;

import mthiessen.IUniqueNumberGenerator;

import java.util.concurrent.atomic.AtomicInteger;

public class UniqueNumberGenerator implements IUniqueNumberGenerator {

  private final AtomicInteger lastIdentifier = new AtomicInteger(-1);

  @Override
  public int getUniqueNumber() {
    return lastIdentifier.incrementAndGet();
  }
}
