package i2.act.errors;

public final class RPGException extends RuntimeException {

  public RPGException() {
    super();
  }

  public RPGException(final String message) {
    super(message);
  }

  public RPGException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
