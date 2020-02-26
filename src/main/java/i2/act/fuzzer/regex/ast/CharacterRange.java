package i2.act.fuzzer.regex.ast;

import i2.act.fuzzer.regex.ast.visitors.RegularExpressionVisitor;

public final class CharacterRange extends Range {

  private final Character lowerCharacter;
  private final Character upperCharacter;

  public CharacterRange(final Character lowerCharacter, final Character upperCharacter) {
    this.lowerCharacter = lowerCharacter;
    this.upperCharacter = upperCharacter;
  }

  public final Character getLowerCharacter() {
    return this.lowerCharacter;
  }

  public final Character getUpperCharacter() {
    return this.upperCharacter;
  }

  @Override
  public final boolean hasAlternatives() {
    return !this.lowerCharacter.getChar().equals(this.upperCharacter.getChar());
  }

  @Override
  public final <P, R> R accept(final RegularExpressionVisitor<P, R> visitor, final P parameter) {
    return visitor.visit(this, parameter);
  }

}
