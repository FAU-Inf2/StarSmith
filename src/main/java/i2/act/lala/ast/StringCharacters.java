package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class StringCharacters extends LaLaASTNode implements StringElement {

  private final String characters;

  public StringCharacters(final SourceRange sourceRange, final String characters) {
    super(sourceRange);
    this.characters = characters;
  }

  public final String getCharacters() {
    return this.characters;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
