package mthiessen.experiments.masters;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import mthiessen.grpc.ExperimentGrpc;
import mthiessen.grpc.NetworkOuterClass;
import mthiessen.misc.Util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

// This experiment is many reads followed by a single rmwReq
public class Experiment1Master {

  private static final Logger LOGGER = LoggerFactory.getLogger(Experiment1Master.class);
  private static final List<Util.Node> NODES = new LinkedList<>();

  private static final List<Session> SSH = new LinkedList<>();
  private static final List<ManagedChannel> CHANNELS = new LinkedList<>();
  private static final List<ExperimentGrpc.ExperimentBlockingStub> STUBS = new LinkedList<>();
  private static String PROTOCOL;
  // percentage of OWD LB's are.
  private static float LBP;

  private static int N;
  private static List<String> TARGETS;

  public static void main(final String[] args)
      throws JSchException, IOException, InterruptedException {
    PROTOCOL = args[0];
    LBP = (float) Integer.parseInt(args[1]) / 40;
    System.out.println("LBP: " + LBP);
    N = args.length - 2;
    TARGETS = List.of(args).subList(2, args.length);

    sshConnections();
    initializeConnections();
    setLatency();
    startServiceNode();
    initializeRouters();
    initializeServiceNode();
    warmup();
    // enableInstrumentation();
    startUniformWorkload();
    shutdown();

    // Shutdown the process as all work is complete.
    System.exit(0);
  }

  private static void sshConnections() throws JSchException, IOException, InterruptedException {
    for (String string : TARGETS) {
      Util.Node node = Util.split(string);
      NODES.add(node);
      JSch jSch = new JSch();

      File privKey = new File("/home/ubuntu/.ssh/myles.pem");

      byte[] file = Files.readAllBytes(privKey.toPath());

      jSch.addIdentity("key", file, null, null);
      System.out.println("ubuntu " + node.ip() + " " + 22);
      Session session = jSch.getSession("ubuntu", node.ip(), 22);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();

      ChannelExec channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand("tmux kill-session -t experiment-slave");
      channel.connect();
      handleSSHCommand(channel);
      channel.disconnect();

      System.out.println("Killed session for " + node.ip());

      channel = (ChannelExec) session.openChannel("exec");

      SSH.add(session);

      channel.setCommand("tmux new-session -s experiment-slave -d ./start-slave.sh");
      channel.connect();
      handleSSHCommand(channel);
      channel.disconnect();

      System.out.println("Spawned session for " + node.ip());
    }
  }

  private static void handleSSHCommand(ChannelExec channel) throws IOException {
    InputStream in = channel.getInputStream();

    byte[] tmp = new byte[1024];
    while (true) {
      while (in.available() > 0) {
        int i = in.read(tmp, 0, 1024);
        if (i < 0) break;
        System.out.print(new String(tmp, 0, i));
      }
      if (channel.isClosed()) {
        if (in.available() > 0) continue;
        System.out.println("exit-status: " + channel.getExitStatus());
        break;
      }
      try {
        Thread.sleep(250);
      } catch (Exception ee) {
      }
    }
  }

  private static void initializeConnections() {
    for (Util.Node node : NODES) {
      System.out.println("Node: " + node);
      String format = String.format("%s:%d", node.ip(), node.experimentPlanePort());
      System.out.println("Establishing connection to " + format);
      connect(format);
    }

    System.out.println("Connected");
  }

  private static void setLatency() {
    String leaderIp = NODES.get(0).ip();
    String follower1Ip = NODES.get(1).ip();
    String follower2Ip = NODES.get(2).ip();

    ExperimentGrpc.ExperimentBlockingStub leaderStub = STUBS.get(0);
    NetworkOuterClass.SetLatencyRequest leaderRequest =
        NetworkOuterClass.SetLatencyRequest.newBuilder()
            .putLatency(follower2Ip, 40)
            .putLatency(follower1Ip, 8)
            .build();
    leaderStub.setLatency(leaderRequest);

    ExperimentGrpc.ExperimentBlockingStub follower1Stub = STUBS.get(1);
    NetworkOuterClass.SetLatencyRequest follower1Request =
        NetworkOuterClass.SetLatencyRequest.newBuilder()
            .putLatency(leaderIp, 8)
            .putLatency(follower2Ip, 32)
            .build();
    follower1Stub.setLatency(follower1Request);

    ExperimentGrpc.ExperimentBlockingStub follower2Stub = STUBS.get(2);
    NetworkOuterClass.SetLatencyRequest follower2Request =
        NetworkOuterClass.SetLatencyRequest.newBuilder()
            .putLatency(leaderIp, 40)
            .putLatency(follower1Ip, 32)
            .build();
    follower2Stub.setLatency(follower2Request);
  }

  private static String buildLB() {
    String leaderId = NODES.get(0).identifier();
    String follower1Id = NODES.get(1).identifier();
    String follower2Id = NODES.get(2).identifier();

    // latency from follower 1 to follower 2 is 50 - OWD

    JSONObject baseObject = new JSONObject();
    JSONObject leaderObject = new JSONObject();
    JSONObject follower1Object = new JSONObject();
    JSONObject follower2Object = new JSONObject();
    baseObject.put(leaderId, leaderObject);
    baseObject.put(follower1Id, follower1Object);
    baseObject.put(follower2Id, follower2Object);

    // No latency, no lower bound.
    leaderObject.put(leaderId, 0);
    // latency from leader to follower 1 is 8
    leaderObject.put(follower1Id, LBP * 8);
    // latency from leader to follower 2 is 40
    leaderObject.put(follower2Id, LBP * 40);

    // No latency, no lower bound.
    follower1Object.put(follower1Id, 0);
    // latency from follower 1 to leader is 8
    follower1Object.put(leaderId, LBP * 8);
    // latency from follower 1 to follower 2 is 32
    follower1Object.put(follower2Id, LBP * 32);

    // No latency, no lower bound.
    follower2Object.put(follower2Id, 0);
    // latency from follower 2 to leader is 40
    follower2Object.put(leaderId, LBP * 40);
    // latency from follower 2 to follower 1 is 32
    follower2Object.put(follower1Id, LBP * 32);

    return baseObject.toString();
  }

  private static void connect(final String format) {
    NetworkOuterClass.ResetRequest request = NetworkOuterClass.ResetRequest.newBuilder().build();
    int retry = 50;

    for (int i = 0; i < retry; i++) {
      try {
        System.out.println("Trying to connect " + i);
        ManagedChannel channel =
            Grpc.newChannelBuilder(format, InsecureChannelCredentials.create())
                .maxInboundMessageSize(Integer.MAX_VALUE)
                .maxInboundMetadataSize(Integer.MAX_VALUE)
                .build();
        ExperimentGrpc.ExperimentBlockingStub stub = ExperimentGrpc.newBlockingStub(channel);
        stub.reset(request);

        CHANNELS.add(channel);
        STUBS.add(stub);
        return;
      } catch (Exception ignored) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private static void startServiceNode() {
    String LBS = buildLB();

    System.out.println(LBS);

    for (int i = 0; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      NetworkOuterClass.StartServiceNodeRequest request =
          NetworkOuterClass.StartServiceNodeRequest.newBuilder()
              .setIdentifier(TARGETS.get(i))
              .setProtocol(PROTOCOL)
              .setDataPlanePort(NODES.get(i).dataPlanePort())
              .setLeader(NODES.get(0).identifier())
              .setLbs(LBS)
              .build();
      stub.startServiceNode(request);
    }
  }

  private static void initializeRouters() {
    for (int i = 0; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      NetworkOuterClass.InitializeRoutersRequest request =
          NetworkOuterClass.InitializeRoutersRequest.newBuilder().addAllTargets(TARGETS).build();
      stub.initializeRouters(request);
    }
  }

  private static void initializeServiceNode() {
    for (int i = 0; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      NetworkOuterClass.InitializeServiceNodeRequest request =
          NetworkOuterClass.InitializeServiceNodeRequest.newBuilder().build();
      stub.initializeServiceNode(request);
    }
  }

  private static void enableInstrumentation() {
    for (int i = 0; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      NetworkOuterClass.SetInstrumentationRequest request =
          NetworkOuterClass.SetInstrumentationRequest.newBuilder().setActive(true).build();
      stub.setInstrumentation(request);
    }
  }

  private static void warmup() {
    NetworkOuterClass.WarmupRequest leaderRequest =
        NetworkOuterClass.WarmupRequest.newBuilder()
            .setWrite(true)
            .setTotalNumberOfOps(100)
            .build();
    NetworkOuterClass.WarmupRequest followerRequest =
        NetworkOuterClass.WarmupRequest.newBuilder()
            .setWrite(false)
            .setTotalNumberOfOps(100)
            .build();
    ExperimentGrpc.ExperimentBlockingStub leaderStub = STUBS.get(0);
    leaderStub.warmup(leaderRequest);
    for (int i = 1; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      stub.warmup(followerRequest);
    }
  }

  private static void startUniformWorkload() {
    long start = System.currentTimeMillis() + 500;

    // Writer Request
    NetworkOuterClass.StartWorkloadRequest writerRequest =
        NetworkOuterClass.StartWorkloadRequest.newBuilder()
            .setDelay(1000)
            .setNumberOfOps(2000)
            .setDiscard(1000)
            .setStart(start)
            .setWritePercentage(1)
            .build();
    // Reader Request
    NetworkOuterClass.StartWorkloadRequest readerRequest =
        NetworkOuterClass.StartWorkloadRequest.newBuilder()
            .setDelay(1000)
            .setNumberOfOps(2000)
            .setDiscard(1000)
            .setStart(start)
            .setWritePercentage(0)
            .build();

    CountDownLatch latch = new CountDownLatch(N + 1);

    // Leader (writer / reader)
    ExperimentGrpc.ExperimentBlockingStub leaderStub = STUBS.get(0);
    Util.CACHED_THREAD_POOL.submit(
        () -> {
          leaderStub.startWorkload(writerRequest);
          latch.countDown();
        });
    Util.CACHED_THREAD_POOL.submit(
        () -> {
          leaderStub.startWorkload(readerRequest);
          latch.countDown();
        });

    // Remainder all readers
    for (int i = 1; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      Util.CACHED_THREAD_POOL.submit(
          () -> {
            stub.startWorkload(readerRequest);
            latch.countDown();
            LOGGER.info("Count down {}, {}", N, latch.getCount());
          });
    }

    try {
      latch.await();
      LOGGER.info("Wait done");
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void shutdown() {
    CHANNELS.forEach(ManagedChannel::shutdownNow);
    for (Session session : SSH) {
      session.disconnect();
    }
  }
}
