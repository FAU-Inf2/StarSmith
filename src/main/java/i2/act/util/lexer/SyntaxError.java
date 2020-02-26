package i2.act.util.lexer;

public final class SyntaxError extends RuntimeException {

  public SyntaxError(final String message) {
    this(message, -1);
  }

  public SyntaxError(final String message, final int position) {
    super(String.format(
        "%s invalid input: %s", (position >= 0) ? "[" + position + "]" : "", message));
  }

}
