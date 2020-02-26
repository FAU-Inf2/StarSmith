package i2.act.lala.semantics.symbols;

import i2.act.lala.ast.ChildDeclaration;

public final class ChildSymbol extends Symbol<ChildDeclaration> {
  
  private final ClassSymbol type;
  
  public ChildSymbol(final String name, final ChildDeclaration childDeclaration,
      final ClassSymbol type) {
    super(name, childDeclaration);

    this.type = type;
  }

  public final ClassSymbol getType() {
    return this.type;
  }

}
