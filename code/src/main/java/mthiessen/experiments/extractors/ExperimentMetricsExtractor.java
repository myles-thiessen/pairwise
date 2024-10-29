package mthiessen.experiments.extractors;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import mthiessen.experiments.clients.BackToBackMixedClient;
import mthiessen.experiments.clients.SimulatedClient;
import mthiessen.experiments.clients.ThroughputReadClient;
import mthiessen.experiments.metrics.MetricsReporter;
import mthiessen.grpc.ExperimentGrpc;
import mthiessen.grpc.NetworkOuterClass;
import mthiessen.misc.Util;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ExperimentMetricsExtractor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentMetricsExtractor.class);
  private static final List<Util.Node> NODES = new LinkedList<>();
  private static final List<ManagedChannel> CHANNELS = new LinkedList<>();
  private static final List<ExperimentGrpc.ExperimentBlockingStub> STUBS = new LinkedList<>();
  private static int N;
  private static List<String> TARGETS;

  public static void main(final String[] args) throws IOException {
    String dir = args[0];
    N = args.length - 1;
    TARGETS = List.of(args).subList(1, args.length);
    initializeConnections();
    getMetrics(dir);
    shutdown();
  }

  private static void initializeConnections() {
    for (String string : TARGETS) {
      Util.Node node = Util.split(string);
      NODES.add(node);
      String format = String.format("%s:%d", node.ip(), node.experimentPlanePort());
      LOGGER.info("Establishing connection to {}", format);
      ManagedChannel channel =
          Grpc.newChannelBuilder(format, InsecureChannelCredentials.create()).build();
      ExperimentGrpc.ExperimentBlockingStub stub = ExperimentGrpc.newBlockingStub(channel);
      CHANNELS.add(channel);
      STUBS.add(stub);
    }
  }

  private static void getMetrics(final String dir) throws IOException {
    for (int i = 0; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      NetworkOuterClass.GetMetricsRequest request =
          NetworkOuterClass.GetMetricsRequest.newBuilder().build();

      System.out.println("Getting Metrics");

      NetworkOuterClass.GetMetricsResponse response = stub.getMetrics(request);

      System.out.println("Got results");

      Map<Class<?>, MetricsReporter> metricsReporterMap = new HashMap<>();

      for (NetworkOuterClass.MetricReporter metricReporter : response.getMetricreportersList()) {

        Class<?> c = SerializationUtils.deserialize(metricReporter.getC().toByteArray());

        MetricsReporter reporter = getMetricsReporter(metricReporter);

        metricsReporterMap.put(c, reporter);
      }

      printMetrics("../results/" + dir + "/" + i + ".txt", metricsReporterMap);
    }
  }

  private static MetricsReporter getMetricsReporter(
      final NetworkOuterClass.MetricReporter metricReporter) {
    Map<Integer, NetworkOuterClass.ReportedMetric> reportedMetricMap =
        metricReporter.getReportedmetricsMap();

    MetricsReporter reporter = new MetricsReporter();

    for (Map.Entry<Integer, NetworkOuterClass.ReportedMetric> entry :
        reportedMetricMap.entrySet()) {
      Integer key = entry.getKey();
      NetworkOuterClass.ReportedMetric reportedMetric = entry.getValue();

      reporter.report(key, reportedMetric.getMarker(), reportedMetric.getMetric());
    }
    return reporter;
  }

  private static void printMetrics(
      final String filename, final Map<Class<?>, MetricsReporter> metricsReporterMap)
      throws IOException {
    System.out.println(filename);
    File file = new File(filename);
    file.createNewFile();
    FileWriter writer = new FileWriter(file);

    printMetricsFromReport(metricsReporterMap.get(SimulatedClient.class), writer);
    printMetricsFromReport(metricsReporterMap.get(ThroughputReadClient.class), writer);
    printMetricsFromReport(metricsReporterMap.get(BackToBackMixedClient.class), writer);

    System.out.println();
  }

  private static void printMetricsFromReport(
      final MetricsReporter reporter, final FileWriter writer) throws IOException {
    if (reporter == null) return;
    Map<Integer, MetricsReporter.Metric> metrics = reporter.getReportedMetrics();
    if (metrics.isEmpty()) return;
    System.out.println(reporter.getClass() + " " + metrics.size());
    for (MetricsReporter.Metric metric : metrics.values()) {
      String result = String.format("%s %f", metric.marker(), metric.metric());
      writer.write(result + "\n");
      writer.flush();
    }
  }

  private static void shutdown() {
    CHANNELS.forEach(ManagedChannel::shutdownNow);
  }
}
