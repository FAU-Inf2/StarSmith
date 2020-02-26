package i2.act.fuzzer.regex.parser;

import i2.act.fuzzer.regex.ast.*;
import i2.act.fuzzer.regex.parser.RegExToken.Kind;

import java.util.ArrayList;
import java.util.List;

public final class RegExParser {

  private static final class RegExLexer {

    private final char[] characters;

    private int position;

    public RegExLexer(final char[] characters) {
      this.characters = characters;
      this.position = 0;
    }

    public final int getPosition() {
      return this.position;
    }

    public final RegExToken peek() {
      return peek(false);
    }

    public final RegExToken peek(final boolean forceCharacterToken) {
      if (this.position >= this.characters.length) {
        return new RegExToken(null, Kind.TK_EOF);
      }

      if (forceCharacterToken) {
        return new RegExToken(String.valueOf(this.characters[this.position]), Kind.TK_CHARACTER);
      }

      int lookaheadPosition = this.position;
      final char firstChar = this.characters[lookaheadPosition];

      switch (firstChar) {
        case '|': {
          return new RegExToken(String.valueOf(firstChar), Kind.TK_PIPE);
        }
        case '(': {
          return new RegExToken(String.valueOf(firstChar), Kind.TK_LPAREN);
        }
        case ')': {
          return new RegExToken(String.valueOf(firstChar), Kind.TK_RPAREN);
        }
        case '[': {
          return new RegExToken(String.valueOf(firstChar), Kind.TK_LBRACK);
        }
        case ']': {
          return new RegExToken(String.valueOf(firstChar), Kind.TK_RBRACK);
        }
        case '{': {
          return new RegExToken(String.valueOf(firstChar), Kind.TK_LBRACE);
        }
        case '}': {
          return new RegExToken(String.valueOf(firstChar), Kind.TK_RBRACE);
        }
        case ',': {
          return new RegExToken(String.valueOf(firstChar), Kind.TK_COMMA);
        }
        case '-': {
          return new RegExToken(String.valueOf(firstChar), Kind.TK_MINUS);
        }
        default: {
          if (firstChar >= '0' && firstChar <= '9') {
            // TK_NUMBER

            final StringBuilder builder = new StringBuilder();
            builder.append(firstChar);

            while ((++lookaheadPosition < this.characters.length)
                && (this.characters[lookaheadPosition] >= '0'
                && this.characters[lookaheadPosition] <= '9')) {
              builder.append(this.characters[lookaheadPosition]);
            }

            return new RegExToken(builder.toString(), Kind.TK_NUMBER);
          } else {
            return new RegExToken(String.valueOf(firstChar), Kind.TK_CHARACTER);
          }
        }
      }
    }

    public final RegExToken pop(final boolean forceCharacterToken) {
      final RegExToken token = peek(forceCharacterToken);
      this.position += token.length();

      return token;
    }

    public final RegExToken assertPop(final Kind kind) {
      final RegExToken token = peek();

      if (token.kind != kind) {
        throw new RegExParserException(this.characters, this.position);
      }

      this.position += token.length();

      return token;
    }

    public final boolean skip(final Kind kind) {
      final RegExToken token = peek(false);

      if (token.kind == kind) {
        this.position += token.length();
        return true;
      } else {
        return false;
      }
    }

    public final void assertNotEOF() {
      if (peek().kind == Kind.TK_EOF) {
        throw new RegExParserException(this.characters, this.position);
      }
    }

  }

  public final RegularExpression parse(final String string) {
    return parse(string.toCharArray());
  }

  public final RegularExpression parse(final char[] characters) {
    final RegExLexer lexer = new RegExLexer(characters);

    final RegularExpression regularExpression = parseRegularExpression(lexer);

    lexer.assertPop(Kind.TK_EOF);

    return regularExpression;
  }

  private final RegularExpression parseRegularExpression(final RegExLexer lexer) {
    final List<Sequence> alternatives = new ArrayList<>();

    final Sequence firstAlternative = parseSequence(lexer);
    alternatives.add(firstAlternative);

    while (lexer.skip(Kind.TK_PIPE) && lexer.peek().kind != Kind.TK_RPAREN) {
      final Sequence sequence = parseSequence(lexer);
      alternatives.add(sequence);
    }

    final RegularExpression regularExpression = new RegularExpression(alternatives);
    return regularExpression;
  }

  private final Sequence parseSequence(final RegExLexer lexer) {
    final List<Atom> atoms = new ArrayList<>();

    while (lexer.peek().kind != Kind.TK_PIPE && lexer.peek().kind != Kind.TK_EOF
        && lexer.peek().kind != Kind.TK_RPAREN) {
      final Atom atom = parseAtom(lexer);
      atoms.add(atom);
    }

    final Sequence sequence = new Sequence(atoms);
    return sequence;
  }

  private final Atom parseAtom(final RegExLexer lexer) {
    if (lexer.peek().kind == Kind.TK_LPAREN) {
      lexer.assertPop(Kind.TK_LPAREN);

      final RegularExpression regularExpression = parseRegularExpression(lexer);

      lexer.assertPop(Kind.TK_RPAREN);

      final Bounds bounds;
      {
        if (lexer.peek().kind == Kind.TK_LBRACE) {
          bounds = parseBounds(lexer);
        } else {
          bounds = null;
        }
      }

      return new Subexpression(regularExpression, bounds);
    } else if (lexer.peek().kind == Kind.TK_LBRACK) {
      return parseGroup(lexer);
    } else {
      return parseCharacter(lexer);
    }
  }

  private final Group parseGroup(final RegExLexer lexer) {
    lexer.assertPop(Kind.TK_LBRACK);

    final List<Range> ranges = new ArrayList<>();

    final Range firstRange = parseRange(lexer);
    ranges.add(firstRange);

    while (lexer.peek().kind != Kind.TK_RBRACK) {
      final Range range = parseRange(lexer);
      ranges.add(range);
    }

    lexer.assertPop(Kind.TK_RBRACK);

    final Bounds bounds;
    {
      if (lexer.peek().kind == Kind.TK_LBRACE) {
        bounds = parseBounds(lexer);
      } else {
        bounds = null;
      }
    }

    return new Group(ranges, bounds);
  }

  private final Range parseRange(final RegExLexer lexer) {
    final i2.act.fuzzer.regex.ast.Character lowerCharacter = parseCharacter(lexer);

    if (lexer.skip(Kind.TK_MINUS)) {
      final i2.act.fuzzer.regex.ast.Character upperCharacter = parseCharacter(lexer);
      return new CharacterRange(lowerCharacter, upperCharacter);
    } else {
      return new SingleCharacter(lowerCharacter);
    }
  }

  private final Bounds parseBounds(final RegExLexer lexer) {
    lexer.assertPop(Kind.TK_LBRACE);

    final RegExToken minimum = lexer.assertPop(Kind.TK_NUMBER);

    lexer.assertPop(Kind.TK_COMMA);

    final RegExToken maximum = lexer.assertPop(Kind.TK_NUMBER);

    lexer.assertPop(Kind.TK_RBRACE);

    final Bounds bounds =
        new Bounds(Integer.parseInt(minimum.string), Integer.parseInt(maximum.string));
    return bounds;
  }

  private final i2.act.fuzzer.regex.ast.Character parseCharacter(final RegExLexer lexer) {
    lexer.assertNotEOF();

    final RegExToken token = lexer.pop(true);
    final i2.act.fuzzer.regex.ast.Character character =
        new i2.act.fuzzer.regex.ast.Character(token.string);

    return character;
  }

}
