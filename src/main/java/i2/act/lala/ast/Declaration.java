package i2.act.lala.ast;

import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.Symbol;

public abstract class Declaration extends LaLaASTNode {

  protected final Identifier identifier;

  public Declaration(final SourceRange sourceRange, final Identifier identifier) {
    super(sourceRange);
    this.identifier = identifier;
  }

  public final Identifier getIdentifier() {
    return this.identifier;
  }

  // may be overridden in a subclass with a more specific return type
  public Symbol getSymbol() {
    return this.identifier.getSymbol();
  }

  public final String getName() {
    return this.identifier.getName();
  }

  @Override
  public String toString() {
    return super.toString() + ":" + getName();
  }

}
