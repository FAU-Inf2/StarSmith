package i2.act.lala.semantics.symbols;

import i2.act.lala.ast.AttributeDeclaration;
import i2.act.lala.ast.AttributeModifier.AttributeModifierKind;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AttributeSymbol extends Symbol<AttributeDeclaration> {

  private final ClassSymbol containingClass;
  private final Set<AttributeSymbol> dependencies;

  public AttributeSymbol(final String name, final AttributeDeclaration attributeDeclaration,
      final ClassSymbol containingClass) {
    super(name, attributeDeclaration);

    this.containingClass = containingClass;
    this.dependencies = new LinkedHashSet<AttributeSymbol>();
  }

  public final ClassSymbol getContainingClass() {
    return this.containingClass;
  }

  public final void addDependency(final AttributeSymbol dependency) {
    this.dependencies.add(dependency);
  }

  public final Set<AttributeSymbol> getDependencies() {
    return Collections.unmodifiableSet(this.dependencies);
  }

  public final AttributeModifierKind getModifierKind() {
    return this.getDeclaration().getModifier().getModifierKind();
  }

  public final boolean isInheritedAttribute() {
    return this.getDeclaration().getModifier().getModifierKind() == AttributeModifierKind.MOD_INH;
  }

  public final boolean isSynthesizedAttribute() {
    return this.getDeclaration().getModifier().getModifierKind() == AttributeModifierKind.MOD_SYN;
  }

  public final boolean isGuardAttribute() {
    return this.getDeclaration().getModifier().getModifierKind() == AttributeModifierKind.MOD_GRD;
  }

  public final String getTypeName() {
    if (isGuardAttribute()) {
      return "boolean";
    }
    return this.declaration.getTypeName().getName().getName();
  }

  @Override
  public final String toString() {
    return String.format("<%s:%s>", this.containingClass.getName(), getName());
  }

}
