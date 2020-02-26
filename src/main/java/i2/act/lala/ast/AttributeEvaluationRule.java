package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

import java.util.List;

public final class AttributeEvaluationRule extends LaLaASTNode {

  private final AttributeAccess targetAttribute;
  private final AttributeExpression attributeExpression;

  public final boolean isAutoCopyRule;

  public AttributeEvaluationRule(final SourceRange sourceRange,
       final AttributeAccess targetAttribute, final AttributeExpression attributeExpression) {
    this(sourceRange, targetAttribute, attributeExpression, false);
  }

  public AttributeEvaluationRule(final SourceRange sourceRange,
      final AttributeAccess targetAttribute, final AttributeExpression attributeExpression,
      final boolean isAutoCopyRule) {
    super(sourceRange);

    this.targetAttribute = targetAttribute;
    this.attributeExpression = attributeExpression;
    this.isAutoCopyRule = isAutoCopyRule;
  }

  public final AttributeAccess getTargetAttribute() {
    return this.targetAttribute;
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
