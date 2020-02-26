package i2.act.errors.specification;

import i2.act.lala.ast.LaLaASTNode;
import i2.act.lala.info.SourcePosition;

public final class LanguageSpecificationError {

  private final SourcePosition sourcePosition;
  private final String message;

  public LanguageSpecificationError(final LaLaASTNode astNode, final String message) {
    this(astNode.getSourcePosition(), message);
  }

  public LanguageSpecificationError(final SourcePosition sourcePosition, final String message) {
    this.sourcePosition = sourcePosition;
    this.message = message;
  }

  public final SourcePosition getSourcePosition() {
    return this.sourcePosition;
  }

  public final String getMessage() {
    return this.message;
  }

  @Override
  public final String toString() {
    return String.format("[%s]: %s", this.sourcePosition, this.message);
  }

  // ==========================================================================

  public static final void fail(final LaLaASTNode astNode, final String message) {
    throw new InvalidLanguageSpecificationException(
        new LanguageSpecificationError(astNode, message));
  }

  public static final void fail(final SourcePosition sourcePosition, final String message) {
    throw new InvalidLanguageSpecificationException(
        new LanguageSpecificationError(sourcePosition, message));
  }

}
