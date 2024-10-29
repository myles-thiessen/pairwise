package mthiessen.network;

public abstract class CountBasedBlockingResponseHandler extends BlockingResponseHandler {

  private final int acceptanceThreshold;
  private final int failureThreshold;
  private int positiveResponses = 0;
  private int negativeResponses = 0;

  public CountBasedBlockingResponseHandler(
      final int numberOfExpectedResponses,
      final int acceptanceThreshold,
      final int failureThreshold) {
    super(numberOfExpectedResponses);
    this.acceptanceThreshold = acceptanceThreshold;
    this.failureThreshold = failureThreshold;
  }

  @Override
  protected Status process(final Object sender, final Object response) {

    if (this.acceptance(response)) {
      this.positiveResponses++;
    } else {
      this.negativeResponses++;
    }

    if (this.acceptanceThreshold <= this.positiveResponses) {
      return Status.SUCCESS;
    } else if (this.failureThreshold < this.negativeResponses) {
      return Status.FAILURE;
    }

    return Status.INCONCLUSIVE;
  }

  protected abstract boolean acceptance(final Object o);
}
