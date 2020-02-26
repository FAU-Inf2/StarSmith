package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

public final class AttributeModifier extends LaLaASTNode {

  public static enum AttributeModifierKind {

    MOD_INH("inh"),
    MOD_SYN("syn"),
    MOD_GRD("grd");

    public final String stringRepresentation; 

    private AttributeModifierKind(final String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

    public static final AttributeModifierKind fromStringRepresentation(
        final String stringRepresentation) {
      for (final AttributeModifierKind modifierKind : values()) {
        if (modifierKind.stringRepresentation.equals(stringRepresentation)) {
          return modifierKind;
        }
      }

      return null;
    }

  }

  private final AttributeModifierKind modifierKind;

  public AttributeModifier(final SourceRange sourceRange,
      final AttributeModifierKind modifierKind) {
    super(sourceRange);
    this.modifierKind = modifierKind;
  }

  public final AttributeModifierKind getModifierKind() {
    return this.modifierKind;
  }

  public final boolean isInheritedAttribute() {
    return this.modifierKind == AttributeModifierKind.MOD_INH;
  }

  public final boolean isSynthesizedAttribute() {
    return this.modifierKind == AttributeModifierKind.MOD_SYN;
  }

  public final boolean isGuardAttribute() {
    return this.modifierKind == AttributeModifierKind.MOD_GRD;
  }

  @Override
  public final String toString() {
    return this.modifierKind.stringRepresentation;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
