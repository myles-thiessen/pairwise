package mthiessen.experiments;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import mthiessen.IProcess;
import mthiessen.IStateMachine;
import mthiessen.experiments.clients.*;
import mthiessen.experiments.metrics.MetricsFactory;
import mthiessen.experiments.metrics.MetricsReporter;
import mthiessen.grpc.ExperimentGrpc;
import mthiessen.grpc.NetworkOuterClass;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.Pair;
import mthiessen.network.IRouter;
import mthiessen.network.grpc.GRPCServer;
import mthiessen.protocol.algorithms.Algorithm;
import mthiessen.protocol.algorithms.AlgorithmFactory;
import mthiessen.protocol.algorithms.DEL;
import mthiessen.protocol.eventschedulingprimitive.LowerBounds;
import mthiessen.protocol.reads.conflictdetection.IConflictDetection;
import mthiessen.protocol.reads.conflictdetection.RocksDBConflictDetection;
import mthiessen.statemachines.IntegerStateMachine;
import mthiessen.statemachines.RocksDBStateMachine;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class ExperimentService extends ExperimentGrpc.ExperimentImplBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentService.class);

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

    IStateMachine stateMachine;
    switch (request.getStateMachineType()) {
      case NOOP -> stateMachine = new IntegerStateMachine();
      case ROCKS -> stateMachine = new RocksDBStateMachine("pairwise-rocks");
      default -> throw new RuntimeException("Bad state machine type");
    }

    IConflictDetection conflictDetection;
    if (protocol.endsWith("RocksDBCD")) {
      conflictDetection = new RocksDBConflictDetection();
    } else {
      conflictDetection = IConflictDetection.NOOP;
    }

    Function<IRouter, IProcess> factory;
    Algorithm algorithm;
    if (protocol.startsWith("EAG")) {
      algorithm = Algorithm.EAG;
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  algorithm, request.getLeader(), router, stateMachine, conflictDetection);
    } else if (protocol.startsWith("DEL")) {
      algorithm = Algorithm.DEL;
      String[] s = protocol.split("-");
      int alpha = Integer.parseInt(s[1]);
      int delta = Integer.parseInt(s[2]);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  algorithm,
                  request.getLeader(),
                  router,
                  stateMachine,
                  conflictDetection,
                  lowerBounds,
                  alpha,
                  delta);
    } else if (protocol.startsWith("PAFFT")) {
      algorithm = Algorithm.PAFFT;
      String[] s = protocol.split("-");
      int alpha = Integer.parseInt(s[1]);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  algorithm,
                  request.getLeader(),
                  router,
                  stateMachine,
                  conflictDetection,
                  lowerBounds,
                  alpha);
    } else if (protocol.startsWith("PA")) {
      algorithm = Algorithm.PA;
      String[] s = protocol.split("-");
      int alpha = Integer.parseInt(s[1]);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  algorithm,
                  request.getLeader(),
                  router,
                  stateMachine,
                  conflictDetection,
                  lowerBounds,
                  alpha);
    } else if (protocol.startsWith("PLFFT")) {
      algorithm = Algorithm.PLFFT;
      String[] s = protocol.split("-");
      int alpha = Integer.parseInt(s[1]);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  algorithm,
                  request.getLeader(),
                  router,
                  stateMachine,
                  conflictDetection,
                  lowerBounds,
                  alpha);
    } else if (protocol.startsWith("PL")) {
      algorithm = Algorithm.PL;
      String[] s = protocol.split("-");
      int alpha = Integer.parseInt(s[1]);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  algorithm,
                  request.getLeader(),
                  router,
                  stateMachine,
                  conflictDetection,
                  lowerBounds,
                  alpha);
    } else {
      algorithm = Algorithm.valueOf(protocol);
      factory =
          (router) ->
              AlgorithmFactory.getAlgorithm(
                  algorithm, request.getLeader(), router, stateMachine, conflictDetection);
    }

    this.grpcServiceProcess = new GRPCServiceProcess(request.getIdentifier(), factory);
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
        new WarmupClient(request.getWrite(), request.getTotalNumberOfOps(), process);
    client.start();
    //    LOGGER.info("Workload finished waiting for completion");
    //    process.waitForWorkloadCompletion(request.getTotalNumberOfOps());
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

    Map<Class<?>, MetricsReporter> metricsReporterMap = MetricsFactory.getMETRICS_MAP();

    NetworkOuterClass.GetMetricsResponse.Builder getMetricsResponseBuilder =
        NetworkOuterClass.GetMetricsResponse.newBuilder();

    for (Map.Entry<Class<?>, MetricsReporter> entry : metricsReporterMap.entrySet()) {
      Class<?> c = entry.getKey();
      MetricsReporter reporter = entry.getValue();

      System.out.println("Compiling for class " + c);

      ByteString classBytes = ByteString.copyFrom(SerializationUtils.serialize(c));

      NetworkOuterClass.MetricReporter.Builder metricReporterBuilder =
          NetworkOuterClass.MetricReporter.newBuilder();
      metricReporterBuilder.setC(classBytes);

      Map<Integer, MetricsReporter.Metric> metricMap = reporter.getReportedMetrics();

      System.out.println("Building map " + metricMap.size());

      for (Map.Entry<Integer, MetricsReporter.Metric> metricEntry : metricMap.entrySet()) {
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
    Map<Pair<Object, Integer>, IOperationMeasurement> trace = this.grpcServiceProcess.getTrace();
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
              ("sudo tc qdisc add dev ens5 root handle 1: prio bands 3 priomap 2"
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
                        "sudo tc filter add dev ens5 protocol ip parent 1:0 prio "
                            + "%d u32 match ip dst %s/32 flowid 1:%d",
                        count, entry.getKey(), count)
                    .split(" "))
            .waitFor();
        LOGGER.info(String.format("Set latency for %s to %f", entry.getKey(), entry.getValue()));
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
  public void updateLatency(
      NetworkOuterClass.SetLatencyRequest request,
      StreamObserver<NetworkOuterClass.SetLatencyResponse> responseObserver) {
    Map<String, Float> map = request.getLatencyMap();

    Runtime r = Runtime.getRuntime();

    try {
      int count = 1;
      for (Map.Entry<String, Float> entry : map.entrySet()) {
        r.exec(
                String.format(
                        "sudo tc qdisc replace dev ens5 parent 1:%d handle %d:"
                            + " netem delay %fms",
                        count, count * 10, entry.getValue())
                    .split(" "))
            .waitFor();
        LOGGER.info(
            String.format("Updated latency for %s to %f", entry.getKey(), entry.getValue()));
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
    this.throughputWriteClient = new ThroughputWriteClient(process, request.getDelay());
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

  @Override
  public void startBackToBackWorkload(
      final NetworkOuterClass.StartBackToBackWorkloadRequest request,
      final StreamObserver<NetworkOuterClass.StartBackToBackWorkloadResponse> responseObserver) {
    IProcess process = this.grpcServiceProcess.getServiceProcess().getProcess();
    BackToBackMixedClient client =
        new BackToBackMixedClient(
            process, request.getWritePercentage(), request.getSeconds(), request.getMeasure());

    client.start();

    responseObserver.onNext(NetworkOuterClass.StartBackToBackWorkloadResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void insert(
      final NetworkOuterClass.InsertRequest request,
      final StreamObserver<NetworkOuterClass.InsertResponse> responseObserver) {
    IProcess process = this.grpcServiceProcess.getServiceProcess().getProcess();

    RocksDBStateMachine.WriteRequest writeRequest =
        new RocksDBStateMachine.WriteRequest(
            request.getTable(), request.getKey(), request.getValue().toByteArray());

    process.rmw(writeRequest);

    responseObserver.onNext(NetworkOuterClass.InsertResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void read(
      final NetworkOuterClass.ReadRequest request,
      final StreamObserver<NetworkOuterClass.ReadResponse> responseObserver) {
    IProcess process = this.grpcServiceProcess.getServiceProcess().getProcess();

    RocksDBStateMachine.ReadRequest readRequest =
        new RocksDBStateMachine.ReadRequest(request.getTable(), request.getKey());

    Object response = process.read(readRequest);

    NetworkOuterClass.ReadResponse.Builder builder = NetworkOuterClass.ReadResponse.newBuilder();

    if (response != null && !(response instanceof RocksDBStateMachine.Status)) {
      ByteString responseBytes = ByteString.copyFrom((byte[]) response);
      builder.setValue(responseBytes);
    }

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void updateDelta(
      final NetworkOuterClass.UpdateDeltaRequest request,
      final StreamObserver<NetworkOuterClass.UpdateDeltaResponse> responseObserver) {

    IProcess process = this.grpcServiceProcess.getServiceProcess().getProcess();

    if (process instanceof DEL delProcess) {
      delProcess.getCommitRequestHandler().setDelta(request.getDelta());
    }

    responseObserver.onNext(NetworkOuterClass.UpdateDeltaResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
