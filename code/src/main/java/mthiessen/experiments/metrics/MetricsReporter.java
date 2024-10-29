package mthiessen.experiments.metrics;

import lombok.Getter;
import mthiessen.IUniqueNumberGenerator;
import mthiessen.misc.UniqueNumberGenerator;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Getter
public class MetricsReporter implements Serializable {
  private final Map<Integer, Metric> reportedMetrics =
      Collections.synchronizedSortedMap(new TreeMap<>());

  private final transient IUniqueNumberGenerator uniqueNumberGenerator =
      new UniqueNumberGenerator();

  public void report(final int index, final String marker, final double metric) {
    this.reportedMetrics.put(index, new Metric(marker, metric));
  }

  public void clear() {
    this.reportedMetrics.clear();
  }

  public record Metric(String marker, double metric) implements Serializable {}
}
