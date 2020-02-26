package i2.act.util;

import java.util.Objects;

public class Pair<F, S> {

  private F first;
  private S second;

  public Pair() {
    this(null, null);
  }

  public Pair(final F first, final S second) {
    this.first = first;
    this.second = second;
  }

  public final F getFirst() {
    return this.first;
  }

  public final S getSecond() {
    return this.second;
  }

  public final void setFirst(final F first) {
    this.first = first;
  }

  public final void setSecond(final S second) {
    this.second = second;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(this.first, this.second);
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof Pair)) {
      return false;
    }

    final Pair<?, ?> otherPair = (Pair<?, ?>) other;
    return Objects.equals(this.first, otherPair.first)
        && Objects.equals(this.second, otherPair.second);
  }

  @Override
  public String toString() {
    return String.format("(%s, %s)", this.first, this.second);
  }

}
