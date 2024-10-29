package mthiessen;

import java.util.Random;

public class Test {
  public static void main(String[] args) {
    int delay = 100;
    Random random = new Random();
    float coinFlip = random.nextFloat();
    int randomDelay = (int) ((coinFlip * delay) + delay);
    System.out.println(randomDelay);
  }
}
