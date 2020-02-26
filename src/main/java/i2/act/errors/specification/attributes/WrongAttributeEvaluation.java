package i2.act.errors.specification.attributes;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.ProductionDeclaration;
import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.lala.semantics.symbols.ChildSymbol;

public final class WrongAttributeEvaluation extends InvalidLanguageSpecificationException {

  private final ChildSymbol childSymbol;
  private final AttributeSymbol attributeSymbol;
  private final ProductionDeclaration productionDeclaration;

  public WrongAttributeEvaluation(final ChildSymbol childSymbol,
      final AttributeSymbol attributeSymbol, final ProductionDeclaration productionDeclaration) {
    super(new LanguageSpecificationError(productionDeclaration.getSourcePosition(),
        String.format("definition of %s.%s not allowed in production '%s'",
            childSymbol.getName(), attributeSymbol.getName(), productionDeclaration.getName())));

    this.childSymbol = childSymbol;
    this.attributeSymbol = attributeSymbol;
    this.productionDeclaration = productionDeclaration;
  }

  public final ChildSymbol getChildSymbol() {
    return this.childSymbol;
  }

  public final AttributeSymbol getAttributeSymbol() {
    return this.attributeSymbol;
  }

  public final ProductionDeclaration getProductionDeclaration() {
    return this.productionDeclaration;
  }

}
