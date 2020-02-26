package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class ChildReference extends AttributeExpression {

  private final Identifier childName;

  public ChildReference(final SourceRange sourceRange, final Identifier childName) {
    super(sourceRange);
    this.childName = childName;
  }

  public final Identifier getChildName() {
    return this.childName;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
