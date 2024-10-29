package mthiessen.protocol.algorithms;

import mthiessen.IProcess;
import mthiessen.IStateMachine;
import mthiessen.network.IRouter;
import mthiessen.protocol.eventschedulingprimitive.LowerBounds;
import mthiessen.protocol.reads.conflictdetection.IConflictDetection;

public class AlgorithmFactory {

  public static IProcess getAlgorithm(
      final Algorithm algorithm,
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final IConflictDetection conflictDetection) {
    return getAlgorithm(
        algorithm, leader, router, stateMachine, conflictDetection, LowerBounds.NOOP, 0, 0);
  }

  public static IProcess getAlgorithm(
      final Algorithm algorithm,
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final IConflictDetection conflictDetection,
      final LowerBounds lowerBounds) {
    return getAlgorithm(
        algorithm, leader, router, stateMachine, conflictDetection, lowerBounds, 0, 0);
  }

  public static IProcess getAlgorithm(
      final Algorithm algorithm,
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final IConflictDetection conflictDetection,
      final LowerBounds lowerBounds,
      final int alpha) {
    return getAlgorithm(
        algorithm, leader, router, stateMachine, conflictDetection, lowerBounds, alpha, 0);
  }

  public static IProcess getAlgorithm(
      final Algorithm algorithm,
      final Object leader,
      final IRouter router,
      final IStateMachine stateMachine,
      final IConflictDetection conflictDetection,
      final LowerBounds lowerBounds,
      final int alpha,
      final int delta) {

    AlgorithmGenerator generator = null;

    switch (algorithm) {
      case LR -> generator = LR::generate;
      case INV -> generator = INV::generate;
      case EAG -> generator = EAG::generate;
      case DEL -> generator = (l, r, s, c) -> DEL.generate(l, r, s, c, alpha, delta);
      case PL -> generator = (l, r, s, c) -> PL.generate(l, r, s, c, lowerBounds, alpha);
      case PA -> generator = (l, r, s, c) -> PA.generate(l, r, s, c, lowerBounds, alpha);
      case PLFFT -> generator = (l, r, s, c) -> PLFFT.generate(l, r, s, c, lowerBounds, alpha);
      case PAFFT -> generator = (l, r, s, c) -> PAFFT.generate(l, r, s, c, lowerBounds, alpha);
      default -> System.exit(-1);
    }

    return generator.generate(leader, router, stateMachine, conflictDetection);
  }

  private interface AlgorithmGenerator {
    IProcess generate(
        final Object leader,
        final IRouter router,
        final IStateMachine stateMachine,
        final IConflictDetection conflictDetection);
  }
}
