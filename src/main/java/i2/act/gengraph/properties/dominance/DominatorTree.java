package i2.act.gengraph.properties.dominance;

import i2.act.errors.RPGException;
import i2.act.gengraph.GeneratorGraph;
import i2.act.gengraph.GeneratorGraphNode;
import i2.act.gengraph.GeneratorGraphNode.ClassNode;
import i2.act.gengraph.GeneratorGraphNode.ProductionNode;
import i2.act.util.FileUtil;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DominatorTree {

  public static final class DominatorTreeNode {

    private static int idCounter;
    public final int id; // used in Dot output

    private final GeneratorGraphNode node;

    private final DominatorTreeNode parent;
    private final List<DominatorTreeNode> children;

    private DominatorTreeNode(final GeneratorGraphNode node, final DominatorTreeNode parent) {
      this.id = idCounter++;

      this.node = node;

      this.parent = parent;
      this.children = new ArrayList<DominatorTreeNode>();
    }

    public final GeneratorGraphNode getNode() {
      return this.node;
    }

    public final DominatorTreeNode getParent() {
      return this.parent;
    }

    private final void addChild(final DominatorTreeNode node) {
      this.children.add(node);
    }

    public final List<DominatorTreeNode> getChildren() {
      return Collections.unmodifiableList(this.children);
    }

  }


  private final DominatorTreeNode rootNode;
  private final Map<GeneratorGraphNode, DominatorTreeNode> nodes;

  public DominatorTree(final GeneratorGraphNode rootNode) {
    this.rootNode = new DominatorTreeNode(rootNode, null);

    this.nodes = new LinkedHashMap<GeneratorGraphNode, DominatorTreeNode>();
    this.nodes.put(rootNode, this.rootNode);
  }

  public final DominatorTreeNode getRootNode() {
    return this.rootNode;
  }

  public final boolean hasNodeFor(final GeneratorGraphNode node) {
    return this.nodes.containsKey(node);
  }

  public final DominatorTreeNode getNodeFor(final GeneratorGraphNode node) {
    if (!hasNodeFor(node)) {
      throw new RPGException(String.format("dominator tree does not contain node %s", node));
    }

    return this.nodes.get(node);
  }

  public final void addChild(final GeneratorGraphNode parent, final GeneratorGraphNode child) {
    if (this.nodes.containsKey(child)) {
      throw new RPGException(String.format("dominator tree already contains node %s", child));
    }

    final DominatorTreeNode parentNode = getNodeFor(parent);
    assert (parentNode != null);

    final DominatorTreeNode childNode = new DominatorTreeNode(child, parentNode);
    parentNode.addChild(childNode);

    this.nodes.put(child, childNode);
  }

  public final boolean dominates(final GeneratorGraphNode dominator,
      final GeneratorGraphNode dominated) {
    final DominatorTreeNode dominatorNode = getNodeFor(dominator);
    final DominatorTreeNode dominatedNode = getNodeFor(dominated);

    DominatorTreeNode current = dominatedNode;

    while (current != null) {
      if (current == dominatorNode) {
        return true;
      }

      current = current.getParent();
    }
    
    return false;
  }

  public final List<GeneratorGraphNode> getAllDominators(final GeneratorGraphNode dominated) {
    final List<GeneratorGraphNode> dominators = new ArrayList<>();

    DominatorTreeNode currentNode = getNodeFor(dominated);
    while (currentNode != null) {
      dominators.add(currentNode.getNode());
      currentNode = currentNode.getParent();
    }

    return dominators;
  }

  public final GeneratorGraphNode getImmediateDominator(final GeneratorGraphNode dominated) {
    final DominatorTreeNode dominatedNode = getNodeFor(dominated);

    if (dominatedNode.getParent() == null) {
      return dominated;
    } else {
      return dominatedNode.getParent().getNode();
    }
  }

  public final ClassNode getImmediateClassDominator(final GeneratorGraphNode dominated) {
    DominatorTreeNode treeNode = getNodeFor(dominated).getParent();

    while (treeNode != null && !(treeNode.getNode() instanceof ClassNode)) {
      treeNode = treeNode.getParent();
    }

    if (treeNode == null) {
      assert (this.rootNode.getNode() instanceof ClassNode);
      return (ClassNode) this.rootNode.getNode();
    } else {
      return (ClassNode) treeNode.getNode();
    }
  }

  public final void printAsDot() {
    printAsDot(new BufferedWriter(new OutputStreamWriter(System.out)));
  }

  public final void printAsDot(final BufferedWriter writer) {
    FileUtil.write("digraph {\n", writer);

    printAsDot(this.rootNode, writer);

    FileUtil.write("}\n", writer);

    FileUtil.flushWriter(writer);
  }

  private final void printAsDot(final DominatorTreeNode treeNode, final BufferedWriter writer) {
    final GeneratorGraphNode node = treeNode.getNode();

    final String style;
    {
      if (node instanceof ClassNode) {
        if (((ClassNode) node).isGuardClass()) {
          style =
              GeneratorGraph.DOT_STYLE_CLASS_NODES + "," + GeneratorGraph.DOT_STYLE_GUARDED_NODES;
        } else if (((ClassNode) node).isUnitClass()) {
          style =
              GeneratorGraph.DOT_STYLE_CLASS_NODES + "," + GeneratorGraph.DOT_STYLE_UNIT_NODES;
        } else {
          style = GeneratorGraph.DOT_STYLE_CLASS_NODES;
        }
      } else {
        assert (node instanceof ProductionNode);
        style = GeneratorGraph.DOT_STYLE_PRODUCTION_NODES;
      }
    }

    FileUtil.write(
        String.format("  n%d [label=\"%s\", %s];\n", treeNode.id, node.getName(), style), writer);

    for (final DominatorTreeNode childTreeNode : treeNode.getChildren()) {
      printAsDot(childTreeNode, writer);
      FileUtil.write(String.format("  n%d -> n%d;\n", treeNode.id, childTreeNode.id), writer);
    }
  }

}
