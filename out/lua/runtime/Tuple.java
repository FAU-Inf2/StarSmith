package runtime;

import java.util.ArrayList;
import java.util.List;

public final class Tuple {

  public final List<Object> elements;

  public Tuple() {
    this.elements = new ArrayList<Object>();
  }

  private Tuple(final List<Object> elements) {
    this.elements = elements;
  }

  public final int size() {
    return this.elements.size();
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof Tuple)) {
      return false;
    }
    return this.elements.equals(((Tuple) other).elements);
  }

  @Override
  public final String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append("(");

    boolean first = true;
    for (final Object element : this.elements) {
      if (first) {
        first = false;
      } else {
        builder.append(", ");
      }

      builder.append(element);
    }

    builder.append(")");

    return builder.toString();
  }

  // ==============================================================================================

  public static final Tuple empty() {
    return new Tuple();
  }

  public static final Tuple from(final Object element) {
    final Tuple tuple = new Tuple();
    tuple.elements.add(element);
    return tuple;
  }

  public static final Tuple prepend(final Object element, final Tuple tuple) {
    final Tuple newTuple = new Tuple();
    newTuple.elements.add(element);
    newTuple.elements.addAll(tuple.elements);
    return newTuple;
  }

  public static final Tuple append(final Tuple tuple, final Object element) {
    final Tuple newTuple = new Tuple();
    newTuple.elements.addAll(tuple.elements);
    newTuple.elements.add(element);
    return newTuple;
  }

  public static final Object head(final Tuple tuple) {
    return tuple.elements.get(0);
  }

  public static final Tuple tail(final Tuple tuple) {
    final Tuple newTuple = new Tuple(new ArrayList<Object>(tuple.elements));
    newTuple.elements.remove(0);
    return newTuple;
  }

  public static final Tuple merge(final Tuple t1, final Tuple t2) {
    final Tuple tuple = new Tuple();
    tuple.elements.addAll(t1.elements);
    tuple.elements.addAll(t2.elements);
    return tuple;
  }

  public static final int size(final Tuple tuple) {
    return tuple.size();
  }

  public static final boolean isEmpty(final Tuple tuple) {
    return tuple.elements.isEmpty();
  }

  public static final Tuple toTypeTuple(final Tuple symbolTuple) {
    final Tuple typeTuple = new Tuple();
    for (final Object symbol : symbolTuple.elements) {
      typeTuple.elements.add(Symbol.getType((Symbol) symbol));
    }
    return typeTuple;
  }

}
