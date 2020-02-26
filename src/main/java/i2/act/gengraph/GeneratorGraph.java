package i2.act.gengraph;

import i2.act.fuzzer.Specification;
import i2.act.gengraph.GeneratorGraphNode.ClassNode;
import i2.act.gengraph.GeneratorGraphNode.ProductionNode;
import i2.act.lala.ast.ClassDeclaration;
import i2.act.lala.ast.LaLaSpecification;
import i2.act.util.FileUtil;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class GeneratorGraph implements Iterable<GeneratorGraphNode> {

  public static final String DOT_STYLE_CLASS_NODES =
      "shape=box, style=filled, fillcolor=lightsalmon";
  public static final String DOT_STYLE_GUARDED_NODES =
      "fillcolor=firebrick3";
  public static final String DOT_STYLE_UNIT_NODES =
      "fillcolor=orangered";
  public static final String DOT_STYLE_PRODUCTION_NODES =
      "shape=ellipse, style=filled, fillcolor=lightgoldenrod1";

  private final ClassNode rootNode;

  private final List<GeneratorGraphNode> graphNodes;
  
  private final List<ProductionNode> productionNodes;
  private final List<ClassNode> classNodes;

  private final Map<GeneratorGraphNode, Integer> nodeIDs;
  private int idCounter;
  
  private GeneratorGraph(final ClassNode rootNode) {
    this.rootNode = rootNode;

    this.graphNodes = new ArrayList<GeneratorGraphNode>();
    this.productionNodes = new ArrayList<ProductionNode>();
    this.classNodes = new ArrayList<ClassNode>();

    this.nodeIDs = new HashMap<GeneratorGraphNode, Integer>();
    this.idCounter = 0;
  }

  private GeneratorGraph(final Specification specification) {
    this(specification.getRootClass());

    gatherGraphNodesFromClasses(specification.getClasses());
  }

  private GeneratorGraph(final LaLaSpecification specification) {
    this(specification.getRootClassDeclaration());

    gatherGraphNodesFromClassDeclarations(specification.getClassDeclarations());
  }

  public static final GeneratorGraph fromSpecification(final Specification specification) {
    return new GeneratorGraph(specification);
  }

  public static final GeneratorGraph fromAST(final LaLaSpecification specification) {
    return new GeneratorGraph(specification);
  }

  // -----

  private final void gatherGraphNodesFromClasses(final List<i2.act.fuzzer.Class> classes) {
    for (final i2.act.fuzzer.Class _class : classes) {
      gatherGraphNodes(_class);
    }
  }

  private final void gatherGraphNodesFromClassDeclarations(final List<ClassDeclaration> classes) {
    for (final ClassDeclaration classDeclaration : classes) {
      gatherGraphNodes(classDeclaration);
    }
  }

  private final void gatherGraphNodes(final ClassNode classNode) {
    this.nodeIDs.put(classNode, this.idCounter++);

    this.graphNodes.add(classNode);
    this.classNodes.add(classNode);

    for (final ProductionNode production : classNode.getProductionNodes()) {
      this.graphNodes.add(production);
      this.productionNodes.add(production);

      this.nodeIDs.put(production, this.idCounter++);
    }
  }

  public final ClassNode getRootNode() {
    return this.rootNode;
  }

  public final Collection<GeneratorGraphNode> getGraphNodes() {
    return Collections.unmodifiableList(this.graphNodes);
  }

  public final Collection<ProductionNode> getProductionNodes() {
    return Collections.unmodifiableList(this.productionNodes);
  }

  public final Collection<ClassNode> getClassNodes() {
    return Collections.unmodifiableList(this.classNodes);
  }

  @Override
  public final Iterator<GeneratorGraphNode> iterator() {
    return this.graphNodes.iterator();
  }

  public final void printMinHeights() {
    printMinHeights(new BufferedWriter(new OutputStreamWriter(System.out)));
  }

  public final void printMinHeights(final BufferedWriter writer) {
    for (final ClassNode classNode : this.classNodes) {
      FileUtil.write(String.format(
          "%-20s: %3d\n", classNode.getName(), classNode.getMinHeight()),
          writer);

      for (final ProductionNode productionNode : classNode.getProductionNodes()) {
        FileUtil.write(String.format(
            "  %-18s: %3d\n", productionNode.getName(), productionNode.getMinHeight()),
            writer);
      }
    }

    FileUtil.flushWriter(writer);
  }

  private final int getNodeId(final GeneratorGraphNode node) {
    assert (this.nodeIDs.containsKey(node));
    return this.nodeIDs.get(node);
  }

  private final String getNodeName(final GeneratorGraphNode node) {
    return String.format("node_%d", getNodeId(node));
  }

  private final void writeNode(final GeneratorGraphNode node, final String label,
      final BufferedWriter writer) {
    final String nodeName = getNodeName(node);

    final String format;
    {
      if ((node instanceof ClassNode) && ((ClassNode) node).isGuardClass()) {
        format = ", " + DOT_STYLE_GUARDED_NODES;
      } else if ((node instanceof ClassNode) && ((ClassNode) node).isUnitClass()) {
        format = ", " + DOT_STYLE_UNIT_NODES;
      } else {
        format = "";
      }
    }

    final String nodeRepresentation =
        String.format("    %s [label=\"%s\"%s,];\n", nodeName, label, format);
    FileUtil.write(nodeRepresentation, writer);
  }

  private final void writeEdge(final GeneratorGraphNode from, final GeneratorGraphNode to,
      final String label, final boolean dashed, final BufferedWriter writer) {
    final String nodeNameFrom = getNodeName(from);
    final String nodeNameTo = getNodeName(to);

    final String edgeRepresentation =
        String.format("    %s -> %s [label=\"%s\"%s];\n", nodeNameFrom, nodeNameTo, label,
            (dashed ? ", style=dashed" : ""));
    FileUtil.write(edgeRepresentation, writer);
  }

  public final void printAsDot() {
    printAsDot(new BufferedWriter(new OutputStreamWriter(System.out)));
  }

  @SuppressWarnings("unchecked")
  public final void printAsDot(final BufferedWriter writer) {
    FileUtil.write("digraph {\n", writer);

    // class nodes
    {
      FileUtil.write("  subgraph {\n", writer);
      FileUtil.write(String.format("    node [%s];\n", DOT_STYLE_CLASS_NODES), writer);

      for (final ClassNode classNode : this.classNodes) {
        final String label = classNode.getName();
        writeNode(classNode, label, writer);
      }

      FileUtil.write("  }\n", writer);
    }


    // production nodes
    {
      FileUtil.write("  subgraph {\n", writer);
      FileUtil.write(String.format("    node [%s];\n", DOT_STYLE_PRODUCTION_NODES), writer);

      for (final ClassNode classNode : this.classNodes) {
        if (!classNode.isLiteralNode()) {
          for (final ProductionNode productionNode : classNode.getProductionNodes()) {
            final String label = productionNode.getName();
            writeNode(productionNode, label, writer);

            int childIndex = 0;
            for (final ClassNode childClass : (List<ClassNode>) productionNode.successors()) {
              writeEdge(productionNode, childClass, String.valueOf(childIndex++), true, writer);
            }

            writeEdge(classNode, productionNode, "", false, writer);
          }
        }
      }

      FileUtil.write("  }\n", writer);
    }

    FileUtil.write("}\n", writer);

    FileUtil.flushWriter(writer);
  }

}
