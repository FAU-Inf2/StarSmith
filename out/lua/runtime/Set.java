package runtime;

import java.util.LinkedHashSet;

public final class Set {

  public final java.util.Set<Object> elements;

  private Set() {
    this.elements = new LinkedHashSet<Object>();
  }

  protected final Set clone() {
    final Set clone = new Set();
    clone.elements.addAll(this.elements);

    return clone;
  }

  @Override
  public final String toString() {
    final StringBuilder builder = new StringBuilder();

    builder.append("{");

    boolean first = true;
    for (final Object element : this.elements) {
      if (first) {
        first = false;
      } else {
        builder.append(", ");
      }

      builder.append(element);
    }

    builder.append("}");

    return builder.toString();
  }
  
  // ===============================================================================================

  public static final Set empty() {
    return new Set();
  }

  public static final Set of(final Object element) {
    final Set set = new Set();
    set.elements.add(element);
    return set;
  }

  public static final int size(final Set set) {
    return set.elements.size();
  }

  public static final Set add(final Set set, final Object element) {
    final Set newSet = set.clone();
    newSet.elements.add(element);

    return newSet;
  }

  public static final boolean contains(final Set set, final Object element) {
    return set.elements.contains(element);
  }

  public static final Set union(final Set setOne, final Set setTwo) {
    final Set newSet = setOne.clone();
    newSet.elements.addAll(setTwo.elements);

    return newSet;
  }

  public static final Set intersection(final Set setOne, final Set setTwo) {
    final Set newSet = new Set();

    for (final Object element : setOne.elements) {
      if (setTwo.elements.contains(element)) {
        newSet.elements.add(element);
      }
    }

    return newSet;
  }

}
