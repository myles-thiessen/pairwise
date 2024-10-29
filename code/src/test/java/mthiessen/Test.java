package mthiessen;

import java.util.concurrent.Phaser;

public class Test {

  public static void main(String[] args) {
    Phaser phaser = new Phaser(1);

    System.out.println(phaser.getPhase());

    System.out.println(phaser.getUnarrivedParties());

    phaser.register();

    phaser.arrive();

    System.out.println(phaser.getUnarrivedParties());

    System.out.println(phaser.getPhase());

    phaser.arrive();

    System.out.println(phaser.getUnarrivedParties());
  }
}
