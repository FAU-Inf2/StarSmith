package i2.act.lala.semantics.types;

import i2.act.lala.semantics.symbols.AttributeSymbol;

public final class AttributeReferenceType extends ReferenceType<AttributeSymbol> {

  public static final AttributeReferenceType INSTANCE = new AttributeReferenceType();

  private AttributeReferenceType() {
    /* intentionally left blank */
  }

  @Override
  public final boolean equals(final Object other) {
    // there is only one instance
    return (other == this);
  }

  @Override
  public final String toString() {
    return "attrref";
  }

}
