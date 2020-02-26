package i2.act.fuzzer.regex.ast;

import i2.act.fuzzer.regex.ast.visitors.RegularExpressionVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Group extends Atom {

  private final List<Range> ranges;
  private final Bounds bounds;

  public Group(final List<Range> ranges, final Bounds bounds) {
    this.ranges = new ArrayList<Range>(ranges);
    this.bounds = bounds;
  }

  public final List<Range> getRanges() {
    return Collections.unmodifiableList(this.ranges);
  }

  public final boolean hasBounds() {
    return this.bounds != null;
  }

  public final Bounds getBounds() {
    return this.bounds;
  }

  @Override
  public final boolean hasAlternatives() {
    if (this.ranges.size() > 1) {
      return true;
    }

    for (final Range range : this.ranges) {
      if (range.hasAlternatives()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public final <P, R> R accept(final RegularExpressionVisitor<P, R> visitor, final P parameter) {
    return visitor.visit(this, parameter);
  }

}
