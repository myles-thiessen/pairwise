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

public class ExperimentYCSBVaryRMWMaster {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentYCSBVaryRMWMaster.class);
  private static final List<Util.Node> NODES = new LinkedList<>();

  private static final List<Session> SSH = new LinkedList<>();
  private static final List<ManagedChannel> CHANNELS = new LinkedList<>();
  private static final List<ExperimentGrpc.ExperimentBlockingStub> STUBS = new LinkedList<>();
  private static String PROTOCOL;
  private static String WORKLOAD;
  private static int N;
  private static List<String> TARGETS;

  public static void main(final String[] args)
      throws JSchException, IOException, InterruptedException {
    PROTOCOL = args[0];
    WORKLOAD = args[1];
    N = args.length - 2;
    TARGETS = List.of(args).subList(2, args.length);

    sshConnections();
    initializeConnections();
    startServiceNode();
    initializeRouters();
    initializeServiceNode();
    loadYCSB();
    startYCSB();
    shutdown();

    // Shutdown the process as all work is complete.
    System.exit(0);
  }

  private static void sshConnections() throws JSchException, IOException {
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
              .setStateMachineType(NetworkOuterClass.StateMachine.ROCKS)
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

  private static void loadYCSB() {
    CountDownLatch latch = new CountDownLatch(N);

    for (int i = 0; i < N; i++) {
      Util.Node node = NODES.get(i);
      Session session = SSH.get(i);
      Util.CACHED_THREAD_POOL.submit(
          () -> {
            try {
              ChannelExec channel = (ChannelExec) session.openChannel("exec");
              channel.setCommand(
                  "./pairwise-YCSB/bin/ycsb.sh load pairwise -p pairwise.ip="
                      + node.ip()
                      + " -p pairwise.port="
                      + node.experimentPlanePort()
                      + " -P ./pairwise-YCSB/workloads/"
                      + WORKLOAD
                      + " -threads 8 > ycsb-load.dat");
              channel.connect();
              handleSSHCommand(channel);
              channel.disconnect();

              latch.countDown();
              LOGGER.info("Count down {}, {}", N, latch.getCount());
            } catch (Exception e) {
              e.printStackTrace();
            }
          });
    }

    try {
      latch.await();
      LOGGER.info("Wait done");
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void startYCSB() {
    CountDownLatch latch = new CountDownLatch(N);

    for (int i = 0; i < N; i++) {
      Util.Node node = NODES.get(i);
      Session session = SSH.get(i);
      Util.CACHED_THREAD_POOL.submit(
          () -> {
            try {
              ChannelExec channel = (ChannelExec) session.openChannel("exec");
              channel.setCommand(
                  "./pairwise-YCSB/bin/ycsb.sh run pairwise -p pairwise.ip="
                      + node.ip()
                      + " -p pairwise.port="
                      + node.experimentPlanePort()
                      + " -P ./pairwise-YCSB/workloads/"
                      + WORKLOAD
                      + " -threads 8 > ycsb-write.dat");
              channel.connect();
              handleSSHCommand(channel);
              channel.disconnect();

              latch.countDown();
              LOGGER.info("Count down {}, {}", N, latch.getCount());
            } catch (Exception e) {
              e.printStackTrace();
            }
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
