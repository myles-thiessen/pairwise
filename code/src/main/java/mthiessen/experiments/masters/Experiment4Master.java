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

public class Experiment4Master {
  private static final List<Util.Node> NODES = new LinkedList<>();

  private static final List<Session> SSH = new LinkedList<>();

  private static final List<ManagedChannel> CHANNELS = new LinkedList<>();
  private static final List<ExperimentGrpc.ExperimentBlockingStub> STUBS = new LinkedList<>();
  private static String PROTOCOL;
  private static String SPLIT;

  private static int DELAY;
  private static int N;
  private static List<String> TARGETS;

  public static void main(final String[] args)
      throws JSchException, IOException, InterruptedException {
    PROTOCOL = args[0];
    SPLIT = args[1];
    DELAY = Integer.parseInt(args[2]);
    N = args.length - 3;
    TARGETS = List.of(args).subList(3, args.length);

    sshConnections();
    initializeConnections();
    // setLatency();
    startServiceNode();
    initializeRouters();
    initializeServiceNode();
    //    warmup();
    // enableInstrumentation();
    warmup1();
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
    JSONObject baseObject = new JSONObject();

    for (int i = 0; i < N; i++) {
      JSONObject ilbs = new JSONObject();
      int iId = Integer.parseInt(NODES.get(i).identifier());
      baseObject.put(String.valueOf(iId), ilbs);

      for (int j = 0; j < N; j++) {
        int jId = Integer.parseInt(NODES.get(j).identifier());

        int ijlb = 0;

        if (iId != jId) {
          if (1 <= iId && iId <= 16) { // i is in M
            if (1 <= jId && jId <= 16) { // j is in M
              ijlb = 0;
            } else if (16 < jId && jId <= 32) { // j is in NV
              ijlb = 3;
            } else if (32 < jId && jId <= 48) { // j is in NC
              ijlb = 13;
            } else if (48 < jId && jId <= 64) { // j is in F
              ijlb = 19;
            } else if (64 < jId && jId <= 80) { // j is in S
              ijlb = 20;
            }
          } else if (16 < iId && iId <= 32) { // i is in NV
            if (1 <= jId && jId <= 16) { // j is in M
              ijlb = 3;
            } else if (16 < jId && jId <= 32) { // j is in NV
              ijlb = 0;
            } else if (32 < jId && jId <= 48) { // j is in NC
              ijlb = 12;
            } else if (48 < jId && jId <= 64) { // j is in F
              ijlb = 22;
            } else if (64 < jId && jId <= 80) { // j is in S
              ijlb = 23;
            }
          } else if (32 < iId && iId <= 48) { // i is in NC
            if (1 <= jId && jId <= 16) { // j is in M
              ijlb = 13;
            } else if (16 < jId && jId <= 32) { // j is in NV
              ijlb = 12;
            } else if (32 < jId && jId <= 48) { // j is in NC
              ijlb = 0;
            } else if (48 < jId && jId <= 64) { // j is in F
              ijlb = 30;
            } else if (64 < jId && jId <= 80) { // j is in S
              ijlb = 29;
            }
          } else if (48 < iId && iId <= 64) { // i is in F
            if (1 <= jId && jId <= 16) { // j is in M
              ijlb = 19;
            } else if (16 < jId && jId <= 32) { // j is in NV
              ijlb = 22;
            } else if (32 < jId && jId <= 48) { // j is in NC
              ijlb = 30;
            } else if (48 < jId && jId <= 64) { // j is in F
              ijlb = 0;
            } else if (64 < jId && jId <= 80) { // j is in S
              ijlb = 14;
            }
          } else if (64 < iId && iId <= 80) { // i is in S
            if (1 <= jId && jId <= 16) { // j is in M
              ijlb = 20;
            } else if (16 < jId && jId <= 32) { // j is in NV
              ijlb = 23;
            } else if (32 < jId && jId <= 48) { // j is in NC
              ijlb = 29;
            } else if (48 < jId && jId <= 64) { // j is in F
              ijlb = 4;
            } else if (64 < jId && jId <= 80) { // j is in S
              ijlb = 0;
            }
          }
        }

        ilbs.put(String.valueOf(jId), ijlb);
      }
    }

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

  private static void enableInstrumentation() {
    for (int i = 0; i < N; i++) {
      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      NetworkOuterClass.SetInstrumentationRequest request =
          NetworkOuterClass.SetInstrumentationRequest.newBuilder().setActive(true).build();
      stub.setInstrumentation(request);
    }
  }

  private static void warmup1() {
    long start = System.currentTimeMillis() + 500;

    NetworkOuterClass.StartThroughputWriteWorkloadRequest leaderRequest =
        NetworkOuterClass.StartThroughputWriteWorkloadRequest.newBuilder().setDelay(150).build();

    CountDownLatch latch = new CountDownLatch(N);

    Util.CACHED_THREAD_POOL.submit(() -> STUBS.get(0).startThroughputWriteWorkload(leaderRequest));

    for (int i = 0; i < N; i++) {

      int window = 1000;

      System.out.println("Starting warmup for follower " + i);

      NetworkOuterClass.StartThroughputReadWorkloadRequest readerRequest =
          NetworkOuterClass.StartThroughputReadWorkloadRequest.newBuilder()
              .setNumberOfWindows(20)
              .setStart(start)
              .setWindow(window)
              .setMeasure(false)
              .setDiscard(0)
              .build();

      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      Util.CACHED_THREAD_POOL.submit(
          () -> {
            stub.startThroughputReadWorkload(readerRequest);
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

    STUBS
        .get(0)
        .stopThroughputWriteWorkload(
            NetworkOuterClass.StopThroughputWriteWorkloadRequest.newBuilder().build());

    System.out.println("Stopped writing");
  }

  private static void startUniformWorkload() {
    long start = System.currentTimeMillis() + 500;

    String[] split = SPLIT.split("-");

    NetworkOuterClass.StartThroughputWriteWorkloadRequest leaderRequest =
        NetworkOuterClass.StartThroughputWriteWorkloadRequest.newBuilder().setDelay(DELAY).build();

    // The condition is because INV will not make progress on any follower if
    // the rmwReq frequency is to high so in those cases we don't bother waiting.
    CountDownLatch latch = new CountDownLatch(PROTOCOL.equals("INV") && DELAY < 150 ? 1 : N);

    Util.CACHED_THREAD_POOL.submit(() -> STUBS.get(0).startThroughputWriteWorkload(leaderRequest));

    for (int i = 0; i < N; i++) {

      float s = 100;
      int window = (int) (1000 * (s / 100));

      System.out.println("Starting follower " + i + " with window " + window);

      NetworkOuterClass.StartThroughputReadWorkloadRequest readerRequest =
          NetworkOuterClass.StartThroughputReadWorkloadRequest.newBuilder()
              .setNumberOfWindows(60)
              .setStart(start)
              .setWindow(window)
              .setMeasure(true)
              .setDiscard(0)
              .build();

      ExperimentGrpc.ExperimentBlockingStub stub = STUBS.get(i);
      Util.CACHED_THREAD_POOL.submit(
          () -> {
            stub.startThroughputReadWorkload(readerRequest);
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

    STUBS
        .get(0)
        .stopThroughputWriteWorkload(
            NetworkOuterClass.StopThroughputWriteWorkloadRequest.newBuilder().build());

    System.out.println("Stopped writing");
  }

  private static void shutdown() {
    CHANNELS.forEach(ManagedChannel::shutdownNow);
    for (Session session : SSH) {
      session.disconnect();
    }
  }
}
