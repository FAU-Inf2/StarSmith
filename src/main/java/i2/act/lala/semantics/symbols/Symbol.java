package i2.act.lala.semantics.symbols;

import i2.act.lala.ast.Declaration;

public abstract class Symbol<D extends Declaration> {

  protected final String name;

  protected final D declaration;

  public Symbol(final String name, final D declaration) {
    this.name = name;
    this.declaration = declaration;
  }

  public final String getName() {
    return this.name;
  }

  public final D getDeclaration() {
    return this.declaration;
  }

}
