package i2.act.errors.specification.attributes;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.ProductionDeclaration;
import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.lala.semantics.symbols.ChildSymbol;

public final class MissingAttributeEvaluationRule extends InvalidLanguageSpecificationException {

  private final ProductionDeclaration productionDeclaration;
  private final ChildSymbol childSymbol;
  private final AttributeSymbol attributeSymbol;
  
  public MissingAttributeEvaluationRule(final ProductionDeclaration productionDeclaration,
      final ChildSymbol childSymbol, final AttributeSymbol attributeSymbol) {
    super(new LanguageSpecificationError(productionDeclaration.getSourcePosition(),
        String.format("production '%s' does not evaluate %s.%s",
            productionDeclaration.getName(), childSymbol.getName(), attributeSymbol.getName())));

    this.productionDeclaration = productionDeclaration;
    this.childSymbol = childSymbol;
    this.attributeSymbol = attributeSymbol;
  }

  public final ProductionDeclaration getProductionDeclaration() {
    return this.productionDeclaration;
  }

  public final ChildSymbol getChildSymbol() {
    return this.childSymbol;
  }

  public final AttributeSymbol getAttributeSymbol() {
    return this.attributeSymbol;
  }

}
