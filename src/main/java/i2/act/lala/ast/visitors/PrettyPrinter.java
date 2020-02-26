package i2.act.lala.ast.visitors;

import i2.act.lala.ast.*;
import i2.act.util.FileUtil;

import java.io.BufferedWriter;
import java.util.List;

public final class PrettyPrinter
    extends BaseLaLaSpecificationVisitor<BufferedWriter, Void> {

  private static final String GENERATOR_VALUE = "$";
  
  private int currentIndentationLevel;

  private final void writeIndentation(final BufferedWriter writer) {
    for (int indentation = 0; indentation < this.currentIndentationLevel; ++indentation) {
      FileUtil.write("  ", writer);
    }
  }

  public final void prettyPrint(final LaLaSpecification specification,
      final BufferedWriter writer) {
    specification.accept(this, writer);
  }

  @Override
  public final Void visit(final LaLaSpecification specification,
      final BufferedWriter writer) {
    this.currentIndentationLevel = 0;

    final List<UseStatement> useStatements = specification.getUseStatements();
    if (!useStatements.isEmpty()) {
      for (final UseStatement useStatement : useStatements) {
        useStatement.accept(this, writer);
      }

      FileUtil.write("\n", writer);
    }

    boolean first = true;
    final List<ClassDeclaration> classDeclarations = specification.getClassDeclarations();
    for (final ClassDeclaration classDeclaration : classDeclarations) {
      if (!first) {
        FileUtil.write("\n", writer);
      } else {
        first = false;
      }

      classDeclaration.accept(this, writer);
    }

    FileUtil.flushWriter(writer);

    return null;
  }

  @Override
  public final Void visit(final UseStatement useStatement, final BufferedWriter writer) {
    FileUtil.write("use ", writer);

    final Identifier namespace = useStatement.getNamespace();
    namespace.accept(this, writer);

    FileUtil.write(";\n", writer);

    return null;
  }

  @Override
  public final Void visit(final ProductionClassDeclaration classDeclaration,
      final BufferedWriter writer) {
    final List<Annotation> annotations = classDeclaration.getAnnotations();
    for (final Annotation annotation : annotations) {
      annotation.accept(this, writer);
    }

    writeIndentation(writer);

    FileUtil.write("class ", writer);

    final Identifier className = classDeclaration.getClassName();
    className.accept(this, writer);

    FileUtil.write(" {\n", writer);

    ++this.currentIndentationLevel;

    // attributes
    {
      final List<AttributeDeclaration> attributeDeclarations =
          classDeclaration.getAttributeDeclarations();
      for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
        attributeDeclaration.accept(this, writer);
      }

      if (!attributeDeclarations.isEmpty()) {
        FileUtil.write("\n", writer);
      }
    }

    // productions
    {
      boolean first = true;
      final List<ProductionDeclaration> productionDeclarations =
          classDeclaration.getProductionDeclarations();
      for (final ProductionDeclaration productionDeclaration : productionDeclarations) {
        if (!first) {
          FileUtil.write("\n", writer);
        } else {
          first = false;
        }

        productionDeclaration.accept(this, writer);
      }
    }

    --this.currentIndentationLevel;

    FileUtil.write("}\n", writer);

    return null;
  }

  @Override
  public final Void visit(final LiteralClassDeclaration classDeclaration,
      final BufferedWriter writer) {
    final List<Annotation> annotations = classDeclaration.getAnnotations();
    for (final Annotation annotation : annotations) {
      annotation.accept(this, writer);
    }

    writeIndentation(writer);

    FileUtil.write("class ", writer);

    final Identifier className = classDeclaration.getClassName();
    className.accept(this, writer);

    FileUtil.write("(", writer);

    final StringLiteral regularExpression = classDeclaration.getRegularExpression();
    regularExpression.accept(this, writer);

    FileUtil.write(");\n", writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeDeclaration attributeDeclaration,
      final BufferedWriter writer) {
    writeIndentation(writer);

    final AttributeModifier modifier = attributeDeclaration.getModifier();
    modifier.accept(this, writer);

    FileUtil.write(" ", writer);

    final Identifier attributeName = attributeDeclaration.getAttributeName();
    attributeName.accept(this, writer);

    if (attributeDeclaration.hasTypeName()) {
      FileUtil.write(" : ", writer);

      final AttributeTypeName typeName = attributeDeclaration.getTypeName();
      typeName.accept(this, writer);
    }

    FileUtil.write(";\n", writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeModifier attributeModifier, final BufferedWriter writer) {
    FileUtil.write(attributeModifier.getModifierKind().stringRepresentation, writer);
    return null;
  }

  @Override
  public final Void visit(final AttributeTypeName attributeTypeName,
      final BufferedWriter writer) {
    final Identifier name = attributeTypeName.getName();
    name.accept(this, writer);

    return null;
  }

  @Override
  public final Void visit(final TreeProductionDeclaration productionDeclaration,
      final BufferedWriter writer) {
    visit((ProductionDeclaration) productionDeclaration, true, writer);
    return null;
  }

  @Override
  public final Void visit(final GeneratorProductionDeclaration productionDeclaration,
      final BufferedWriter writer) {
    visit((ProductionDeclaration) productionDeclaration, false, writer);
    return null;
  }

  private final void visit(final ProductionDeclaration productionDeclaration,
      final boolean addParens, final BufferedWriter writer) {
    final List<Annotation> annotations = productionDeclaration.getAnnotations();
    for (final Annotation annotation : annotations) {
      annotation.accept(this, writer);
    }

    writeIndentation(writer);

    final Identifier productionName = productionDeclaration.getProductionName();
    productionName.accept(this, writer);

    FileUtil.write(" ", writer);

    if (addParens) {
      FileUtil.write("(", writer);
    }

    final LaLaASTNode content = productionDeclaration.getContent();
    content.accept(this, writer);

    if (addParens) {
      FileUtil.write(")", writer);
    }

    if (productionDeclaration instanceof GeneratorProductionDeclaration) {
      final AttributeTypeName typeName =
          ((GeneratorProductionDeclaration) productionDeclaration).getTypeName();
      FileUtil.write(" : ", writer);
      typeName.accept(this, writer);
    }

    FileUtil.write(" {\n", writer);

    ++this.currentIndentationLevel;

    // visit local attribute definitions
    {
      final List<LocalAttributeDefinition> localAttributeDefinitions =
          productionDeclaration.getLocalAttributeDefinitions();
      for (final LocalAttributeDefinition localAttributeDefinition : localAttributeDefinitions) {
        localAttributeDefinition.accept(this, writer);
      }

      if (!localAttributeDefinitions.isEmpty()) {
        FileUtil.write("\n", writer);
      }
    }

    // visit attribute evaluation rules
    {
      final List<AttributeEvaluationRule> attributeEvaluationRules =
          productionDeclaration.getAttributeEvaluationRules();
      for (final AttributeEvaluationRule attributeEvaluationRule : attributeEvaluationRules) {
        if (!attributeEvaluationRule.isAutoCopyRule) {
          attributeEvaluationRule.accept(this, writer);
          FileUtil.write("\n", writer);
        }
      }
    }

    --this.currentIndentationLevel;

    writeIndentation(writer);
    FileUtil.write("}\n", writer);
  }

  @Override
  public final Void visit(final LocalAttributeDefinition localAttributeDefinition,
      final BufferedWriter writer) {
    writeIndentation(writer);

    FileUtil.write("loc ", writer);

    final Identifier attributeName = localAttributeDefinition.getAttributeName();
    attributeName.accept(this, writer);

    FileUtil.write(" = ", writer);

    final AttributeExpression attributeExpression =
        localAttributeDefinition.getAttributeExpression();
    attributeExpression.accept(this, writer);

    FileUtil.write(";\n", writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeEvaluationRule attributeEvaluationRule,
      final BufferedWriter writer) {
    writeIndentation(writer);

    final AttributeAccess targetAttribute = attributeEvaluationRule.getTargetAttribute();
    targetAttribute.accept(this, writer);

    FileUtil.write(" = ", writer);

    final AttributeExpression attributeExpression =
        attributeEvaluationRule.getAttributeExpression();
    attributeExpression.accept(this, writer);

    FileUtil.write(";", writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeAccess attributeAccess, final BufferedWriter writer) {
    final Identifier targetName = attributeAccess.getTargetName();
    targetName.accept(this, writer);

    FileUtil.write(".", writer);

    final Identifier attributeName = attributeAccess.getAttributeName();
    attributeName.accept(this, writer);

    return null;
  }

  @Override
  public final Void visit(final LocalAttributeAccess localAttributeAccess,
      final BufferedWriter writer) {
    FileUtil.write(".", writer);

    final Identifier attributeName = localAttributeAccess.getAttributeName();
    attributeName.accept(this, writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeLiteral attributeLiteral, final BufferedWriter writer) {
    final String value = attributeLiteral.getValue();

    FileUtil.write(value, writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeFunctionCall attributeFunctionCall,
      final BufferedWriter writer) {
    FileUtil.write("(", writer);

    final AttributeFunction function = attributeFunctionCall.getFunction();
    function.accept(this, writer);

    final List<AttributeExpression> arguments = attributeFunctionCall.getArguments();
    for (final AttributeExpression argument : arguments) {
      FileUtil.write(" ", writer);
      argument.accept(this, writer);
    }

    FileUtil.write(")", writer);

    return null;
  }

  @Override
  public final Void visit(final GeneratorValue generatorValue, final BufferedWriter writer) {
    FileUtil.write(GENERATOR_VALUE, writer);
    return null;
  }

  @Override
  public final Void visit(final AttributeFunction attributeFunction, final BufferedWriter writer) {
    final Identifier namespace = attributeFunction.getNamespace();
    if (namespace != null) {
      namespace.accept(this, writer);
      FileUtil.write(":", writer);
    }

    final Identifier functionName = attributeFunction.getFunctionName();
    functionName.accept(this, writer);

    return null;
  }

  @Override
  public final Void visit(final Annotation annotation, final BufferedWriter writer) {
    writeIndentation(writer);

    FileUtil.write("@", writer);

    final Identifier identifier = annotation.getIdentifier();
    identifier.accept(this, writer);

    FileUtil.write("(", writer);

    boolean first = true;

    final List<Expression> arguments = annotation.getArguments();
    for (final Expression argument : arguments) {
      if (first) {
        first = false;
      } else {
        FileUtil.write(", ", writer);
      }

      argument.accept(this, writer);
    }

    FileUtil.write(")\n", writer);

    return null;
  }

  @Override
  public final Void visit(final StringCharacters stringCharacters, final BufferedWriter writer) {
    final String characters = stringCharacters.getCharacters();
    FileUtil.write(characters, writer);

    return null;
  }

  @Override
  public final Void visit(final EscapeSequence escapeSequence, final BufferedWriter writer) {
    final String stringRepresentation = escapeSequence.getStringRepresentation();
    FileUtil.write(stringRepresentation, writer);

    return null;
  }

  @Override
  public final Void visit(final ChildDeclaration childDeclaration, final BufferedWriter writer) {
    if (childDeclaration.hasAutomaticParentheses()) {
      FileUtil.write("\\(", writer);
    }

    FileUtil.write("${", writer);

    final Identifier childName = childDeclaration.getChildName();
    childName.accept(this, writer);

    FileUtil.write(" : ", writer);

    final TypeName typeName = childDeclaration.getTypeName();
    typeName.accept(this, writer);

    FileUtil.write("}", writer);

    if (childDeclaration.hasAutomaticParentheses()) {
      FileUtil.write("\\)", writer);
    }

    return null;
  }

  @Override
  public final Void visit(final PrintCommand printCommand, final BufferedWriter writer) {
    FileUtil.write("#{", writer);

    final AttributeExpression attributeExpression = printCommand.getExpression();
    attributeExpression.accept(this, writer);

    FileUtil.write("}", writer);

    return null;
  }

  @Override
  public final Void visit(final TypeName typeName, final BufferedWriter writer) {
    final Identifier name = typeName.getName();
    name.accept(this, writer);

    return null;
  }

  @Override
  public final Void visit(final Identifier identifier, final BufferedWriter writer) {
    final String name = identifier.getName();
    FileUtil.write(name, writer);

    return null;
  }

  @Override
  public final Void visit(final Constant constant, final BufferedWriter writer) {
    final String value = constant.getValue();
    FileUtil.write(value, writer);

    return null;
  }

  @Override
  public final Void visit(final StringLiteral stringLiteral, final BufferedWriter writer) {
    FileUtil.write("\"", writer);

    final List<StringElement> stringElements = stringLiteral.getStringElements();
    for (final StringElement stringElement : stringElements) {
      stringElement.accept(this, writer);
    }

    FileUtil.write("\"", writer);

    return null;
  }

}
