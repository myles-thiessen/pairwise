package mthiessen.protocol.eventschedulingprimitive;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// Stores LBs on message delays.
public class LowerBounds {
  public static final LowerBounds NOOP = new LowerBounds("");

  private final Map<Object, Map<Object, Integer>> lbs = new HashMap<>();

  public LowerBounds(final String lbs) {
    if (lbs != null && !lbs.isEmpty()) {
      this.parse(new JSONObject(lbs));
    }
  }

  private void parse(final JSONObject object) {
    for (String key : object.keySet()) {
      this.lbs.put(key, new HashMap<>());
      JSONObject object1 = object.getJSONObject(key);
      for (String key1 : object1.keySet()) {
        this.lbs.get(key).put(key1, object1.getInt(key1));
      }
    }
    System.out.println(this.lbs);
  }

  public int getLowerBound(final Object sender, final Object receiver) {
    return this.lbs.get(sender).get(receiver);
  }
}
