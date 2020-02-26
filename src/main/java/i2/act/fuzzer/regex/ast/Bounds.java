package i2.act.fuzzer.regex.ast;

import i2.act.fuzzer.regex.ast.visitors.RegularExpressionVisitor;

public final class Bounds extends ASTNode {

  private final int minimum;
  private final int maximum;

  public Bounds(final int minimum, final int maximum) {
    this.minimum = minimum;
    this.maximum = maximum;
  }

  public final int getMinimum() {
    return this.minimum;
  }

  public final int getMaximum() {
    return this.maximum;
  }
  
  @Override
  public final boolean hasAlternatives() {
    return this.maximum != this.minimum;
  }

  @Override
  public final <P, R> R accept(final RegularExpressionVisitor<P, R> visitor, final P parameter) {
    return visitor.visit(this, parameter);
  }

}
