package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.ChildSymbol;

public final class ChildDeclaration extends Declaration implements StringElement {

  private final TypeName typeName;
  private final boolean automaticParentheses;

  public ChildDeclaration(final SourceRange sourceRange, final Identifier childName,
      final TypeName typeName, final boolean automaticParentheses) {
    super(sourceRange, childName);
    this.typeName = typeName;
    this.automaticParentheses = automaticParentheses;
  }

  @Override
  public final ChildSymbol getSymbol() {
    return (ChildSymbol) super.getSymbol();
  }

  public final Identifier getChildName() {
    return getIdentifier();
  }

  public final TypeName getTypeName() {
    return this.typeName;
  }

  public final boolean hasAutomaticParentheses() {
    return this.automaticParentheses;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
