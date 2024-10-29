package mthiessen.protocol.algorithms;

import mthiessen.IProcess;
import mthiessen.IStateMachine;
import mthiessen.network.IRouter;

public class AlgorithmFactory {

  public static IProcess getAlgorithm(
      final Variant variant,
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine) {
    return getAlgorithm(variant, leader, router, stateMachine,
        LowerBounds.NOOP, 0, 0);
  }

  public static IProcess getAlgorithm(
      final Variant variant,
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final LowerBounds lowerBounds) {
    return getAlgorithm(variant, leader, router, stateMachine, lowerBounds, 0
        , 0);
  }

  public static IProcess getAlgorithm(
      final Variant variant,
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final LowerBounds lowerBounds,
      final int alpha) {
    return getAlgorithm(variant, leader, router, stateMachine, lowerBounds,
        alpha, 0);
  }

  public static IProcess getAlgorithm(
      final Variant variant,
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final LowerBounds lowerBounds,
      final int alpha,
      final int delta) {

    AlgorithmGenerator generator = null;

    switch (variant) {
      case LEADER_READS -> generator = LeaderReadsAlgorithm::generate;
      case PQL -> generator = PQLAlgorithm::generate;
      case CHT -> generator = CHTAlgorithm::generate;
      case BHT ->
          generator = (l, r, s) -> BHTAlgorithm.generate(l, r, s, alpha, delta);
      case US ->
          generator = (l, r, s) -> USAlgorithm.generate(l, r, s, lowerBounds,
              alpha);
      case US2 ->
          generator = (l, r, s) -> US2Algorithm.generate(l, r, s, lowerBounds
              , alpha);
      default -> System.exit(-1);
    }

    return generator.generate(leader, router, stateMachine);
  }

  private interface AlgorithmGenerator {
    IProcess generate(final Object leader, final IRouter router,
                      final IStateMachine stateMachine);
  }
}
