package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.LocalAttributeSymbol;

public final class LocalAttributeAccess extends AttributeExpression {

  private final Identifier attributeName;

  private LocalAttributeSymbol symbol;

  public LocalAttributeAccess(final SourceRange sourceRange, final Identifier attributeName) {
    super(sourceRange);
    this.attributeName = attributeName;
  }

  public final void setSymbol(final LocalAttributeSymbol symbol) {
    this.attributeName.setSymbol(symbol);
  }

  public final LocalAttributeSymbol getSymbol() {
    return (LocalAttributeSymbol) this.attributeName.getSymbol();
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
