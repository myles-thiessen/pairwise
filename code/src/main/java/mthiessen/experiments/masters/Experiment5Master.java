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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Experiment5Master {
  private static final List<Util.Node> NODES = new LinkedList<>();

  private static final List<Session> SSH = new LinkedList<>();

  private static final List<ManagedChannel> CHANNELS = new LinkedList<>();
  private static final List<ExperimentGrpc.ExperimentBlockingStub> STUBS = new LinkedList<>();
  private static String PROTOCOL;
  private static float WRITE_PERCENTAGE;
  private static int N;
  private static List<String> TARGETS;

  public static void main(final String[] args)
      throws JSchException, IOException, InterruptedException {
    PROTOCOL = args[0];
    WRITE_PERCENTAGE = Float.parseFloat(args[1]);
    N = args.length - 2;
    TARGETS = List.of(args).subList(2, args.length);

    sshConnections();
    initializeConnections();
    startServiceNode();
    initializeRouters();
    initializeServiceNode();
    warmup();
    startWorkload();
    //    startWorkload(10, false);
    //    startWorkload(SECONDS, true);
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

  private static void connect(final String format) {
    NetworkOuterClass.ResetRequest request = NetworkOuterClass.ResetRequest.newBuilder().build();

    for (int i = 0; true; i++) {
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

  private static String buildLB() {
    String leaderId = NODES.get(0).identifier();
    String follower1Id = NODES.get(1).identifier();
    String follower2Id = NODES.get(2).identifier();

    JSONObject baseObject = new JSONObject();
    JSONObject leaderObject = new JSONObject();
    JSONObject follower1Object = new JSONObject();
    JSONObject follower2Object = new JSONObject();
    baseObject.put(leaderId, leaderObject);
    baseObject.put(follower1Id, follower1Object);
    baseObject.put(follower2Id, follower2Object);

    leaderObject.put(leaderId, 0);
    leaderObject.put(follower1Id, 3);
    leaderObject.put(follower2Id, 13);

    follower1Object.put(follower1Id, 0);
    follower1Object.put(leaderId, 3);
    follower1Object.put(follower2Id, 12);

    follower2Object.put(follower2Id, 0);
    follower2Object.put(leaderId, 13);
    follower2Object.put(follower1Id, 12);

    return baseObject.toString();
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
    CountDownLatch latch = new CountDownLatch(N);

    ExperimentGrpc.ExperimentBlockingStub leaderStub = STUBS.get(0);
    Util.CACHED_THREAD_POOL.submit(
        () -> {
          leaderStub.warmup(leaderRequest);
          latch.countDown();
        });
    for (int i = 1; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      Util.CACHED_THREAD_POOL.submit(
          () -> {
            stub.warmup(followerRequest);
            latch.countDown();
          });
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void startWorkload() {
    long start = System.currentTimeMillis() + 500;

    // leader request
    NetworkOuterClass.StartWorkloadRequest request =
        NetworkOuterClass.StartWorkloadRequest.newBuilder()
            .setDelay(1000)
            .setNumberOfOps(11000)
            .setDiscard(1000)
            .setStart(start)
            .setWritePercentage(WRITE_PERCENTAGE)
            .build();

    CountDownLatch latch = new CountDownLatch(N);

    for (int i = 0; i < N; i++) {
      System.out.println("Starting workload for follower " + i);

      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      Util.CACHED_THREAD_POOL.submit(
          () -> {
            stub.startWorkload(request);
            latch.countDown();
            System.out.printf("Count down %d, %d%n", N, latch.getCount());
          });
    }

    try {
      latch.await();
      System.out.println("Wait done");
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  //  private static void startWorkload(final int seconds, final boolean measure) {
  //    CountDownLatch latch = new CountDownLatch(N);
  //
  //    for (int i = 0; i < N; i++) {
  //
  //      System.out.println("Starting workload for follower " + i);
  //
  //      NetworkOuterClass.StartBackToBackWorkloadRequest req =
  //          NetworkOuterClass.StartBackToBackWorkloadRequest.newBuilder()
  //              .setWritePercentage(WRITE_PERCENTAGE)
  //              .setSeconds(seconds)
  //              .setMeasure(measure)
  //              .build();
  //
  //      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
  //      Util.CACHED_THREAD_POOL.submit(
  //          () -> {
  //            stub.startBackToBackWorkload(req);
  //            latch.countDown();
  //            System.out.printf("Count down %d, %d%n", N, latch.getCount());
  //          });
  //    }
  //
  //    try {
  //      latch.await();
  //      System.out.println("Wait done");
  //    } catch (InterruptedException e) {
  //      throw new RuntimeException(e);
  //    }
  //  }

  private static void shutdown() {
    CHANNELS.forEach(ManagedChannel::shutdownNow);
    for (Session session : SSH) {
      session.disconnect();
    }
  }
}
