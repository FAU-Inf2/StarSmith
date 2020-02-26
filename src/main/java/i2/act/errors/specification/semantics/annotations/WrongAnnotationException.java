package i2.act.errors.specification.semantics.annotations;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.Annotation;

public final class WrongAnnotationException extends InvalidLanguageSpecificationException {
  
  private final Annotation annotation;

  public WrongAnnotationException(final Annotation annotation) {
    super(new LanguageSpecificationError(annotation.getSourcePosition(),
        String.format("'%s' annotation not allowed here", annotation.getSymbol().getName())));
    this.annotation = annotation;
  }

  public final Annotation getAnnotation() {
    return this.annotation;
  }

}
