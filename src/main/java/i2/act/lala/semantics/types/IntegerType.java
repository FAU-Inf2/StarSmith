package i2.act.lala.semantics.types;

public final class IntegerType implements Type {
  
  public static final IntegerType INSTANCE = new IntegerType();

  private IntegerType() {
    /* intentionally left blank */
  }

  @Override
  public final boolean equals(final Object other) {
    // there is only one instance
    return (other == this);
  }

  @Override
  public final String toString() {
    return "int";
  }

}
