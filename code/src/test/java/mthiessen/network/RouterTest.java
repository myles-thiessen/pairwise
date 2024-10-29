package mthiessen.network;

import mthiessen.misc.UniqueNumberGenerator;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class RouterTest {

  public static final int n = 10;

  public static void main(String[] args) {

    List<UUID> nodeIdentifiers = IntStream.range(0, n).mapToObj(i -> UUID.randomUUID()).toList();
    List<IRouter> routers = new LinkedList<>();

    for (UUID nodeIdentifier : nodeIdentifiers) {
      IRouterState state = new RouterState(nodeIdentifier);
      IRouter router =
          new FullyConnectedRouter(new UniqueNumberGenerator(), state, new MessageTimerManager());
      routers.add(router);
    }

    for (IRouter router : routers) {
      for (IRouter router2 : routers) {
        router.registerRouter(router2.getRouterState().getIdentifier(), new LocalLink(router2));
      }
    }

    for (IRouter router : routers) {
      router
          .getRouterState()
          .registerRequestHandlerForOp(IntegerOps.OP, IntegerRequestHandler::new);

      router
          .getRouterState()
          .registerResponseHandleForOp(IntegerOps.OP, IntegerResponseHandler::new);
    }

    routers.get(0).broadcastRequest(nodeIdentifiers, IntegerOps.OP, 10);
  }
}
