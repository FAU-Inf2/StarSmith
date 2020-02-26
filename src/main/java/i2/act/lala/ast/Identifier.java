package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.Symbol;

public final class Identifier extends LaLaASTNode {

  private final String name;

  private Symbol symbol;

  public Identifier(final SourceRange sourceRange, final String name) {
    super(sourceRange);
    this.name = name;
  }

  public final String getName() {
    return this.name;
  }

  public final Symbol getSymbol() {
    return this.symbol;
  }

  public final void setSymbol(final Symbol symbol) {
    this.symbol = symbol;
  }

  @Override
  public final String toString() {
    return String.format("<%s>", this.name);
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
