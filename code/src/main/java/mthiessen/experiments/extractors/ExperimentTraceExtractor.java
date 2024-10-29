package mthiessen.experiments.extractors;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import mthiessen.grpc.ExperimentGrpc;
import mthiessen.grpc.NetworkOuterClass;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.Pair;
import mthiessen.misc.Util;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ExperimentTraceExtractor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentTraceExtractor.class);
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
    getTraces(dir);
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

  private static void getTraces(final String dir) throws IOException {
    List<Map<Pair<Object, Integer>, IOperationMeasurement>> traces = new LinkedList<>();
    for (int i = 0; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      NetworkOuterClass.GetTraceRequest request =
          NetworkOuterClass.GetTraceRequest.newBuilder().build();
      NetworkOuterClass.GetTraceResponse response = stub.getTrace(request);
      Map<Pair<Object, Integer>, IOperationMeasurement> traceMap =
          SerializationUtils.deserialize(response.getTrace().toByteArray());
      traces.add(traceMap);
      printTrace("../traces/" + dir + "/" + i + ".txt", traceMap);
    }
    //    printWrite("../traces/" + dir + "/rmwReq.txt", traces);
  }

  private static void printTrace(
      final String filename, final Map<Pair<Object, Integer>, IOperationMeasurement> traceMap)
      throws IOException {
    System.out.println(filename);
    File file = new File(filename);
    file.createNewFile();
    try (FileWriter writer = new FileWriter(file)) {
      for (Map.Entry<Pair<Object, Integer>, IOperationMeasurement> entry : traceMap.entrySet()) {
        Object identifier = entry.getKey().k1();
        int opNumber = entry.getKey().k2();
        IOperationMeasurement operationMeasurement = entry.getValue();
        Map<String, Long> stages = operationMeasurement.getStageTimes();

        for (Map.Entry<String, Long> stageEntry : stages.entrySet()) {

          String opType = operationMeasurement.isWrite() ? "WRITE" : "READ";
          String result =
              String.format(
                  "%s %s %d %s %d",
                  opType, identifier, opNumber, stageEntry.getKey(), stageEntry.getValue());
          System.out.println(result);
          writer.write(result + "\n");
        }
      }
    }
    System.out.println();
  }

  private static void shutdown() {
    CHANNELS.forEach(ManagedChannel::shutdownNow);
  }
}
