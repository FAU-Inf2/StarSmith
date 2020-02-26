package i2.act.fuzzer;

public abstract class Attribute {

  public static enum AttributeKind {
    INHERITED,
    SYNTHESIZED,
    GUARD;
  }

  private final String name;
  private final String className;
  private final AttributeKind kind;

  public Attribute(final String name, final String className, final AttributeKind kind) {
    this.name = name;
    this.className = className;
    this.kind = kind;
  }

  public final String getName() {
    return this.name;
  }

  public final String getQualifiedName() {
    return String.format("%s:%s", this.className, this.name);
  }

  public final AttributeKind getKind() {
    return this.kind;
  }

  public final boolean isInherited() {
    return this.kind == AttributeKind.INHERITED;
  }

  public final boolean isSynthesized() {
    return this.kind == AttributeKind.SYNTHESIZED;
  }

  public final boolean isGuard() {
    return this.kind == AttributeKind.GUARD;
  }

  @Override
  public final String toString() {
    return String.format("<ATTR:%s>", getQualifiedName());
  }


  // --- to implement in the sub-classes ---


  public abstract boolean hasValue(final Node node);

  public abstract Object getValue(final Node node);

  public abstract void setValue(final Node node, final Object value);

  public abstract boolean clearValue(final Node node);

}
