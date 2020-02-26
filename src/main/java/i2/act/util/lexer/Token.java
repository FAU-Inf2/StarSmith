package i2.act.util.lexer;

public final class Token {
  
  public static enum Kind {
    TK_LPAREN("("),
    TK_RPAREN(")"),
    TK_LBRACE("{"),
    TK_RBRACE("}"),
    TK_COLON(":"),
    TK_QMARK("?"),
    TK_COMMA(","),
    TK_ARROW(">"),
    TK_IDENTIFIER("<id>"),
    TK_NUMBER("<num>"),
    TK_EOF("<EOF>");

    public final String stringRepresentation;

    private Kind(final String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

  }

  public final String string;
  public final Kind kind;

  public final int start;
  public final int end;

  public Token(final String string, final Kind kind, final int start, final int end) {
    this.string = string;
    this.kind = kind;
    this.start = start;
    this.end = end;
  }

  public final int length() {
    return this.end - this.start + 1;
  }

}
