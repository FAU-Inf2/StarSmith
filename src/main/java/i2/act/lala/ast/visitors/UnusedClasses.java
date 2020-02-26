package i2.act.lala.ast.visitors;

import i2.act.gengraph.GeneratorGraph;
import i2.act.gengraph.GeneratorGraphNode.ClassNode;
import i2.act.gengraph.GeneratorGraphNode.ProductionNode;
import i2.act.lala.ast.*;

import java.util.HashSet;
import java.util.Set;

public final class UnusedClasses extends BaseLaLaSpecificationVisitor<Set<ClassNode>, Void> {

  private UnusedClasses() {
    // intentionally left blank
  }

  public static final void emitWarnings(final LaLaSpecification specification) {
    final GeneratorGraph generatorGraph = GeneratorGraph.fromAST(specification);

    final Set<ClassNode> reachableClasses = new HashSet<>();
    gatherReachableClasses(generatorGraph.getRootNode(), reachableClasses);

    (new UnusedClasses()).visit(specification, reachableClasses);
  }

  private static final void gatherReachableClasses(final ClassNode node,
      final Set<ClassNode> reachableNodes) {
    reachableNodes.add(node);

    for (final ProductionNode productionNode : node.getProductionNodes()) {
      for (final ClassNode childClassNode : productionNode.getChildClassNodes()) {
        if (!reachableNodes.contains(childClassNode)) {
          gatherReachableClasses(childClassNode, reachableNodes);
        }
      }
    }
  }

  @Override
  public final Void visit(final LiteralClassDeclaration classDeclaration,
      final Set<ClassNode> reachableClasses) {
    checkClass(classDeclaration, reachableClasses);
    return null;
  }

  @Override
  public final Void visit(final ProductionClassDeclaration classDeclaration,
      final Set<ClassNode> reachableClasses) {
    checkClass(classDeclaration, reachableClasses);
    return null;
  }

  private static final void checkClass(final ClassDeclaration classDeclaration,
      final Set<ClassNode> reachableClasses) {
    if (!reachableClasses.contains(classDeclaration)) {
      final String className = classDeclaration.getName();
      System.err.format("[i] class '%s' is not reachable from root class\n", className);
    }
  }

}
