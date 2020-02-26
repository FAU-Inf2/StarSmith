package i2.act.gengraph.properties;

import i2.act.fuzzer.Specification;
import i2.act.gengraph.GeneratorGraph;
import i2.act.gengraph.GeneratorGraphNode.ClassNode;
import i2.act.gengraph.GeneratorGraphNode.ProductionNode;
import i2.act.lala.ast.LaLaSpecification;

import java.util.HashSet;
import java.util.Set;

public final class RecursiveProductionsComputation {

  private RecursiveProductionsComputation() {
    // intentionally left blank
  }
  
  public static final void determineRecursiveProductions(final Specification specification) {
    determineRecursiveProductions(GeneratorGraph.fromSpecification(specification));
  }

  public static final void determineRecursiveProductions(final LaLaSpecification specification) {
    determineRecursiveProductions(GeneratorGraph.fromAST(specification));
  }

  public static final void determineRecursiveProductions(final GeneratorGraph generatorGraph) {
    for (final ClassNode _class : generatorGraph.getClassNodes()) {
      for (final ProductionNode production : _class.getProductionNodes()) {
        final Set<ClassNode> classes = new HashSet<>();
        gatherClassNodes(production, classes);

        if (classes.contains(_class)) {
          production.setIsRecursive(true);
        }
      }
    }
  }

  private static final void gatherClassNodes(final ProductionNode production,
      final Set<ClassNode> classes) {
    for (final ClassNode childClass : production.getChildClassNodes()) {
      if (childClass == production.ownClassNode() && childClass.isListClass()) {
        // lists are not 'recursive'
      } else {
        if (!classes.contains(childClass)) {
          classes.add(childClass);

          for (final ProductionNode childProduction : childClass.getProductionNodes()) {
            gatherClassNodes(childProduction, classes);
          }
        }
      }
    }
  }

}
