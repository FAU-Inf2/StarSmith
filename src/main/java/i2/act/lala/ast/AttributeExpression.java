package i2.act.lala.ast;

import i2.act.lala.ast.visitors.BaseLaLaSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.LocalAttributeSymbol;

import java.util.ArrayList;
import java.util.List;

public abstract class AttributeExpression extends LaLaASTNode {

  public AttributeExpression(final SourceRange sourceRange) {
    super(sourceRange);
  }

  public final List<AttributeAccess> gatherSourceAttributes() {
    final List<AttributeAccess> sourceAttributes = new ArrayList<>();

    this.accept(new BaseLaLaSpecificationVisitor<Void, Void>() {

          @Override
          public final Void visit(final AttributeAccess attributeAccess, final Void parameter) {
            sourceAttributes.add(attributeAccess);
            return null;
          }

          @Override
          public final Void visit(final LocalAttributeAccess localAttributeAccess,
              final Void parameter) {
            final LocalAttributeSymbol attributeSymbol = localAttributeAccess.getSymbol();
            assert (attributeSymbol != null);

            final LocalAttributeDefinition definition = attributeSymbol.getDeclaration();
            assert (definition != null);

            final AttributeExpression attributeExpression = definition.getAttributeExpression();
            attributeExpression.accept(this, parameter);

            return null;
          }

          @Override
          public final Void visit(final GeneratorValue generatorValue, final Void parameter) {
            final AttributeFunctionCall generatorCall = generatorValue.getGeneratorCall();
            assert (generatorValue.getGeneratorCall() != null);

            generatorCall.accept(this, parameter);

            return null;
          }

        }, null);

    return sourceAttributes;
  }

}
