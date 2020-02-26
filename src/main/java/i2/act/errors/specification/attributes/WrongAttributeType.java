package i2.act.errors.specification.attributes;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.AttributeDeclaration;

public final class WrongAttributeType extends InvalidLanguageSpecificationException {

  private final AttributeDeclaration attributeDeclaration;

  public WrongAttributeType(final AttributeDeclaration attributeDeclaration) {
    super(new LanguageSpecificationError(attributeDeclaration.getSourcePosition(),
        String.format("wrong type of attribute '%s'",
            attributeDeclaration.getIdentifier().getName())));

    this.attributeDeclaration = attributeDeclaration;
  }

  public final AttributeDeclaration getAttributeDeclaration() {
    return this.attributeDeclaration;
  }

}
