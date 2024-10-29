package mthiessen.experiments;

import lombok.Getter;
import mthiessen.IProcess;
import mthiessen.IUniqueNumberGenerator;
import mthiessen.instrumentation.IOperationMeasurement;
import mthiessen.misc.Pair;
import mthiessen.network.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
public class ServiceProcess {
  private final Object identifier;

  private final IRouter router;

  private final IProcess process;

  public ServiceProcess(
      final Object identifier,
      final IUniqueNumberGenerator uniqueNumberGenerator,
      final IMessageTimerManager messageTimerManager,
      final Function<IRouter, IProcess> processFactory) {
    this.identifier = identifier;
    IRouterState routerState = new RouterState(this.identifier);
    this.router = new FullyConnectedRouter(uniqueNumberGenerator, routerState, messageTimerManager);
    this.process = processFactory.apply(this.router);
  }

  public void registerRoutes(final List<Pair<Object, ILink>> transportLinkList) {
    for (Pair<Object, ILink> transportLinkPair : transportLinkList) {
      Object nodeIdentifier = transportLinkPair.k1();
      ILink transportLink = transportLinkPair.k2();
      this.router.registerRouter(nodeIdentifier, transportLink);
    }
  }

  public void initialize() {
    this.process.initialize();
  }

  public void setInitialize(final boolean active) {
    this.process.setInstrumentation(active);
  }

  public Map<Pair<Object, Integer>, IOperationMeasurement> getTrace() {
    return this.process.getTrace();
  }
}
