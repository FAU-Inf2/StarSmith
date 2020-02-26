package i2.act.lala.ast;

import i2.act.errors.RPGException;
import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class EscapeSequence extends LaLaASTNode implements StringElement {

  public static enum EscapeToken {

    ESCAPE_NEWLINE("\\n"),
    ESCAPE_INDENT("\\+"),
    ESCAPE_UNINDENT("\\-"),
    ESCAPE_DOLLAR("\\$"),
    ESCAPE_HASH("\\#"),
    ESCAPE_QUOTE("\\\"");

    public final String stringRepresentation;

    private EscapeToken(final String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

    public static final EscapeToken fromStringRepresentation(final String stringRepresentation) {
      for (final EscapeToken escapeToken : EscapeToken.values()) {
        if (escapeToken.stringRepresentation.equals(stringRepresentation)) {
          return escapeToken;
        }
      }
      throw new RPGException(String.format("unknown escape token '%s'", stringRepresentation));
    }

    @Override
    public final String toString() {
      return this.stringRepresentation;
    }

  }

  private final EscapeToken escapeToken;

  public EscapeSequence(final SourceRange sourceRange, final EscapeToken escapeToken) {
    super(sourceRange);
    this.escapeToken = escapeToken;
  }

  public final EscapeToken getEscapeToken() {
    return this.escapeToken;
  }

  public final String getStringRepresentation() {
    return this.escapeToken.stringRepresentation;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
