package i2.act.errors.specification.attributes;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.AttributeAccess;
import i2.act.lala.ast.ProductionDeclaration;

public final class WrongSourceAttributeException extends InvalidLanguageSpecificationException {

  private final AttributeAccess sourceAttribute;
  private final ProductionDeclaration productionDeclaration;

  public WrongSourceAttributeException(final AttributeAccess sourceAttribute,
      final ProductionDeclaration productionDeclaration) {
    super(new LanguageSpecificationError(sourceAttribute.getSourcePosition(),
        String.format("invalid source attribute %s.%s in production '%s'",
            sourceAttribute.getTargetName().getName(), sourceAttribute.getAttributeName().getName(),
            productionDeclaration.getName())));

    this.sourceAttribute = sourceAttribute;
    this.productionDeclaration = productionDeclaration;
  }

  public final AttributeAccess getSourceAttribute() {
    return this.sourceAttribute;
  }

  public final ProductionDeclaration getProductionDeclaration() {
    return this.productionDeclaration;
  }

}
