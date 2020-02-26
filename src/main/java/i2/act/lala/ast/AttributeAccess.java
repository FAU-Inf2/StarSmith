package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.AttributeSymbol;

public final class AttributeAccess extends AttributeExpression {

  private final Identifier targetName;
  private final Identifier attributeName;

  private AttributeSymbol symbol;

  public AttributeAccess(final SourceRange sourceRange,
       final Identifier targetName, final Identifier attributeName) {
    super(sourceRange);
    this.targetName = targetName;
    this.attributeName = attributeName;
  }

  public final void setSymbol(final AttributeSymbol symbol) {
    this.symbol = symbol;
  }

  public final AttributeSymbol getSymbol() {
    return this.symbol;
  }

  public final Identifier getTargetName() {
    return this.targetName;
  }

  public final Identifier getAttributeName() {
    return this.attributeName;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
