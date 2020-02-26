package runtime;

import i2.act.fuzzer.Node;
import java.util.List;
import java.util.ArrayList;

public abstract class ROL<T> {

  public static <T> ROL<T> add(ROL<T> rol, T obj) {
    return rol.add(obj);
  }

  public static <T> boolean contains(ROL<T> rol, T obj) {
    return rol.contains(obj);
  }

  public static <T> boolean isEmpty(ROL<T> rol) {
    return rol.list.isEmpty();
  }

  public static <T> T getRandom(ROL<T> rol, Node node) {
    return rol.getRandom(node);
  }

  public ROL<T> add(T obj) {
    ROL<T> newrol = clone();
    newrol.list.add(obj);
    return newrol;
  }

  public static <T> T get(ROL<T> rol, int index) {
    return rol.list.get(index);
  }

  public static <T> int size(ROL<T> rol) {
    return rol.list.size();
  }

  public boolean contains(T obj) {
    return list.stream().anyMatch(l -> equalsAccordingToList(l, obj));
  }

  public T getRandom(Node node) {
    return ListUtil.chooseRandom(list, node);
  }

  public List<T> getAll() {
    return list;
  }

  protected List<T> list = new ArrayList<T>();

  public abstract ROL<T> getNew();
  public abstract boolean equalsAccordingToList(T a, T b);

  protected ROL<T> clone() {
    ROL<T> r = getNew();
    r.list.addAll(this.list);
    return r;
  }

}

