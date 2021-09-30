package i2.act.lala.ast.visitors;

import i2.act.errors.specification.attributes.CyclicAttributeDependencyException;
import i2.act.lala.ast.*;
import i2.act.lala.ast.AttributeModifier.AttributeModifierKind;
import i2.act.lala.semantics.attributes.AttributeInstance;
import i2.act.lala.semantics.attributes.DependencyGraph;
import i2.act.lala.semantics.attributes.ISSIGraph;
import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.lala.semantics.symbols.ChildSymbol;
import i2.act.lala.semantics.symbols.ClassSymbol;
import i2.act.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StrongNonCyclicityCheck extends BaseLaLaSpecificationVisitor<Void, Void> {

  private static final boolean DEBUG = false;

  private final Map<ProductionDeclaration, DependencyGraph> dependencyGraphs;
  private final Map<ClassSymbol, ISSIGraph> issiGraphs;

  private ClassDeclaration currentClassDeclaration;

  private final boolean printISSI;
  private final boolean printDependencyGraphs;

  private final Set<ClassDeclaration> unitClasses;

  private StrongNonCyclicityCheck(final boolean printISSI, final boolean printDependencyGraphs,
      final Set<ClassDeclaration> unitClasses) {
    this.printISSI = printISSI;
    this.printDependencyGraphs = printDependencyGraphs;
    this.unitClasses = unitClasses;

    this.dependencyGraphs = new HashMap<ProductionDeclaration, DependencyGraph>();
    this.issiGraphs = new HashMap<ClassSymbol, ISSIGraph>();
  }

  public static final void analyze(final LaLaSpecification specification,
      final boolean printISSI, final boolean printDependencyGraphs) {
    analyze(specification, printISSI, printDependencyGraphs, null);
  }

  public static final void analyze(final LaLaSpecification specification,
      final boolean printISSI, final boolean printDependencyGraphs,
      final Set<ClassDeclaration> unitClasses) {
    specification.accept(
        new StrongNonCyclicityCheck(printISSI, printDependencyGraphs, unitClasses), null);
  }

  private final void addAttributeInstances(final ChildSymbol childSymbol,
      final DependencyGraph dependencyGraph) {
    final ClassSymbol classSymbol = childSymbol.getType();

    final List<AttributeSymbol> attributes = classSymbol.getAttributes().gatherSymbols();
    for (final AttributeSymbol attribute : attributes) {
      final AttributeInstance attributeInstance = new AttributeInstance(childSymbol, attribute);
      dependencyGraph.addAttribute(attributeInstance);
    }
  }

  private final AttributeInstance getAttributeInstance(final AttributeAccess attributeAccess) {
    final ChildSymbol targetChild = (ChildSymbol) attributeAccess.getTargetName().getSymbol();
    assert (targetChild != null);

    final AttributeDeclaration attributeDeclaration =
        (AttributeDeclaration) attributeAccess.getAttributeName().getSymbol().getDeclaration();
    assert (attributeDeclaration != null);

    final AttributeSymbol attributeSymbol = attributeDeclaration.getSymbol();
    assert (attributeSymbol != null);

    final AttributeInstance attributeInstance =
        new AttributeInstance(targetChild, attributeSymbol);
        
    return attributeInstance;
  }

  private final DependencyGraph cloneDependencyGraph(
      final ProductionDeclaration productionDeclaration) {
    final DependencyGraph dependencyGraph = this.dependencyGraphs.get(productionDeclaration);
    assert (dependencyGraph != null);
    
    return dependencyGraph.clone();
  }

  private final void addEdgesFromISSIGraphs(final DependencyGraph dependencyGraph,
      final ClassSymbol classSymbol) {
    final ISSIGraph issiGraph = this.issiGraphs.get(classSymbol);
    assert (issiGraph != null);

    for (final AttributeSymbol from : issiGraph.getAttributes()) {
      for (final AttributeSymbol to : issiGraph.getDependencies(from)) {
        dependencyGraph.addAllDependencies(from, to);
      }
    }
  }

  private final void addEdgesFromISSIGraphs(final DependencyGraph dependencyGraph,
      final ProductionDeclaration productionDeclaration) {
    // handle 'this' ('left hand side' of production)
    {
      final ChildSymbol thisSymbol = productionDeclaration.getThisSymbol();
      assert (thisSymbol != null);

      final ClassSymbol classSymbol = thisSymbol.getType();
      assert (classSymbol != null);

      addEdgesFromISSIGraphs(dependencyGraph, classSymbol);
    }

    // handle children ('right hand side' of production)
    {
      final List<ChildDeclaration> childDeclarations = productionDeclaration.getChildDeclarations();
      for (final ChildDeclaration childDeclaration : childDeclarations) {
        final ChildSymbol childSymbol = childDeclaration.getSymbol();
        assert (childSymbol != null);

        final ClassSymbol classSymbol = childSymbol.getType();
        assert (classSymbol != null);

        addEdgesFromISSIGraphs(dependencyGraph, classSymbol);
      }
    }
  }

  private final boolean addEdgesFromDependencyGraph(final DependencyGraph dependencyGraph,
      final ChildSymbol childSymbol) {
    boolean change = false;

    final ClassSymbol classSymbol = childSymbol.getType();
    assert (classSymbol != null);

    final ISSIGraph issiGraph = this.issiGraphs.get(classSymbol);
    assert (issiGraph != null);

    for (final AttributeInstance from : dependencyGraph.getAttributes()) {
      if (from.childSymbol == childSymbol) {
        final AttributeSymbol attributeSymbolFrom = from.attributeSymbol;

        for (final AttributeInstance to : dependencyGraph.getDependencies(from)) {
          if (to.childSymbol == childSymbol) {
            final AttributeSymbol attributeSymbolTo = to.attributeSymbol;
            if (!issiGraph.hasDependency(attributeSymbolFrom, attributeSymbolTo)) {
              issiGraph.addDependency(attributeSymbolFrom, attributeSymbolTo);
              change = true;
            }
          }
        }
      }
    }

    return change;
  }

  private final boolean addEdgesFromDependencyGraph(final DependencyGraph dependencyGraph,
      final ProductionDeclaration productionDeclaration) {
    boolean change = false;

    // handle 'this' ('left hand side' of production)
    {
      final ChildSymbol thisSymbol = productionDeclaration.getThisSymbol();
      assert (thisSymbol != null);

      change |= addEdgesFromDependencyGraph(dependencyGraph, thisSymbol);
    }

    // handle children ('right hand side' of production)
    {
      final List<ChildDeclaration> childDeclarations = productionDeclaration.getChildDeclarations();
      for (final ChildDeclaration childDeclaration : childDeclarations) {
        final ChildSymbol childSymbol = childDeclaration.getSymbol();
        assert (childSymbol != null);

        change |= addEdgesFromDependencyGraph(dependencyGraph, childSymbol);
      }
    }

    return change;
  }

  @Override
  public final Void visit(final LaLaSpecification specification, final Void parameter) {
    // visit language specification to create the dependency graphs for each production
    // as well as the initial IS-SI graph for each class
    super.visit(specification, parameter);

    // compute transitive closure of attribute dependencies until no more changes occur
    boolean change = true;
    while (change) {
      change = false;

      final List<ClassDeclaration> classDeclarations = specification.getClassDeclarations();
      for (final ClassDeclaration classDeclaration : classDeclarations) {
        if (classDeclaration instanceof ProductionClassDeclaration) {
          final ProductionClassDeclaration productionClassDeclaration =
              (ProductionClassDeclaration) classDeclaration;

          final List<ProductionDeclaration> productionDeclarations =
              productionClassDeclaration.getProductionDeclarations();
          for (final ProductionDeclaration productionDeclaration : productionDeclarations) {
            // clone dependency graph of production
            final DependencyGraph dependencyGraphClone =
                cloneDependencyGraph(productionDeclaration);

            // add edges from IS-SI graphs
            addEdgesFromISSIGraphs(dependencyGraphClone, productionDeclaration);

            // compute transitive closure and check that no cycles exist
            dependencyGraphClone.computeTransitiveClosure();
            if (dependencyGraphClone.hasSelfDependency()) {
              if (DEBUG) {
                final DependencyGraph simpleEdgesClone =
                    cloneDependencyGraph(productionDeclaration);
                addEdgesFromISSIGraphs(simpleEdgesClone, productionDeclaration);
                simpleEdgesClone.printDot();
              }

              throw new CyclicAttributeDependencyException(productionDeclaration);
            }

            // update IS-SI graphs with newly detected edges
            change |= addEdgesFromDependencyGraph(dependencyGraphClone, productionDeclaration);
          }
        }
      }
    }

    if (this.printISSI) {
      printISSI();
    }

    if (this.printDependencyGraphs) {
      printDependencyGraphs();
    }

    // compute the attributes that the guard attributes depend on
    computeGuardDependencies();

    // compute the visitation order based on attribute dependencies
    computeVisitationOrder();

    return null;
  }

  @Override
  public final Void visit(final ProductionClassDeclaration classDeclaration, final Void parameter) {
    this.currentClassDeclaration = classDeclaration;

    // visit children to compute dependency graph for each production
    super.visit(classDeclaration, parameter);

    this.currentClassDeclaration = null;

    final ClassSymbol classSymbol = classDeclaration.getSymbol();
    assert (classSymbol != null);

    final ISSIGraph issiGraph = new ISSIGraph(classSymbol);

    final List<AttributeDeclaration> attributeDeclarations =
        classDeclaration.getAttributeDeclarations();
    for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
      final AttributeSymbol attributeSymbol = attributeDeclaration.getSymbol();
      assert (attributeSymbol != null);

      issiGraph.addAttribute(attributeSymbol);
    }

    this.issiGraphs.put(classSymbol, issiGraph);

    return null;
  }

  @Override
  public final Void visit(final LiteralClassDeclaration classDeclaration, final Void parameter) {
    this.currentClassDeclaration = classDeclaration;

    super.visit(classDeclaration, parameter);

    this.currentClassDeclaration = null;

    final ClassSymbol classSymbol = classDeclaration.getSymbol();
    assert (classSymbol != null);

    final ISSIGraph issiGraph = new ISSIGraph(classSymbol);

    final List<AttributeDeclaration> attributeDeclarations =
        classDeclaration.getAttributeDeclarations();
    for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
      final AttributeSymbol attributeSymbol = attributeDeclaration.getSymbol();
      assert (attributeSymbol != null);

      issiGraph.addAttribute(attributeSymbol);
    }

    this.issiGraphs.put(classSymbol, issiGraph);

    return null;
  }

  @Override
  public final Void visit(final TreeProductionDeclaration productionDeclaration,
      final Void parameter) {
    return visit((ProductionDeclaration) productionDeclaration, parameter);
  }

  @Override
  public final Void visit(final GeneratorProductionDeclaration productionDeclaration,
      final Void parameter) {
    return visit((ProductionDeclaration) productionDeclaration, parameter);
  }

  private final Void visit(final ProductionDeclaration productionDeclaration,
      final Void parameter) {
    assert (this.currentClassDeclaration != null);

    final DependencyGraph dependencyGraph = new DependencyGraph();

    // add nodes to dependency graph (i.e., attribute instances)
    {
      addAttributeInstances(productionDeclaration.getThisSymbol(), dependencyGraph);

      final List<ChildDeclaration> childDeclarations = productionDeclaration.getChildDeclarations();
      for (final ChildDeclaration childDeclaration : childDeclarations) {
        final ChildSymbol childSymbol = childDeclaration.getSymbol();
        assert (childSymbol != null);

        addAttributeInstances(childSymbol, dependencyGraph);
      }
    }

    // add edges to dependency graph (i.e., dependencies between attribute instances)
    {
      final List<AttributeEvaluationRule> attributeEvaluationRules =
          productionDeclaration.getAttributeEvaluationRules();
      for (final AttributeEvaluationRule attributeEvaluationRule : attributeEvaluationRules) {
        final AttributeAccess targetAttribute = attributeEvaluationRule.getTargetAttribute();
        final AttributeInstance targetAttributeInstance = getAttributeInstance(targetAttribute);

        final List<AttributeAccess> sourceAttributes =
            attributeEvaluationRule.gatherSourceAttributes();
        for (final AttributeAccess sourceAttribute : sourceAttributes) {
          final AttributeInstance sourceAttributeInstance = getAttributeInstance(sourceAttribute);
          dependencyGraph.addDependency(sourceAttributeInstance, targetAttributeInstance);
        }
      }
    }

    // add implicit edges from inherited attributes to guard attributes
    // and from guard attributes to synthesized attributes.
    // for classes with the @unit annotation, add implicit edges from all inherited attributes to
    // all synthesized attributes
    {
      final List<AttributeDeclaration> inheritedAttributes =
          getInheritedAttributes(this.currentClassDeclaration);
      final List<AttributeDeclaration> guardAttributes =
          getGuardAttributes(this.currentClassDeclaration);
      final List<AttributeDeclaration> synthesizedAttributes =
          getSynthesizedAttributes(this.currentClassDeclaration);

      for (final AttributeDeclaration guardAttribute : guardAttributes) {
        final AttributeInstance guardInstance = dependencyGraph.getAttribute(
            productionDeclaration.getThisSymbol(), guardAttribute.getSymbol());
        assert (guardInstance != null);

        for (final AttributeDeclaration inheritedAttribute : inheritedAttributes) {
          final AttributeInstance inheritedInstance = dependencyGraph.getAttribute(
              productionDeclaration.getThisSymbol(), inheritedAttribute.getSymbol());
          assert (inheritedInstance != null);

          dependencyGraph.addDependency(inheritedInstance, guardInstance);
        }

        for (final AttributeDeclaration synthesizedAttribute : synthesizedAttributes) {
          final AttributeInstance synthesizedInstance = dependencyGraph.getAttribute(
              productionDeclaration.getThisSymbol(), synthesizedAttribute.getSymbol());
          assert (synthesizedInstance != null);

          dependencyGraph.addDependency(guardInstance, synthesizedInstance);
        }
      }

      final boolean isUnitClass = isUnitClass(this.currentClassDeclaration);
      if (isUnitClass && guardAttributes.isEmpty()) {
        for (final AttributeDeclaration inheritedAttribute : inheritedAttributes) {
          final AttributeInstance inheritedInstance = dependencyGraph.getAttribute(
              productionDeclaration.getThisSymbol(), inheritedAttribute.getSymbol());
          assert (inheritedInstance != null);

          for (final AttributeDeclaration synthesizedAttribute : synthesizedAttributes) {
            final AttributeInstance synthesizedInstance = dependencyGraph.getAttribute(
                productionDeclaration.getThisSymbol(), synthesizedAttribute.getSymbol());
            assert (synthesizedInstance != null);

            dependencyGraph.addDependency(inheritedInstance, synthesizedInstance);
          }
        }
      }
    }

    this.dependencyGraphs.put(productionDeclaration, dependencyGraph);
    return null;
  }

  private final boolean isGuardClass(final ClassSymbol classSymbol) {
    return isGuardClass(classSymbol.getDeclaration());
  }

  private final boolean isGuardClass(final ClassDeclaration classDeclaration) {
    return classDeclaration.isGuardClass();
  }

  private final boolean isUnitClass(final ClassDeclaration classDeclaration) {
    // guard classes
    if (isGuardClass(classDeclaration)) {
      return true;
    }

    // classes marked explicitly as @unit
    if (classDeclaration.isUnitClass()) {
      return true;
    }

    // generator classes
    if ((classDeclaration instanceof ProductionClassDeclaration)
        && ((ProductionClassDeclaration) classDeclaration).isGeneratorClass()) {
      return true;
    }

    // additional unit classes
    if (this.unitClasses != null && this.unitClasses.contains(classDeclaration)) {
      return true;
    }

    return false;
  }

  private final List<AttributeDeclaration> getGuardAttributes(
      final ClassDeclaration classDeclaration) {
    return getAttributes(classDeclaration, AttributeModifierKind.MOD_GRD);
  }

  private final List<AttributeDeclaration> getInheritedAttributes(
      final ClassDeclaration classDeclaration) {
    return getAttributes(classDeclaration, AttributeModifierKind.MOD_INH);
  }

  private final List<AttributeDeclaration> getSynthesizedAttributes(
      final ClassDeclaration classDeclaration) {
    return getAttributes(classDeclaration, AttributeModifierKind.MOD_SYN);
  }

  private final List<AttributeDeclaration> getAttributes(final ClassDeclaration classDeclaration,
      final AttributeModifierKind kind) {
    final List<AttributeDeclaration> attributes = new ArrayList<>();

    final List<AttributeDeclaration> attributeDeclarations =
        classDeclaration.getAttributeDeclarations();
    for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
      final AttributeModifier attributeModifier = attributeDeclaration.getModifier();
      if (attributeModifier.getModifierKind() == kind) {
        attributes.add(attributeDeclaration);
      }
    }
    
    return attributes;
  }

  private final void printISSI() {
    System.out.println("digraph G {");

    System.out.println("  graph [fontname=\"Droid Sans Mono\", nodesep=2, ranksep=2];");
    System.out.println("  node [fontname=\"Droid Sans Mono\", shape=box];");
    System.out.println("  compound=true;");

    final Map<AttributeSymbol, String> attributeNames = new HashMap<>();

    int classIndex = 0;
    int attributeIndex = 0;

    for (final ClassSymbol classSymbol : this.issiGraphs.keySet()) {
      final String className = classSymbol.getName();

      final ISSIGraph issiGraph = this.issiGraphs.get(classSymbol);

      System.out.format("  subgraph cluster%d {\n", classIndex);
      System.out.format("    graph [label=\"%s\", labelloc=t, penwidth=3];\n", className);
      System.out.format("    node [penwidth=3];\n");
      System.out.format("    edge [penwidth=3];\n");

      for (final AttributeSymbol attribute : issiGraph.getAttributes()) {
        final String attributeName = String.format("attr%d", attributeIndex);
        attributeNames.put(attribute, attributeName);

        final String attributeLabel = String.format("%s:%s", className, attribute.getName());
        System.out.format("    %s [label=\"%s\"];\n", attributeName, attributeLabel);

        ++attributeIndex;
      }

      for (final AttributeSymbol fromAttribute : issiGraph.getAttributes()) {
        final String fromAttributeName = attributeNames.get(fromAttribute);
        assert (fromAttributeName != null);

        for (final AttributeSymbol toAttribute : issiGraph.getDependencies(fromAttribute)) {
          final String toAttributeName = attributeNames.get(toAttribute);
          assert (toAttributeName != null);

          System.out.format("    %s -> %s;\n", fromAttributeName, toAttributeName);
        }
      }

      System.out.println("  }");

      ++classIndex;
    }

    final Set<Pair<AttributeSymbol, AttributeSymbol>> visited = new HashSet<>();

    for (final ProductionDeclaration production : this.dependencyGraphs.keySet()) {
      final DependencyGraph dependencyGraph = this.dependencyGraphs.get(production);

      for (final AttributeInstance from : dependencyGraph.getAttributes()) {
        final AttributeSymbol fromSymbol = from.attributeSymbol;
        final String fromName = attributeNames.get(fromSymbol);

        for (final AttributeInstance to : dependencyGraph.getDependencies(from)) {
          final AttributeSymbol toSymbol = to.attributeSymbol;

          if (fromSymbol.getContainingClass() == toSymbol.getContainingClass()) {
            continue;
          }

          final Pair<AttributeSymbol, AttributeSymbol> edge = new Pair<>(fromSymbol, toSymbol);

          if (visited.contains(edge)) {
            continue;
          }
          visited.add(edge);

          final String toName = attributeNames.get(toSymbol);
          System.out.format("  %s -> %s [penwidth=1, style=dashed];\n", fromName, toName);
        }
      }
    }

    System.out.println("}");
  }

  private final void printDependencyGraphs() {
    System.out.println("digraph G {");

    System.out.println("  node [fontname=\"Droid Sans Mono\"];");
    System.out.println("  compound=true;");

    int clusterIndex = 0;
    int attributeIndex = 0;

    for (final ProductionDeclaration production : this.dependencyGraphs.keySet()) {
      final DependencyGraph dependencyGraph = this.dependencyGraphs.get(production);

      System.out.format("  subgraph cluster%d {\n", ++clusterIndex);
      System.out.format("    label=\"%s\";\n", production.getName());

      final Map<AttributeInstance, String> attributeNames = new HashMap<>();

      for (final AttributeInstance attribute : dependencyGraph.getAttributes()) {
        final String attributeName = String.format("attr%d", attributeIndex);
        attributeNames.put(attribute, attributeName);

        final String attributeLabel = attribute.toString();
        System.out.format("    %s [label=\"%s\"];\n", attributeName, attributeLabel);

        ++attributeIndex;
      }

      for (final AttributeInstance from : dependencyGraph.getAttributes()) {
        final String fromName = attributeNames.get(from);
        assert (fromName != null);

        for (final AttributeInstance to : dependencyGraph.getDependencies(from)) {
          final String toName = attributeNames.get(to);
          assert (toName != null);

          System.out.format("    %s -> %s;\n", fromName, toName);
        }
      }

      System.out.println("  }\n");
    }

    System.out.println("}");
  }

  private final void computeGuardDependencies() {
    final Map<AttributeSymbol, String> attributeNames = new HashMap<>();

    final Map<AttributeSymbol, Integer> attributeIndexes = new HashMap<>();
    final Map<Integer, AttributeSymbol> attributeIndexesInverted = new HashMap<>();

    final List<AttributeSymbol> guardAttributes = new ArrayList<>();

    // assign indexes
    final int numberOfNodes;
    {
      int attributeIndex = 0;

      for (final ClassSymbol classSymbol : this.issiGraphs.keySet()) {
        final String className = classSymbol.getName();
        final ISSIGraph issiGraph = this.issiGraphs.get(classSymbol);

        for (final AttributeSymbol attribute : issiGraph.getAttributes()) {
          attributeIndexes.put(attribute, attributeIndex);
          attributeIndexesInverted.put(attributeIndex, attribute);
          ++attributeIndex;

          if (DEBUG) {
            final String attributeName = String.format("%s:%s", className, attribute.getName());
            attributeNames.put(attribute, attributeName);
          }

          if (attribute.isGuardAttribute()) {
            guardAttributes.add(attribute);
          }
        }
      }

      numberOfNodes = attributeIndex;
    }

    // compute adjacency matrix
    final boolean[][] adjacencyMatrix = new boolean[numberOfNodes][numberOfNodes];
    {
      for (final ProductionDeclaration production : this.dependencyGraphs.keySet()) {
        final DependencyGraph dependencyGraph = this.dependencyGraphs.get(production);

        for (final AttributeInstance from : dependencyGraph.getAttributes()) {
          final int fromIndex = attributeIndexes.get(from.attributeSymbol);
          for (final AttributeInstance to : dependencyGraph.getDependencies(from)) {
            final int toIndex = attributeIndexes.get(to.attributeSymbol);
            adjacencyMatrix[fromIndex][toIndex] = true;
          }
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

    for (final AttributeSymbol guardAttribute : guardAttributes) {
      final Set<AttributeSymbol> sourceAttributes = new HashSet<>();

      final int guardIndex = attributeIndexes.get(guardAttribute);

      for (int attributeIndex = 0; attributeIndex < numberOfNodes; ++attributeIndex) {
        if (adjacencyMatrix[attributeIndex][guardIndex]) {
          final AttributeSymbol sourceAttribute = attributeIndexesInverted.get(attributeIndex);

          if (DEBUG) {
            sourceAttributes.add(sourceAttribute);
          }

          if (sourceAttribute.isInheritedAttribute()
              && (sourceAttribute.getContainingClass() == guardAttribute.getContainingClass())) {
            guardAttribute.addDependency(sourceAttribute);
          }
        }
      }

      if (DEBUG) {
        System.out.format("dependencies of %s:", attributeNames.get(guardAttribute));
        for (final AttributeSymbol sourceAttribute : sourceAttributes) {
          System.out.format(" %s", attributeNames.get(sourceAttribute));
        }
        System.out.println();
      }
    }
  }

  private final void computeVisitationOrder() {
    final Map<ProductionDeclaration, Map<ChildSymbol, Set<ChildSymbol>>> dependencies =
        new HashMap<>();

    for (final ProductionDeclaration production : this.dependencyGraphs.keySet()) {
      if (production instanceof TreeProductionDeclaration) {
        final ChildSymbol thisSymbol = production.getThisSymbol();

        final DependencyGraph dependencyGraph = this.dependencyGraphs.get(production);

        // use a LinkedHashMap for a deterministic ordering
        final Map<ChildSymbol, Set<ChildSymbol>> childDependencies = new LinkedHashMap<>();
        {
          for (final ChildDeclaration childDeclaration : production.getChildDeclarations()) {
            final ChildSymbol childSymbol = childDeclaration.getSymbol();
            // use a LinkedHashSet for a deterministic ordering
            childDependencies.put(childSymbol, new LinkedHashSet<ChildSymbol>());
          }
        }
        dependencies.put(production, childDependencies);

        // compute dependencies between children
        for (final AttributeInstance from : dependencyGraph.getAttributes()) {
          final ChildSymbol fromSymbol = from.childSymbol;
          if (fromSymbol == thisSymbol) {
            continue;
          }

          for (final AttributeInstance to : dependencyGraph.getDependencies(from)) {
            final ChildSymbol toSymbol = to.childSymbol;
            if (toSymbol == thisSymbol) {
              continue;
            }

            childDependencies.get(fromSymbol).add(toSymbol);
          }
        }

        // compute the child visitation order
        final List<Integer> childVisitationOrder =
            computeChildVisitationOrder((TreeProductionDeclaration) production, childDependencies);

        production.getSymbol().setChildVisitationOrder(childVisitationOrder);
      } else {
        // generator productions do not have any children
        production.getSymbol().setChildVisitationOrder(new ArrayList<Integer>());
      }
    }

    // XXX just for debugging...
    if (false) {
      System.out.println("digraph G {");

      System.out.println("  node [fontname=\"Droid Sans Mono\"];");
      System.out.println("  compound=true;");

      int clusterIndex = 0;
      int childIndex = 0;

      for (final ProductionDeclaration production : this.dependencyGraphs.keySet()) {
        final Map<ChildSymbol, Set<ChildSymbol>> childDependencies = dependencies.get(production);

        System.out.format("  subgraph cluster%d {\n", ++clusterIndex);
        System.out.format("    label=\"%s\";\n", production.getName());

        final Map<ChildSymbol, String> childNames = new HashMap<>();

        for (final ChildSymbol childSymbol : childDependencies.keySet()) {
          final String childName = String.format("ch%d", childIndex);
          childNames.put(childSymbol, childName);

          final String childLabel = childSymbol.getName();
          System.out.format("    %s [label=\"%s\"];\n", childName, childLabel);

          ++childIndex;
        }

        for (final ChildSymbol from : childDependencies.keySet()) {
          final String fromName = childNames.get(from);
          assert (fromName != null);

          for (final ChildSymbol to : childDependencies.get(from)) {
            final String toName = childNames.get(to);
            assert (toName != null);

            System.out.format("    %s -> %s;\n", fromName, toName);
          }
        }

        System.out.println("  }\n");
      }

      System.out.println("}");
    }
  }

  private final List<Integer> computeChildVisitationOrder(
      final TreeProductionDeclaration production,
      final Map<ChildSymbol, Set<ChildSymbol>> childDependencies) {
    final List<Integer> childVisitationOrder = new ArrayList<>();

    // sort children topologically (if possible)
    final List<ChildSymbol> sorted = sortChildren(childDependencies);

    final int numberOfChildren = sorted.size();
    for (int sortedIndex = 0; sortedIndex < numberOfChildren; ++sortedIndex) {
      final ChildSymbol child = sorted.get(sortedIndex);
      final int childIndex = production.getIndexOfChild(child.getDeclaration());
      assert (childIndex > -1);

      childVisitationOrder.add(childIndex);
    }

    return childVisitationOrder;
  }

  private final List<ChildSymbol> sortChildren(
      final Map<ChildSymbol, Set<ChildSymbol>> childDependencies) {
    final List<ChildSymbol> sorted = new ArrayList<>();

    // use a LinkedHashMap for a deterministic ordering
    final Map<ChildSymbol, Set<ChildSymbol>> copy = new LinkedHashMap<>(childDependencies);
    while (!copy.isEmpty()) {
      ChildSymbol nextChild = null;
      for (final ChildSymbol child : copy.keySet()) {
        if (copy.get(child).isEmpty()) {
          nextChild = child;
          break;
        }
      }

      if (nextChild != null) {
        sorted.add(nextChild);
        copy.remove(nextChild);

        for (final ChildSymbol child : copy.keySet()) {
          copy.get(child).remove(nextChild);
        }
      } else {
        // did not find a child without dependencies
        // -> add remaining children in arbitrary order (XXX could be improved if necessary)
        sorted.addAll(copy.keySet());
        copy.clear();
      }
    }

    return sorted;
  }

}
