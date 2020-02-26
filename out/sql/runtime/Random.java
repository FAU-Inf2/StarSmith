package runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import i2.act.fuzzer.Node;

public class Random {

  static long PRIME_NUMER = 243301L;

  public static final java.util.Random getRandom(final Node node) {
    return new java.util.Random(node.id);
  }

  public static int nextInt(final Node node, int bound) {
    return (int)((new java.util.Random(PRIME_NUMER * node.id).nextLong() % bound + bound) % bound);
  }

  public static <T> T getRandomFromList(final List<T> list, final Node node) {
    return list.get(Random.nextInt(node, list.size()));
  }

  public static int getRandomInBounds(final int from, final int to_exclusive, final Node node) {
    assert to_exclusive > from;
    return from + getRandom(node).nextInt(to_exclusive - from);
  }

  public static <T> T getRandomFromSet(final Set<T> set, final Node node) {
    ArrayList<T> ls = new ArrayList<T>(set);
    return getRandomFromList(ls, node);
  }

}
