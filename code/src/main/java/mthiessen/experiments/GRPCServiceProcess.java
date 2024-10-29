package mthiessen.experiments;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import mthiessen.IProcess;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.Pair;
import mthiessen.misc.UniqueNumberGenerator;
import mthiessen.misc.Util;
import mthiessen.network.ILink;
import mthiessen.network.IRouter;
import mthiessen.network.MessageTimerManager;
import mthiessen.network.grpc.GRPCLink;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RequiredArgsConstructor
public class GRPCServiceProcess {
  private final List<ILink> transportLinks = new LinkedList<>();
  @Getter private ServiceProcess serviceProcess;

  @SneakyThrows
  public GRPCServiceProcess(final String node, final Function<IRouter, IProcess> nodeFactory) {
    Util.Node split = Util.split(node);
    this.serviceProcess =
        new ServiceProcess(
            split.identifier(),
            new UniqueNumberGenerator(),
            new MessageTimerManager(),
            nodeFactory);
  }

  public void initializeRouters(final String[] args) {
    List<Pair<Object, ILink>> localTransportLinks = new LinkedList<>();
    for (String target : args) {
      Util.Node split = Util.split(target);
      GRPCLink transportLink =
          new GRPCLink(this.serviceProcess.getRouter(), split.ip(), split.dataPlanePort());
      localTransportLinks.add(new Pair<>(split.identifier(), transportLink));
      this.transportLinks.add(transportLink);
    }
    this.serviceProcess.registerRoutes(localTransportLinks);
  }

  public void initializeServiceNode() {
    this.serviceProcess.initialize();
  }

  public void setInitialize(final boolean active) {
    this.serviceProcess.setInitialize(active);
  }

  public Map<Pair<Object, Integer>, IOperationMeasurement> getTrace() {
    return this.serviceProcess.getTrace();
  }

  public void shutdown() {
    this.transportLinks.forEach(ILink::shutdown);
  }
}
