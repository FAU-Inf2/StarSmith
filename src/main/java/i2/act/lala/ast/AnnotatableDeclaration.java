package i2.act.lala.ast;

import i2.act.lala.semantics.symbols.AnnotationSymbol;

import java.util.List;

public interface AnnotatableDeclaration {

  public void addAnnotation(final Annotation annotation);

  public List<Annotation> getAnnotations();

  public Annotation findAnnotation(final AnnotationSymbol annotationSymbol);

  public boolean hasAnnotation(final AnnotationSymbol annotationSymbol);

}
