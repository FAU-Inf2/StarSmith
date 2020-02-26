package i2.act.fuzzer.regex.ast;

import i2.act.fuzzer.regex.ast.visitors.RegularExpressionVisitor;

public final class Subexpression extends Atom {

  private final RegularExpression expression;
  private final Bounds bounds;

  public Subexpression(final RegularExpression expression, final Bounds bounds) {
    this.expression = expression;
    this.bounds = bounds;
  }

  public final RegularExpression getExpression() {
    return this.expression;
  }

  public final boolean hasBounds() {
    return this.bounds != null;
  }
  
  public final Bounds getBounds() {
    return this.bounds;
  }

  @Override
  public final boolean hasAlternatives() {
    if (this.bounds != null && this.bounds.hasAlternatives()) {
      return true;
    }

    return this.expression.hasAlternatives();
  }

  @Override
  public final <P, R> R accept(final RegularExpressionVisitor<P, R> visitor, final P parameter) {
    return visitor.visit(this, parameter);
  }

}
