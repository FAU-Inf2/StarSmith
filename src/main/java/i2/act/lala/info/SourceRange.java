package i2.act.lala.info;

public final class SourceRange {

  public static final SourceRange UNKNOWN =
      new SourceRange(
        SourcePosition.UNKNOWN,
        SourcePosition.UNKNOWN
      );
  
  private SourcePosition begin;
  private SourcePosition end;

  public SourceRange(final SourcePosition begin, final SourcePosition end) {
    this.begin = begin;
    this.end = end;
  }

  public final SourcePosition getBegin() {
    return this.begin;
  }

  public final void setBegin(final SourcePosition begin) {
    this.begin = begin;
  }

  public final SourcePosition getEnd() {
    return this.end;
  }

  public final void setEnd(final SourcePosition end) {
    this.end = end;
  }

  @Override
  public final String toString() {
    return String.format("%s--%s", this.begin, this.end);
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof SourceRange)) {
      return false;
    }

    final SourceRange otherSourceRange = (SourceRange) other;

    return this.begin.equals(otherSourceRange.begin)
      && this.end.equals(otherSourceRange.end);
  }

}
