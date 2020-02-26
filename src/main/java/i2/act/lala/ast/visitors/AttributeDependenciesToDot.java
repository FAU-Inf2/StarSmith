package i2.act.lala.ast.visitors;

import i2.act.lala.ast.*;
import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.util.FileUtil;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AttributeDependenciesToDot
    extends BaseLaLaSpecificationVisitor<BufferedWriter, Void> {

  private int idCounter;
  private final Map<AttributeSymbol, String> attributeNodes;

  public AttributeDependenciesToDot() {
    this.idCounter = 0;
    this.attributeNodes = new HashMap<AttributeSymbol, String>();
  }

  @Override
  public final Void visit(final LaLaSpecification node, final BufferedWriter writer) {
    FileUtil.write("digraph G {\n", writer);

    for (final ClassDeclaration classDeclaration : node.getClassDeclarations()) {
      final List<AttributeDeclaration> attributeDeclarations =
          classDeclaration.getAttributeDeclarations();

      final String className = classDeclaration.getName();

      for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
        final AttributeSymbol attribute = attributeDeclaration.getSymbol();
        assert (attribute != null);

        final String attributeName = attribute.getName();

        final String attributeId = String.format("a%d", ++this.idCounter);
        this.attributeNodes.put(attribute, attributeId);

        FileUtil.write(
            String.format("  %s [label=\"%s:%s\"];\n", attributeId, className, attributeName),
            writer);
      }
    }

    super.visit(node, writer);

    FileUtil.write("}\n", writer);
    return null;
  }

  @Override
  public final Void visit(final AttributeEvaluationRule node, final BufferedWriter writer) {
    final AttributeAccess targetAttributeAccess = node.getTargetAttribute();
    final AttributeSymbol targetAttribute = targetAttributeAccess.getSymbol();

    assert (targetAttribute != null);
    assert (this.attributeNodes.containsKey(targetAttribute));

    for (final AttributeAccess sourceAttributeAccess : node.gatherSourceAttributes()) {
      final AttributeSymbol sourceAttribute = sourceAttributeAccess.getSymbol();

      assert (sourceAttribute != null);
      assert (this.attributeNodes.containsKey(sourceAttribute));

      FileUtil.write(
          String.format("  %s -> %s;\n",
              this.attributeNodes.get(sourceAttribute),
              this.attributeNodes.get(targetAttribute)),
          writer);
    }
    
    return null;
  }

}
