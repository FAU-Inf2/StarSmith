package i2.act.lala.semantics.attributes;

import i2.act.errors.RPGException;
import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.lala.semantics.symbols.ClassSymbol;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ISSIGraph {

  private final ClassSymbol classSymbol;

  private final Set<AttributeSymbol> attributes;
  private final Map<AttributeSymbol, Set<AttributeSymbol>> dependencies;

  public ISSIGraph(final ClassSymbol classSymbol) {
    this.classSymbol = classSymbol;
    this.attributes = new HashSet<AttributeSymbol>();
    this.dependencies = new HashMap<AttributeSymbol, Set<AttributeSymbol>>();
  }

  public final ClassSymbol getClassSymbol() {
    return this.classSymbol;
  }

  private final void checkAttributeAdded(final AttributeSymbol attribute) {
    if (!this.attributes.contains(attribute)) {
      throw new RPGException("unknown attribute: " + attribute);
    }
  }

  public final void addAttribute(final AttributeSymbol attribute) {
    this.attributes.add(attribute);
  }

  public final void addDependency(final AttributeSymbol from, final AttributeSymbol to) {
    checkAttributeAdded(from);
    checkAttributeAdded(to);

    final Set<AttributeSymbol> tos;

    if (this.dependencies.containsKey(from)) {
      tos = this.dependencies.get(from);
    } else {
      tos = new HashSet<AttributeSymbol>();
      this.dependencies.put(from, tos);
    }

    tos.add(to);
  }

  public final boolean hasDependency(final AttributeSymbol from, final AttributeSymbol to) {
    final Set<AttributeSymbol> tos = this.dependencies.get(from);

    if (tos == null) {
      return false;
    }

    return tos.contains(to);
  }

  public final Set<AttributeSymbol> getAttributes() {
    return Collections.unmodifiableSet(this.attributes);
  }

  public final Set<AttributeSymbol> getDependencies(final AttributeSymbol from) {
    if (this.dependencies.containsKey(from)) {
      return Collections.unmodifiableSet(this.dependencies.get(from));
    } else {
      return Collections.unmodifiableSet(new HashSet<AttributeSymbol>());
    }
  }

}
