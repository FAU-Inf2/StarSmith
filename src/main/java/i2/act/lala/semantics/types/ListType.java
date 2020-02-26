package i2.act.lala.semantics.types;

public final class ListType implements Type {

  private final Type elementType;

  public ListType(final Type elementType) {
    this.elementType = elementType;
  }

  public final Type getElementType() {
    return this.elementType;
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof ListType)) {
      return false;
    }

    final ListType otherListType = (ListType) other;

    return this.elementType.equals(otherListType.elementType);
  }

  @Override
  public final String toString() {
    return String.format("list<%s>", this.elementType.toString());
  }


}
