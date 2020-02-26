package i2.act.lala.semantics.types;

public final class StringType implements Type {
  
  public static final StringType INSTANCE = new StringType();

  private StringType() {
    /* intentionally left blank */
  }

  @Override
  public final boolean equals(final Object other) {
    // there is only one instance
    return (other == this);
  }

  @Override
  public final String toString() {
    return "string";
  }

}
