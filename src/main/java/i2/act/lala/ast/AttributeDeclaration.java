package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.AttributeSymbol;

public final class AttributeDeclaration extends Declaration {

  private final AttributeModifier modifier;

  private final AttributeTypeName typeName;

  public AttributeDeclaration(final SourceRange sourceRange,
      final Identifier attributeName, final AttributeModifier modifier,
      final AttributeTypeName typeName) {
    super(sourceRange, attributeName);
    this.modifier = modifier;
    this.typeName = typeName;
  }

  @Override
  public final AttributeSymbol getSymbol() {
    return (AttributeSymbol) super.getSymbol();
  }

  public final Identifier getAttributeName() {
    return getIdentifier();
  }

  public final AttributeModifier getModifier() {
    return this.modifier;
  }

  public final AttributeTypeName getTypeName() {
    return this.typeName;
  }

  public final boolean hasTypeName() {
    return this.typeName != null;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
