package mthiessen.network;

public abstract class CountBasedBlockingResponseHandler extends BlockingResponseHandler {

  private final int acceptanceThreshold;
  private int positiveResponses = 0;

  public CountBasedBlockingResponseHandler(
      final int numberOfExpectedResponses, final int acceptanceThreshold) {
    super(numberOfExpectedResponses);
    this.acceptanceThreshold = acceptanceThreshold;
  }

  @Override
  protected Status process(final Object sender, final Object response) {

    this.handle(sender, response);

    this.positiveResponses++;

    if (this.acceptanceThreshold <= this.positiveResponses) {
      return Status.SUCCESS;
    }

    return Status.INCONCLUSIVE;
  }

  protected abstract void handle(final Object sender, final Object o);
}
