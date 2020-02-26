package runtime;

import i2.act.fuzzer.runtime.Printable;

import java.util.Objects;

public final class Symbol implements Printable {

  public static final Symbol create(final String name, final Type type) {
    return new Symbol(name, type);
  }

  public static final String getName(final Symbol symbol) {
    return symbol.name;
  }

  public static final Type getType(final Symbol symbol) {
    return symbol.type;
  }

  // -----------------------------------------------------------------------------------------------

  public final String name;
  public final Type type;

  public Symbol(final String name, final Type type) {
    this.name = name;
    this.type = type;
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof Symbol)) {
      return false;
    }

    final Symbol otherSymbol = (Symbol) other;

    return Objects.equals(this.name, otherSymbol.name)
        && Objects.equals(this.type, otherSymbol.type);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(this.name, this.type);
  }

  @Override
  public final String toString() {
    return String.format("(%s, %s)", this.name, this.type);
  }

  @Override
  public final String print() {
    return this.name;
  }

}
