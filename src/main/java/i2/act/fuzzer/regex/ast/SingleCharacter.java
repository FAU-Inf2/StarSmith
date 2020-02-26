package i2.act.fuzzer.regex.ast;

import i2.act.fuzzer.regex.ast.visitors.RegularExpressionVisitor;

public final class SingleCharacter extends Range {

  private final Character character;

  public SingleCharacter(final Character character) {
    this.character = character;
  }

  public final Character getCharacter() {
    return this.character;
  }

  @Override
  public final boolean hasAlternatives() {
    return this.character.hasAlternatives();
  }

  @Override
  public final <P, R> R accept(final RegularExpressionVisitor<P, R> visitor, final P parameter) {
    return visitor.visit(this, parameter);
  }

}
