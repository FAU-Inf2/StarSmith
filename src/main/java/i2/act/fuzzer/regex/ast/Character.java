package i2.act.fuzzer.regex.ast;

import i2.act.fuzzer.regex.ast.visitors.RegularExpressionVisitor;

public final class Character extends Atom {

  private final String character;

  public Character(final String character) {
    this.character = character;
  }

  public final String getChar() {
    return this.character;
  }

  @Override
  public final boolean hasAlternatives() {
    return false;
  }

  @Override
  public final <P, R> R accept(final RegularExpressionVisitor<P, R> visitor, final P parameter) {
    return visitor.visit(this, parameter);
  }

}
