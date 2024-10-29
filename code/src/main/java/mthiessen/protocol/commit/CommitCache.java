package mthiessen.protocol.commit;

import mthiessen.misc.Pair;
import mthiessen.protocol.Payloads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CommitCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommitCache.class);

  private final Map<Integer, Pair<Payloads.PrepareRequest, Pair<Object, Integer>>> cache =
      new TreeMap<>();

  public List<Pair<Payloads.PrepareRequest, Pair<Object, Integer>>> getCachedCommitsFromIndex(
      final int index) {

    List<Pair<Payloads.PrepareRequest, Pair<Object, Integer>>> response = new LinkedList<>();

    Set<Integer> toRemove = new HashSet<>();

    int last = index;

    for (Map.Entry<Integer, Pair<Payloads.PrepareRequest, Pair<Object, Integer>>> entry :
        this.cache.entrySet()) {

      Integer cachedKey = entry.getKey();

      // skip all entries that are less than the provided index
      if (cachedKey < index) continue;

      // If the key passes the above guard then the key is larger than or
      // equal to the index where we should start removing keys from the
      // commit cache. However, any keys we return must be in a continuous
      // range from index onwards until the largest index we have in the cache.
      if (cachedKey != last + 1) {
        break;
      }

      last = cachedKey;
      toRemove.add(last);
      response.add(entry.getValue());
    }

    // All entries we are returning will be removed from cache.l
    toRemove.forEach(this.cache::remove);

    return response;
  }

  public void addToCache(
      final int index,
      final Payloads.PrepareRequest request,
      final Pair<Object, Integer> operationIdentifier) {
    this.cache.put(index, new Pair<>(request, operationIdentifier));
  }
}
