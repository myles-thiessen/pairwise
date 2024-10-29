package mthiessen.experiments.metrics;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class MetricsFactory {
  @Getter private static final Map<Class<?>, MetricsReporter> METRICS_MAP = new HashMap<>();

  public static MetricsReporter getMetricsReporter(final Class<?> className) {
    MetricsReporter metricsReporter = new MetricsReporter();
    METRICS_MAP.put(className, metricsReporter);
    return metricsReporter;
  }

  public static void clear() {
    METRICS_MAP.values().forEach(MetricsReporter::clear);
  }
}
