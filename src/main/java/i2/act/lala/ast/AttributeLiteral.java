package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class AttributeLiteral extends AttributeExpression {

  private final String value;

  public AttributeLiteral(final SourceRange sourceRange, final String value) {
    super(sourceRange);
    this.value = value;
  }

  public final String getValue() {
    return this.value;
  }

  public final boolean isNil() {
    return "nil".equals(this.value);
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
