package i2.act.lala.semantics.symbols;

import i2.act.lala.ast.ClassDeclaration;
import i2.act.lala.ast.Identifier;

public final class ClassSymbol extends Symbol<ClassDeclaration> {

  private SymbolTable<ProductionSymbol> productions;
  private SymbolTable<AttributeSymbol> attributes;

  public ClassSymbol(final String name, final ClassDeclaration classDeclaration) {
    super(name, classDeclaration);
  }

  public final void setProductions(final SymbolTable<ProductionSymbol> productions) {
    this.productions = productions;
  }

  public final SymbolTable<ProductionSymbol> getProductions() {
    return this.productions;
  }

  public final void setAttributes(final SymbolTable<AttributeSymbol> attributes) {
    this.attributes = attributes;
  }

  public final SymbolTable<AttributeSymbol> getAttributes() {
    return this.attributes;
  }

  public final AttributeSymbol lookupAttribute(final Identifier attributeName) {
    return this.attributes.lookupSymbol(attributeName);
  }

}
