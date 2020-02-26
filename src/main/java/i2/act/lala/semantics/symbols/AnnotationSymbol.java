package i2.act.lala.semantics.symbols;

import i2.act.lala.ast.Declaration;
import i2.act.lala.semantics.types.*;

public final class AnnotationSymbol extends Symbol<Declaration> {

  public static final AnnotationSymbol ANNOTATION_PRECEDENCE =
      new AnnotationSymbol("precedence", false, true, new AnnotationType(IntegerType.INSTANCE));

  public static final AnnotationSymbol ANNOTATION_WEIGHT =
      new AnnotationSymbol("weight", false, true, new AnnotationType(IntegerType.INSTANCE));

  public static final AnnotationSymbol ANNOTATION_FEATURE =
      new AnnotationSymbol("feature", false, true, new AnnotationType(StringType.INSTANCE));

  public static final AnnotationSymbol ANNOTATION_LIST =
      new AnnotationSymbol("list", true, false,
          new AnnotationType(new OptionalType(IntegerType.INSTANCE)));

  public static final AnnotationSymbol ANNOTATION_UNIT =
      new AnnotationSymbol("unit", true, false, new AnnotationType());

  public static final AnnotationSymbol ANNOTATION_COUNT =
      new AnnotationSymbol("count", true, false, new AnnotationType(IntegerType.INSTANCE));

  public static final AnnotationSymbol ANNOTATION_MAX_HEIGHT =
      new AnnotationSymbol("max_height", true, false, new AnnotationType(IntegerType.INSTANCE));

  public static final AnnotationSymbol ANNOTATION_MAX_ALTERNATIVES =
      new AnnotationSymbol("max_alternatives", true, false,
          new AnnotationType(IntegerType.INSTANCE));

  public static final AnnotationSymbol ANNOTATION_HIDDEN =
      new AnnotationSymbol("hidden", true, true, new AnnotationType());

  public static final AnnotationSymbol ANNOTATION_COPY =
      new AnnotationSymbol("copy", true, true,
          new AnnotationType(new OptionalType(new ListType(AttributeReferenceType.INSTANCE))));

  public static final AnnotationSymbol[] predefinedAnnotationSymbols = {
      ANNOTATION_LIST, ANNOTATION_UNIT, ANNOTATION_WEIGHT, ANNOTATION_COUNT, ANNOTATION_MAX_HEIGHT,
      ANNOTATION_MAX_ALTERNATIVES, ANNOTATION_HIDDEN, ANNOTATION_FEATURE, ANNOTATION_PRECEDENCE,
      ANNOTATION_COPY
  };


  // ========================================================================================


  private final boolean forClasses;
  private final boolean forProductions;

  private final AnnotationType type;
  
  private AnnotationSymbol(final String name, final boolean forClasses,
      final boolean forProductions, final AnnotationType type) {
    super(name, null);

    this.forClasses = forClasses;
    this.forProductions = forProductions;
    this.type = type;
  }

  public final boolean forClasses() {
    return this.forClasses;
  }

  public final boolean forProductions() {
    return this.forProductions;
  }

  public final AnnotationType getType() {
    return this.type;
  }

}
