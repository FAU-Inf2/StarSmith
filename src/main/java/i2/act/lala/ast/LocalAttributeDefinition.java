package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

import java.util.List;

public final class LocalAttributeDefinition extends Declaration {

  private final AttributeExpression attributeExpression;

  public LocalAttributeDefinition(final SourceRange sourceRange,
       final Identifier attributeName, final AttributeExpression attributeExpression) {
    super(sourceRange, attributeName);
    this.attributeExpression = attributeExpression;
  }

  public final Identifier getAttributeName() {
    return getIdentifier();
  }

  public final AttributeExpression getAttributeExpression() {
    return this.attributeExpression;
  }

  public final List<AttributeAccess> gatherSourceAttributes() {
    return this.attributeExpression.gatherSourceAttributes();
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
