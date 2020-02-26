package i2.act.fuzzer.runtime;

import java.util.LinkedList;
import java.util.List;

public final class EmbeddedCode {

  private abstract static class Token {

    public static final void printIndentation(final StringBuilder builder, final int indentation) {
      for (int i = 0; i < indentation; ++i) {
        builder.append("  ");
      }
    }

    public abstract int printToken(final StringBuilder builder, final int indentation);

  }

  private static final class StringToken extends Token {

    public final String string;

    public StringToken(final String string) {
      this.string = string;
    }

    @Override
    public final int printToken(final StringBuilder builder, final int indentation) {
      builder.append(this.string);
      return indentation;
    }

  }

  private static final Token NEWLINE = new Token() {
    
    @Override
    public final int printToken(final StringBuilder builder, final int indentation) {
      builder.append("\n");
      printIndentation(builder, indentation);
      return indentation;
    }

  };

  private static final Token INDENT = new Token() {

    @Override
    public final int printToken(final StringBuilder builder, final int indentation) {
      builder.append("\n");
      printIndentation(builder, indentation + 1);
      return indentation + 1;
    }

  };

  private static final Token UNINDENT = new Token() {

    @Override
    public final int printToken(final StringBuilder builder, final int indentation) {
      builder.append("\n");
      printIndentation(builder, indentation - 1);
      return indentation - 1;
    }

  };

  private final List<Token> tokens;

  private EmbeddedCode() {
    this.tokens = new LinkedList<Token>();
  }

  // --------------------------------------------------------------------------

  public static final EmbeddedCode create() {
    return new EmbeddedCode();
  }

  public final EmbeddedCode print(final String string) {
    this.tokens.add(new StringToken(string));
    return this;
  }

  public final EmbeddedCode println(final String string) {
    print(string);
    this.tokens.add(NEWLINE);
    return this;
  }

  public final EmbeddedCode newline() {
    this.tokens.add(NEWLINE);
    return this;
  }
  
  public final EmbeddedCode indent() {
    this.tokens.add(INDENT);
    return this;
  }
  
  public final EmbeddedCode unindent() {
    this.tokens.add(UNINDENT);
    return this;
  }

  public final String printCode() {
    return printCode(0);
  }

  public final String printCode(final int indentation) {
    final StringBuilder builder = new StringBuilder();
    printCode(builder, indentation);

    return builder.toString();
  }

  public final void printCode(final StringBuilder builder, final int indentation) {
    int currentIndentation = indentation;

    for (final Token token : this.tokens) {
      currentIndentation = token.printToken(builder, currentIndentation);
    }
  }

}
