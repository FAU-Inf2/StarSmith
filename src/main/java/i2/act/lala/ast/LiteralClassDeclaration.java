package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class LiteralClassDeclaration extends ClassDeclaration {

  private final StringLiteral regularExpression;

  public LiteralClassDeclaration(final SourceRange sourceRange, final Identifier className,
      final StringLiteral regularExpression) {
    super(sourceRange, className);
    this.regularExpression = regularExpression;
  }

  public final StringLiteral getRegularExpression() {
    return this.regularExpression;
  }

  public final String getRegularExpressionString() {
    return this.regularExpression.toString(true);
  }

  @Override
  public final boolean isLiteralNode() {
    return true;
  }

  @Override
  public final boolean isGeneratorNode() {
    return false;
  }

  @Override
  public final boolean isListClass() {
    return false;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
