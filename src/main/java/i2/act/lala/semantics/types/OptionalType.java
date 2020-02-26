package i2.act.lala.semantics.types;

public final class OptionalType implements Type {

  private final Type type;

  public OptionalType(final Type type) {
    this.type = type;
  }

  public final Type getType() {
    return this.type;
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof OptionalType)) {
      return false;
    }

    final OptionalType otherOptionalType = (OptionalType) other;

    return this.type.equals(otherOptionalType.type);
  }

  @Override
  public final String toString() {
    return String.format("opt<%s>", this.type.toString());
  }

}
