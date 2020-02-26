package i2.act.errors.specification.attributes;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.ProductionDeclaration;
import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.lala.semantics.symbols.ChildSymbol;

public final class AttributeRedefinitionException extends InvalidLanguageSpecificationException {

  private final ChildSymbol childSymbol;
  private final AttributeSymbol attributeSymbol;
  private final ProductionDeclaration productionDeclaration;

  public AttributeRedefinitionException(final ChildSymbol childSymbol,
      final AttributeSymbol attributeSymbol, final ProductionDeclaration productionDeclaration) {
    super(new LanguageSpecificationError(productionDeclaration.getSourcePosition(),
        String.format("production '%s' has multiple definitions for %s.%s",
            productionDeclaration.getName(), childSymbol.getName(), attributeSymbol.getName())));

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
