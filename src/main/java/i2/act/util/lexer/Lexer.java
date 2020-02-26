package i2.act.util.lexer;

import i2.act.util.lexer.Token.Kind;

public final class Lexer {

  public static final char ESCAPED_IDENTIFIER_DELIMITER = '$';

  private final char[] characters;
  private final boolean escapedIdentifiers;

  private int position;

  public Lexer(final char[] characters, final boolean escapedIdentifiers) {
    this.characters = characters;
    this.escapedIdentifiers = escapedIdentifiers;
    this.position = 0;
  }

  public final int getPosition() {
    return this.position;
  }

  private static final boolean isWhitespaceCharacter(final char c) {
    return c == ' ' || c == '\n' || c == '\r' || c == '\t';
  }

  private static final boolean isIdentifierCharacter(final char c) {
    return Character.isJavaIdentifierStart(c);
  }

  private static final boolean isDigitCharacter(final char c) {
    return (c >= '0') && (c <= '9');
  }

  private final boolean isEscapedIdentifierDelimiter(final char c) {
    return this.escapedIdentifiers && c == ESCAPED_IDENTIFIER_DELIMITER;
  }

  public final Token peek() {
    return peek(false);
  }

  public final Token peek(final boolean allowNumber) {
    int lookaheadPosition = this.position;

    while (true) { // skip whitespace
      if (lookaheadPosition >= this.characters.length) {
        return new Token(null, Kind.TK_EOF, lookaheadPosition, lookaheadPosition);
      }

      final int start = lookaheadPosition;
      int end = start;

      final char firstChar = this.characters[lookaheadPosition];

      switch (firstChar) {
        case '(': {
          return new Token(String.valueOf(firstChar), Kind.TK_LPAREN,
              lookaheadPosition, lookaheadPosition);
        }
        case ')': {
          return new Token(String.valueOf(firstChar), Kind.TK_RPAREN,
              lookaheadPosition, lookaheadPosition);
        }
        case '{': {
          return new Token(String.valueOf(firstChar), Kind.TK_LBRACE,
              lookaheadPosition, lookaheadPosition);
        }
        case '}': {
          return new Token(String.valueOf(firstChar), Kind.TK_RBRACE,
              lookaheadPosition, lookaheadPosition);
        }
        case ':': {
          return new Token(String.valueOf(firstChar), Kind.TK_COLON,
              lookaheadPosition, lookaheadPosition);
        }
        case '?': {
          return new Token(String.valueOf(firstChar), Kind.TK_QMARK,
              lookaheadPosition, lookaheadPosition);
        }
        case ',': {
          return new Token(String.valueOf(firstChar), Kind.TK_COMMA,
              lookaheadPosition, lookaheadPosition);
        }
        case '>': {
          return new Token(String.valueOf(firstChar), Kind.TK_ARROW,
              lookaheadPosition, lookaheadPosition);
        }
        default: {
          if (isWhitespaceCharacter(firstChar)) {
            while ((++lookaheadPosition < this.characters.length)
                && (isWhitespaceCharacter(this.characters[lookaheadPosition]))) {
              ++end;
            }
          } else if (isEscapedIdentifierDelimiter(firstChar)) {
            final StringBuilder builder = new StringBuilder();
            // do not append first character (delimiter)

            while ((++lookaheadPosition < this.characters.length)
                && !isEscapedIdentifierDelimiter(this.characters[lookaheadPosition])) {
              ++end;
              builder.append(this.characters[lookaheadPosition]);
            }

            if (lookaheadPosition < this.characters.length
                && isEscapedIdentifierDelimiter(this.characters[lookaheadPosition])) {
              ++end;
            }

            return new Token(builder.toString(), Kind.TK_IDENTIFIER, start, end);
          } else if (isIdentifierCharacter(firstChar) || isDigitCharacter(firstChar)) {
            final StringBuilder builder = new StringBuilder();
            builder.append(firstChar);

            boolean allDigits = isDigitCharacter(firstChar);

            while ((++lookaheadPosition < this.characters.length)
                && (isIdentifierCharacter(this.characters[lookaheadPosition])
                    || isDigitCharacter(this.characters[lookaheadPosition]))) {
              ++end;
              builder.append(this.characters[lookaheadPosition]);

              allDigits &= isDigitCharacter(this.characters[lookaheadPosition]);
            }

            if (allDigits && allowNumber) {
              return new Token(builder.toString(), Kind.TK_NUMBER, start, end);
            } else {
              return new Token(builder.toString(), Kind.TK_IDENTIFIER, start, end);
            }
          } else {
            // unknown token
            throw new SyntaxError(
                String.format("invalid character '%s'", firstChar), lookaheadPosition);
          }
        }
      }
    }
  }

  public final Token pop() {
    final Token token = peek();
    this.position = token.end + 1;

    return token;
  }

  public final Token assertPop(final Kind kind) {
    return assertPop(kind, false);
  }

  public final Token assertPop(final Kind kind, final boolean allowNumber) {
    final Token token = peek(allowNumber);

    if (token.kind != kind) {
      throw new SyntaxError(
        String.format("expected %s, but found %s ('%s')", kind, token.kind, token.string),
        this.position);
    }

    this.position = token.end + 1;

    return token;
  }

  public final boolean skip(final Kind kind) {
    final Token token = peek();

    if (token.kind == kind) {
      this.position = token.end + 1;
      return true;
    } else {
      return false;
    }
  }

  public final void assertNotEOF() {
    if (peek().kind == Kind.TK_EOF) {
      throw new SyntaxError("unexpected EOF", this.position);
    }
  }

}
