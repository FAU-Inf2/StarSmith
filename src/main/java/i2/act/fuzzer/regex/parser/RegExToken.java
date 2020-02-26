package i2.act.fuzzer.regex.parser;

public final class RegExToken {
  
  public static enum Kind {
    TK_PIPE("|"),
    TK_LPAREN("("),
    TK_RPAREN(")"),
    TK_LBRACK("["),
    TK_RBRACK("]"),
    TK_LBRACE("{"),
    TK_RBRACE("}"),
    TK_COMMA(","),
    TK_MINUS("-"),
    TK_CHARACTER("<char>"),
    TK_NUMBER("<num>"),
    TK_EOF("<EOF>");

    public final String stringRepresentation;

    private Kind(final String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

  }

  public final String string;
  public final Kind kind;

  public RegExToken(final String string, final Kind kind) {
    this.string = string;
    this.kind = kind;
  }

  public final int length() {
    if (this.string == null) {
      return 0;
    }
    return this.string.length();
  }

}
