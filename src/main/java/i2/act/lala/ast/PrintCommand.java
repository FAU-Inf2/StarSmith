package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class PrintCommand extends LaLaASTNode implements StringElement {

  private final AttributeExpression expression;

  public PrintCommand(final SourceRange sourceRange, final AttributeExpression expression) {
    super(sourceRange);
    this.expression = expression;
  }

  public final AttributeExpression getExpression() {
    return this.expression;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
