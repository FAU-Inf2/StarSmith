package i2.act.lala.semantics.symbols;

import i2.act.lala.ast.LocalAttributeDefinition;

public final class LocalAttributeSymbol extends Symbol<LocalAttributeDefinition> {

  private final ProductionSymbol containingProduction;

  public LocalAttributeSymbol(final String name,
      final LocalAttributeDefinition localAttributeDefinition,
      final ProductionSymbol containingProduction) {
    super(name, localAttributeDefinition);

    this.containingProduction = containingProduction;
  }

  public final ProductionSymbol getContainingProduction() {
    return this.containingProduction;
  }

  @Override
  public final String toString() {
    return String.format("<LOC:%s>", getName());
  }

}
