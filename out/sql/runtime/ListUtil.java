package runtime;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import i2.act.fuzzer.Node;

public final class ListUtil {

  public static <T> T chooseRandom(final List<T> list, final Node node) {
    return Random.getRandomFromList(list, node);
  }

  public static <T> java.util.List<T> pickRandomSubset(final List<T> list, final Node node) {
    java.util.Random rand = Random.getRandom(node);
    java.util.List<T> newlist = new java.util.ArrayList<>();

    for(T elem : list){
      if(rand.nextBoolean()){
        newlist.add(elem);
      }
    }

    return newlist;
  }

  public static <T> java.util.List<T> permutateRandom(final List<T> list, final Node node) {
    final List<T> newlist = new ArrayList<>(list);
    Collections.shuffle(newlist, Random.getRandom(node));
    return newlist;
  }

  public static <T> boolean isEmpty(final List<T> list){
    return list.isEmpty();
  }

  public static <T> String printCommaSeparated (final List<T> list) {
    List<String> stringlist = list.stream().map(s -> s.toString()).collect(Collectors.toList());
    return String.join(", ", stringlist);
  }

  public static <T> boolean contains(final List<T> list, T elem) {
    return list.contains(elem);
  }

}
