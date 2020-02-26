package i2.act.lala.info;

public final class SourcePosition implements Comparable<SourcePosition> {

  public static final SourcePosition UNKNOWN = new SourcePosition(-1, -1);

  private final int line;
  private final int column;

  public SourcePosition(final int line, final int column) {
    this.line = line;
    this.column = column;
  }

  public final int getLine() {
    return this.line;
  }

  public final int getColumn() {
    return this.column;
  }

  @Override
  public final String toString() {
    if (this == SourcePosition.UNKNOWN) {
      return "<UNKNOWN>";
    }

    return String.format("%d:%d", this.line, this.column);
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof SourcePosition)) {
      return false;
    }

    final SourcePosition otherSourcePosition = (SourcePosition) other;

    return this.line == otherSourcePosition.line
        && this.column == otherSourcePosition.column;
  }

  @Override
  public final int compareTo(final SourcePosition other) {
    if (this.line != other.line) {
      return this.line - other.line;
    }

    return this.column - other.column;
  }

}
