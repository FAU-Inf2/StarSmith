package runtime;

import java.lang.StringBuilder;

public final class IdentifierBuilder {

  public static final IdentifierBuilder getNew() {
    return new IdentifierBuilder();
  }

  public static final IdentifierBuilder append(final IdentifierBuilder identifierbuilder,
      final String newpart) {
    final IdentifierBuilder newIdentifierBuilder = identifierbuilder.clone();
    newIdentifierBuilder.builder.append(newpart);

    return newIdentifierBuilder;
  }

  public static final String getString(final IdentifierBuilder identifierbuilder){
    return identifierbuilder.builder.toString();
  }

  // ===============================================================================================

  private final StringBuilder builder;

  private IdentifierBuilder() {
    builder = new StringBuilder();
  }

  protected final IdentifierBuilder clone() {
    final IdentifierBuilder clone = new IdentifierBuilder();
    clone.builder.append(this.builder);

    return clone;
  }

}
