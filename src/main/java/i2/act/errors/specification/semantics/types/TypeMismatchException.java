package i2.act.errors.specification.semantics.types;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.info.SourcePosition;
import i2.act.lala.semantics.types.Type;

public final class TypeMismatchException extends InvalidLanguageSpecificationException {
  
  private final Type expectedType;
  private final Type actualType;

  public TypeMismatchException(final SourcePosition sourcePosition, final Type expectedType,
       final Type actualType) {
    super(new LanguageSpecificationError(sourcePosition,
        String.format("expected type '%s' but found '%s'", expectedType, actualType)));
    this.expectedType = expectedType;
    this.actualType = actualType;
  }

  public final Type getExpectedType() {
    return this.expectedType;
  }

  public final Type getActualType() {
    return this.actualType;
  }

}
