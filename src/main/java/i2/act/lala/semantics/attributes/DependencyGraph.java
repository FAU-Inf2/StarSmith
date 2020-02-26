package i2.act.lala.semantics.attributes;

import i2.act.errors.RPGException;
import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.lala.semantics.symbols.ChildSymbol;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class DependencyGraph {

  private static final boolean DEBUG = false;

  private final Set<AttributeInstance> attributes;
  private final Map<AttributeInstance, Integer> attributeIndexes;
  private final Map<AttributeInstance, Set<AttributeInstance>> dependencies;

  public DependencyGraph() {
    this.attributes = new LinkedHashSet<AttributeInstance>();
    this.attributeIndexes = new HashMap<AttributeInstance, Integer>();
    this.dependencies = new HashMap<AttributeInstance, Set<AttributeInstance>>();
  }

  public final DependencyGraph clone() {
    final DependencyGraph clone = new DependencyGraph();

    // copy attributes
    {
      for (final AttributeInstance attribute : this.attributes) {
        clone.addAttribute(attribute);
      }
    }

    // copy dependencies
    {
      for (final AttributeInstance from : this.dependencies.keySet()) {
        final Set<AttributeInstance> tos = this.dependencies.get(from);
        assert (tos != null);
        for (final AttributeInstance to : tos) {
          clone.addDependency(from, to);
        }
      }
    }
  
    return clone;
  }

  private final void checkAttributeAdded(final AttributeInstance attribute) {
    if (!this.attributes.contains(attribute)) {
      throw new RPGException("unknown attribute: " + attribute);
    }
  }

  public final void addAttribute(final AttributeInstance attribute) {
    if (this.attributes.contains(attribute)) {
      return;
    }

    this.attributes.add(attribute);

    final int index = this.attributeIndexes.size();
    this.attributeIndexes.put(attribute, index);
  }

  public final void addDependency(final AttributeInstance from, final AttributeInstance to) {
    checkAttributeAdded(from);
    checkAttributeAdded(to);

    final Set<AttributeInstance> tos;

    if (this.dependencies.containsKey(from)) {
      tos = this.dependencies.get(from);
    } else {
      tos = new LinkedHashSet<AttributeInstance>();
      this.dependencies.put(from, tos);
    }

    tos.add(to);
  }

  public final void addAllDependencies(final AttributeSymbol from, final AttributeSymbol to) {
    for (final AttributeInstance attributeInstanceFrom : this.attributes) {
      if (attributeInstanceFrom.attributeSymbol == from) {
        for (final AttributeInstance attributeInstanceTo : this.attributes) {
          if (attributeInstanceTo.attributeSymbol == to) {
            addDependency(attributeInstanceFrom, attributeInstanceTo);
          }
        }
      }
    }
  }

  public final boolean hasDependency(final AttributeInstance from, final AttributeInstance to) {
    final Set<AttributeInstance> tos = this.dependencies.get(from);

    if (tos == null) {
      return false;
    }

    return tos.contains(to);
  }

  public final Set<AttributeInstance> getAttributes() {
    return Collections.unmodifiableSet(this.attributes);
  }

  public final AttributeInstance getAttribute(final ChildSymbol childSymbol,
      final AttributeSymbol attributeSymbol) {
    final AttributeInstance instance = new AttributeInstance(childSymbol, attributeSymbol);
    for (final AttributeInstance otherInstance : this.attributes) {
      if (instance.equals(otherInstance)) {
        return otherInstance;
      }
    }
    return null;
  }

  public final Set<AttributeInstance> getDependencies(final AttributeInstance from) {
    if (this.dependencies.containsKey(from)) {
      return Collections.unmodifiableSet(this.dependencies.get(from));
    } else {
      return Collections.unmodifiableSet(new LinkedHashSet<AttributeInstance>());
    }
  }

  public final void computeTransitiveClosure() {
    // create adjacency matrix
    final int numberOfNodes = this.attributes.size();
    final boolean[][] adjacencyMatrix = new boolean[numberOfNodes][numberOfNodes];

    // add edges to adjacency matrix
    {
      for (final AttributeInstance from : this.dependencies.keySet()) {
        final int fromIndex = this.attributeIndexes.get(from);

        final Set<AttributeInstance> tos = this.dependencies.get(from);
        for (final AttributeInstance to : tos) {
          final int toIndex = this.attributeIndexes.get(to);
          adjacencyMatrix[fromIndex][toIndex] = true;
        }
      }
    }

    // Warshall's algorithm to compute the transitive closure
    {
      for (int k = 0; k < numberOfNodes; ++k) {
        for (int i = 0; i < numberOfNodes; ++i) {
          for (int j = 0; j < numberOfNodes; ++j) {
            adjacencyMatrix[i][j] |= (adjacencyMatrix[i][k] && adjacencyMatrix[k][j]);
          }
        }
      }
    }

    // add new edges to dependency graph
    {
      int fromIndex = 0;
      for (final AttributeInstance from : this.attributes) {
        int toIndex = 0;
        for (final AttributeInstance to : this.attributes) {
          if (adjacencyMatrix[fromIndex][toIndex]) {
            this.addDependency(from, to);
          }

          ++toIndex;
        }

        ++fromIndex;
      }
    }
  }

  public final boolean hasSelfDependency() {
    for (final AttributeInstance from : this.dependencies.keySet()) {
      final Set<AttributeInstance> tos = this.dependencies.get(from);
      if (tos.contains(from)) {
        if (DEBUG) {
          System.err.format("%s:%s (%s)\n",
              from.attributeSymbol.getContainingClass().getName(),
              from.attributeSymbol.getName(),
              from.childSymbol.getName());
        }

        return true;
      }
    }

    return false;
  }

  public final void printDot() {
    System.out.println("digraph G {");

    int index = 0;
    final Map<AttributeInstance, String> attributeIDs = new HashMap<>();
    for (final AttributeInstance attribute : this.attributes) {
      final String attributeID = String.format("attr_%d", index);
      attributeIDs.put(attribute, attributeID);

      final AttributeSymbol symbol = attribute.attributeSymbol;
      final String childName = attribute.childSymbol.getName();
      final String className = symbol.getContainingClass().getName();
      final String attributeName = symbol.getName();

      System.out.format("  %s [label=\"%s:%s (%s)\"];\n",
          attributeID, className, attributeName, childName);

      ++index;
    }

    for (final AttributeInstance attribute : this.attributes) {
      if (!this.dependencies.containsKey(attribute)) {
        continue;
      }

      final String sourceID = attributeIDs.get(attribute);

      for (final AttributeInstance dependency : this.dependencies.get(attribute)) {
        final String targetID = attributeIDs.get(dependency);
        System.out.format("  %s -> %s;\n", sourceID, targetID);
      }
    }

    System.out.println("}");
  }

}
