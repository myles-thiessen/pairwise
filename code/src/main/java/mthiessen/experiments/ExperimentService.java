package mthiessen.experiments;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import mthiessen.IProcess;
import mthiessen.experiments.metrics.MetricsFactory;
import mthiessen.experiments.metrics.MetricsReporter;
import mthiessen.grpc.ExperimentGrpc;
import mthiessen.grpc.NetworkOuterClass;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.IntegerStateMachine;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.network.grpc.GRPCServer;
import mthiessen.protocol.algorithms.AlgorithmFactory;
import mthiessen.protocol.algorithms.LowerBounds;
import mthiessen.protocol.algorithms.Variant;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class ExperimentService extends ExperimentGrpc.ExperimentImplBase {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExperimentService.class);

  private GRPCServer server = null;
  private GRPCServiceProcess grpcServiceProcess = null;

  private ThroughputWriteClient throughputWriteClient = null;

  @Override
  public void reset(
      final NetworkOuterClass.ResetRequest request,
      final StreamObserver<NetworkOuterClass.ResetResponse> responseObserver) {
    if (this.server != null) {
      this.server.shutdown();
    }
    if (this.grpcServiceProcess != null) {
      this.grpcServiceProcess.shutdown();
    }
    MetricsFactory.clear();
    responseObserver.onNext(NetworkOuterClass.ResetResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void startServiceNode(
      final NetworkOuterClass.StartServiceNodeRequest request,
      final StreamObserver<NetworkOuterClass.StartServiceNodeResponse> responseObserver) {
    try {
      this.server = new GRPCServer(request.getDataPlanePort());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    String protocol = request.getProtocol();
    LowerBounds lowerBounds = new LowerBounds(request.getLbs());
    Function<IRouter, IProcess> factory;
    Variant variant;
    if (protocol.startsWith("BHT")) {
      variant = Variant.BHT;
      String[] s = protocol.split("-");
      int alpha = Integer.parseInt(s[1]);
      int delta = Integer.parseInt(s[2]);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  variant,
                  request.getLeader(),
                  router,
                  new IntegerStateMachine(),
                  lowerBounds,
                  alpha,
                  delta);
    } else if (protocol.startsWith("US2")) {
      variant = Variant.US2;
      String[] s = protocol.split("-");
      int alpha = Integer.parseInt(s[1]);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  variant,
                  request.getLeader(),
                  router,
                  new IntegerStateMachine(),
                  lowerBounds,
                  alpha);
    } else if (protocol.startsWith("US")) {
      variant = Variant.US;
      String[] s = protocol.split("-");
      int alpha = Integer.parseInt(s[1]);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  variant,
                  request.getLeader(),
                  router,
                  new IntegerStateMachine(),
                  lowerBounds,
                  alpha);
    } else {
      variant = Variant.valueOf(protocol);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  variant, request.getLeader(), router,
                  new IntegerStateMachine());
    }

    this.grpcServiceProcess = new GRPCServiceProcess(request.getIdentifier(),
        factory);
    this.server
        .getNetworkService()
        .setRouter(this.grpcServiceProcess.getServiceProcess().getRouter());
    responseObserver.onNext(NetworkOuterClass.StartServiceNodeResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void initializeRouters(
      final NetworkOuterClass.InitializeRoutersRequest request,
      final StreamObserver<NetworkOuterClass.InitializeRoutersResponse> responseObserver) {
    this.grpcServiceProcess.initializeRouters(request.getTargetsList().toArray(new String[0]));
    responseObserver.onNext(NetworkOuterClass.InitializeRoutersResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void initializeServiceNode(
      final NetworkOuterClass.InitializeServiceNodeRequest request,
      final StreamObserver<NetworkOuterClass.InitializeServiceNodeResponse> responseObserver) {
    this.grpcServiceProcess.initializeServiceNode();
    responseObserver.onNext(NetworkOuterClass.InitializeServiceNodeResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void setInstrumentation(
      final NetworkOuterClass.SetInstrumentationRequest request,
      final StreamObserver<NetworkOuterClass.SetInstrumentationResponse> responseObserver) {

    this.grpcServiceProcess.setInitialize(request.getActive());

    responseObserver.onNext(NetworkOuterClass.SetInstrumentationResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void warmup(
      final NetworkOuterClass.WarmupRequest request,
      final StreamObserver<NetworkOuterClass.WarmupResponse> responseObserver) {
    IProcess process = this.grpcServiceProcess.getServiceProcess().getProcess();
    WarmupClient client =
        new WarmupClient(request.getWrite(), request.getTotalNumberOfOps(),
            process);
    client.start();
    LOGGER.info("Workload completed");
    responseObserver.onNext(NetworkOuterClass.WarmupResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void startWorkload(
      final NetworkOuterClass.StartWorkloadRequest request,
      final StreamObserver<NetworkOuterClass.StartWorkloadResponse> responseObserver) {
    IProcess process = this.grpcServiceProcess.getServiceProcess().getProcess();
    SimulatedClient client =
        new SimulatedClient(
            request.getDelay(),
            request.getNumberOfOps(),
            process,
            request.getDiscard(),
            request.getWritePercentage());
    long wait = request.getStart() - System.currentTimeMillis();
    if (wait > 0) {
      try {
        Thread.sleep(wait);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    client.start();
    responseObserver.onNext(NetworkOuterClass.StartWorkloadResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void getMetrics(
      final NetworkOuterClass.GetMetricsRequest request,
      final StreamObserver<NetworkOuterClass.GetMetricsResponse> responseObserver) {
    System.out.println("Fetching metrics");

    Map<Class<?>, MetricsReporter> metricsReporterMap =
        MetricsFactory.getMETRICS_MAP();

    NetworkOuterClass.GetMetricsResponse.Builder getMetricsResponseBuilder =
        NetworkOuterClass.GetMetricsResponse.newBuilder();

    for (Map.Entry<Class<?>, MetricsReporter> entry :
        metricsReporterMap.entrySet()) {
      Class<?> c = entry.getKey();
      MetricsReporter reporter = entry.getValue();

      System.out.println("Compiling for class " + c);

      ByteString classBytes =
          ByteString.copyFrom(SerializationUtils.serialize(c));

      NetworkOuterClass.MetricReporter.Builder metricReporterBuilder =
          NetworkOuterClass.MetricReporter.newBuilder();
      metricReporterBuilder.setC(classBytes);

      Map<Integer, MetricsReporter.Metric> metricMap =
          reporter.getReportedMetrics();

      System.out.println("Building map " + metricMap.size());

      for (Map.Entry<Integer, MetricsReporter.Metric> metricEntry :
          metricMap.entrySet()) {
        int index = metricEntry.getKey();
        MetricsReporter.Metric metric = metricEntry.getValue();

        if (metric == null) continue;

        NetworkOuterClass.ReportedMetric reportedMetric =
            NetworkOuterClass.ReportedMetric.newBuilder()
                .setMarker(metric.marker())
                .setMetric(metric.metric())
                .build();

        metricReporterBuilder.putReportedmetrics(index, reportedMetric);
      }

      System.out.println("Done");

      getMetricsResponseBuilder.addMetricreporters(metricReporterBuilder.build());
    }

    responseObserver.onNext(getMetricsResponseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void getTrace(
      final NetworkOuterClass.GetTraceRequest request,
      final StreamObserver<NetworkOuterClass.GetTraceResponse> responseObserver) {
    Map<Pair<Object, Integer>, IOperationMeasurement> trace =
        this.grpcServiceProcess.getTrace();
    NetworkOuterClass.GetTraceResponse response =
        NetworkOuterClass.GetTraceResponse.newBuilder()
            .setTrace(ByteString.copyFrom(SerializationUtils.serialize((Serializable) trace)))
            .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void setLatency(
      final NetworkOuterClass.SetLatencyRequest request,
      final StreamObserver<NetworkOuterClass.SetLatencyResponse> responseObserver) {

    Map<String, Float> map = request.getLatencyMap();

    Runtime r = Runtime.getRuntime();

    try {
      r.exec("sudo tc qdisc del dev ens5 root".split(" ")).waitFor();
      r.exec(
              ("sudo tc qdisc add dev ens5 root handle 1: prio bands 3 " +
                  "priomap 2"
                  + " 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2")
                  .split(" "))
          .waitFor();

      int count = 1;
      for (Map.Entry<String, Float> entry : map.entrySet()) {
        r.exec(
                String.format(
                        "sudo tc qdisc add dev ens5 parent 1:%d handle %d: " + "netem delay %fms",
                        count, count * 10, entry.getValue())
                    .split(" "))
            .waitFor();
        r.exec(
                String.format(
                        "sudo tc filter add dev ens5 protocol ip parent 1:0 " +
                            "prio "
                            + "%d u32 match ip dst %s/32 flowid 1:%d",
                        count, entry.getKey(), count)
                    .split(" "))
            .waitFor();
        LOGGER.info(String.format("Set latency for %s to %f", entry.getKey(),
            entry.getValue()));
        count++;
      }

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    NetworkOuterClass.SetLatencyResponse response =
        NetworkOuterClass.SetLatencyResponse.newBuilder().build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void startThroughputReadWorkload(
      final NetworkOuterClass.StartThroughputReadWorkloadRequest request,
      final StreamObserver<NetworkOuterClass.StartThroughputReadWorkloadResponse>
          responseObserver) {
    IProcess process = this.grpcServiceProcess.getServiceProcess().getProcess();
    ThroughputReadClient client =
        new ThroughputReadClient(
            request.getNumberOfWindows(),
            process,
            request.getWindow(),
            request.getMeasure(),
            request.getDiscard());
    long wait = request.getStart() - System.currentTimeMillis();
    if (wait > 0) {
      try {
        Thread.sleep(wait);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    client.start();
    responseObserver.onNext(
        NetworkOuterClass.StartThroughputReadWorkloadResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void startThroughputWriteWorkload(
      final NetworkOuterClass.StartThroughputWriteWorkloadRequest request,
      final StreamObserver<NetworkOuterClass.StartThroughputWriteWorkloadResponse>
          responseObserver) {
    IProcess process = this.grpcServiceProcess.getServiceProcess().getProcess();
    this.throughputWriteClient = new ThroughputWriteClient(process,
        request.getDelay());
    this.throughputWriteClient.start();

    responseObserver.onNext(
        NetworkOuterClass.StartThroughputWriteWorkloadResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void stopThroughputWriteWorkload(
      final NetworkOuterClass.StopThroughputWriteWorkloadRequest request,
      final StreamObserver<NetworkOuterClass.StopThroughputWriteWorkloadResponse>
          responseObserver) {
    assert this.throughputWriteClient != null;

    this.throughputWriteClient.stop();
    this.throughputWriteClient = null;

    responseObserver.onNext(
        NetworkOuterClass.StopThroughputWriteWorkloadResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
