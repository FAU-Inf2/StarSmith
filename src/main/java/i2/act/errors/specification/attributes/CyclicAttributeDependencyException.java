package i2.act.errors.specification.attributes;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.ProductionDeclaration;

public final class CyclicAttributeDependencyException
    extends InvalidLanguageSpecificationException {

  private final ProductionDeclaration productionDeclaration;

  public CyclicAttributeDependencyException(final ProductionDeclaration productionDeclaration) {
    super(new LanguageSpecificationError(productionDeclaration.getSourcePosition(),
        String.format("attribute dependency cycle detected in production '%s'",
            productionDeclaration.getName())));

    this.productionDeclaration = productionDeclaration;
  }

  public final ProductionDeclaration getProductionDeclaration() {
    return this.productionDeclaration;
  }

}
