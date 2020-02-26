package i2.act.lala.semantics.attributes;

import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.lala.semantics.symbols.ChildSymbol;

public final class AttributeInstance {

  public final ChildSymbol childSymbol;
  public final AttributeSymbol attributeSymbol;

  public AttributeInstance(final ChildSymbol childSymbol, final AttributeSymbol attributeSymbol) {
    this.childSymbol = childSymbol;
    this.attributeSymbol = attributeSymbol;
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof AttributeInstance)) {
      return false;
    }

    final AttributeInstance otherAttributeInstance = (AttributeInstance) other;

    return this.childSymbol.equals(otherAttributeInstance.childSymbol)
        && this.attributeSymbol.equals(otherAttributeInstance.attributeSymbol);
  }

  @Override
  public final int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = (result * PRIME) + this.childSymbol.hashCode();
    result = (result * PRIME) + this.attributeSymbol.hashCode();
    return result;
  }

  public final String format() {
    return String.format("%s.%s", this.childSymbol.getName(), this.attributeSymbol.getName());
  }

  @Override
  public final String toString() {
    return String.format("%s.%s", this.childSymbol.getName(), this.attributeSymbol);
  }

}
