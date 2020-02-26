package i2.act.lala.ast.visitors;

import i2.act.lala.ast.*;

import java.util.List;

public abstract class BaseLaLaSpecificationVisitor<P, R>
    implements LanguageSpecificationVisitor<P, R> {

  protected R prolog(final LaLaASTNode node, final P parameter) {
    /* intentionally left blank -- may be implemented in subclass */
    return null;
  }

  protected R epilog(final LaLaASTNode node, final P parameter) {
    /* intentionally left blank -- may be implemented in subclass */
    return null;
  }

  protected R beforeChild(final LaLaASTNode parent, final LaLaASTNode child, final P parameter) {
    /* intentionally left blank -- may be implemented in subclass */
    return null;
  }

  protected R afterChild(final LaLaASTNode parent, final LaLaASTNode child, final P parameter) {
    /* intentionally left blank -- may be implemented in subclass */
    return null;
  }

  // ---------------------------------------------------------------------------------

  protected final R visitChild(final LaLaASTNode parent, final LaLaASTNode child,
                               final P parameter) {
    beforeChild(parent, child, parameter);
    final R returnValue = child.accept(this, parameter);
    afterChild(parent, child, parameter);

    return returnValue;
  }

  // ---------------------------------------------------------------------------------

  @Override
  public R visit(final LaLaSpecification specification, final P parameter) {
    prolog(specification, parameter);

    final List<UseStatement> useStatements = specification.getUseStatements();
    for (final UseStatement useStatement : useStatements) {
      visitChild(specification, useStatement, parameter);
    }

    final List<ClassDeclaration> classDeclarations = specification.getClassDeclarations();
    for (final ClassDeclaration classDeclaration : classDeclarations) {
      visitChild(specification, classDeclaration, parameter);
    }

    epilog(specification, parameter);
    return null;
  }

  @Override
  public R visit(final UseStatement useStatement, final P parameter) {
    prolog(useStatement, parameter);

    final Identifier namespace = useStatement.getNamespace();
    visitChild(useStatement, namespace, parameter);
    
    epilog(useStatement, parameter);
    return null;
  }

  @Override
  public R visit(final ProductionClassDeclaration classDeclaration, final P parameter) {
    prolog(classDeclaration, parameter);

    final List<Annotation> annotations = classDeclaration.getAnnotations();
    for (final Annotation annotation : annotations) {
      visitChild(classDeclaration, annotation, parameter);
    }

    final Identifier className = classDeclaration.getClassName();
    visitChild(classDeclaration, className, parameter);

    final List<AttributeDeclaration> attributeDeclarations =
        classDeclaration.getAttributeDeclarations();
    for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
      visitChild(classDeclaration, attributeDeclaration, parameter);
    }

    final List<ProductionDeclaration> productionDeclarations =
        classDeclaration.getProductionDeclarations();
    for (final ProductionDeclaration productionDeclaration : productionDeclarations) {
      visitChild(classDeclaration, productionDeclaration, parameter);
    }

    epilog(classDeclaration, parameter);
    return null;
  }

  @Override
  public R visit(final LiteralClassDeclaration classDeclaration, final P parameter) {
    prolog(classDeclaration, parameter);

    final List<Annotation> annotations = classDeclaration.getAnnotations();
    for (final Annotation annotation : annotations) {
      visitChild(classDeclaration, annotation, parameter);
    }

    final Identifier className = classDeclaration.getClassName();
    visitChild(classDeclaration, className, parameter);

    final StringLiteral regularExpression = classDeclaration.getRegularExpression();
    visitChild(classDeclaration, regularExpression, parameter);

    epilog(classDeclaration, parameter);
    return null;
  }

  @Override
  public R visit(final AttributeDeclaration attributeDeclaration, final P parameter) {
    prolog(attributeDeclaration, parameter);

    final Identifier attributeName = attributeDeclaration.getAttributeName();
    visitChild(attributeDeclaration, attributeName, parameter);

    final AttributeModifier modifier = attributeDeclaration.getModifier();
    visitChild(attributeDeclaration, modifier, parameter);

    if (attributeDeclaration.hasTypeName()) {
      final AttributeTypeName typeName = attributeDeclaration.getTypeName();
      visitChild(attributeDeclaration, typeName, parameter);
    }

    epilog(attributeDeclaration, parameter);
    return null;
  }

  @Override
  public R visit(final LocalAttributeDefinition localAttributeDefinition, final P parameter) {
    prolog(localAttributeDefinition, parameter);

    final Identifier attributeName = localAttributeDefinition.getAttributeName();
    visitChild(localAttributeDefinition, attributeName, parameter);

    final AttributeExpression attributeExpression =
        localAttributeDefinition.getAttributeExpression();
    visitChild(localAttributeDefinition, attributeExpression, parameter);

    epilog(localAttributeDefinition, parameter);
    return null;
  }

  @Override
  public R visit(final AttributeModifier attributeModifier, final P parameter) {
    prolog(attributeModifier, parameter);
    epilog(attributeModifier, parameter);
    return null;
  }

  @Override
  public R visit(final AttributeTypeName attributeTypeName, final P parameter) {
    prolog(attributeTypeName, parameter);

    final Identifier name = attributeTypeName.getName();
    visitChild(attributeTypeName, name, parameter);

    epilog(attributeTypeName, parameter);
    return null;
  }

  @Override
  public R visit(final TreeProductionDeclaration treeProductionDeclaration, final P parameter) {
    return visit((ProductionDeclaration) treeProductionDeclaration, parameter);
  }

  @Override
  public R visit(final GeneratorProductionDeclaration generatorProductionDeclaration,
      final P parameter) {
    return visit((ProductionDeclaration) generatorProductionDeclaration, parameter);
  }

  private final R visit(final ProductionDeclaration productionDeclaration, final P parameter) {
    prolog(productionDeclaration, parameter);

    final List<Annotation> annotations = productionDeclaration.getAnnotations();
    for (final Annotation annotation : annotations) {
      visitChild(productionDeclaration, annotation, parameter);
    }

    final Identifier productionName = productionDeclaration.getProductionName();
    visitChild(productionDeclaration, productionName, parameter);

    final LaLaASTNode content = productionDeclaration.getContent();
    visitChild(productionDeclaration, content, parameter);

    if (productionDeclaration instanceof GeneratorProductionDeclaration) {
      final AttributeTypeName typeName =
          ((GeneratorProductionDeclaration) productionDeclaration).getTypeName();
      visitChild(productionDeclaration, typeName, parameter);
    }

    final List<LocalAttributeDefinition> localAttributeDefinitions =
        productionDeclaration.getLocalAttributeDefinitions();
    for (final LocalAttributeDefinition localAttributeDefinition : localAttributeDefinitions) {
      visitChild(productionDeclaration, localAttributeDefinition, parameter);
    }

    final List<AttributeEvaluationRule> attributeEvaluationRules =
        productionDeclaration.getAttributeEvaluationRules();
    for (final AttributeEvaluationRule attributeEvaluationRule : attributeEvaluationRules) {
      visitChild(productionDeclaration, attributeEvaluationRule, parameter);
    }

    epilog(productionDeclaration, parameter);
    return null;
  }

  @Override
  public R visit(final AttributeEvaluationRule attributeEvaluationRule, final P parameter) {
    prolog(attributeEvaluationRule, parameter);

    final AttributeAccess targetAttribute = attributeEvaluationRule.getTargetAttribute();
    visitChild(attributeEvaluationRule, targetAttribute, parameter);

    final AttributeExpression attributeExpression =
        attributeEvaluationRule.getAttributeExpression();
    visitChild(attributeEvaluationRule, attributeExpression, parameter);

    epilog(attributeEvaluationRule, parameter);
    return null;
  }

  @Override
  public R visit(final AttributeAccess attributeAccess, final P parameter) {
    prolog(attributeAccess, parameter);

    final Identifier targetName = attributeAccess.getTargetName();
    visitChild(attributeAccess, targetName, parameter);

    final Identifier attributeName = attributeAccess.getAttributeName();
    visitChild(attributeAccess, attributeName, parameter);

    epilog(attributeAccess, parameter);
    return null;
  }

  @Override
  public R visit(final LocalAttributeAccess localAttributeAccess, final P parameter) {
    prolog(localAttributeAccess, parameter);

    final Identifier attributeName = localAttributeAccess.getAttributeName();
    visitChild(localAttributeAccess, attributeName, parameter);

    epilog(localAttributeAccess, parameter);
    return null;
  }

  @Override
  public R visit(final AttributeLiteral attributeLiteral, final P parameter) {
    prolog(attributeLiteral, parameter);
    epilog(attributeLiteral, parameter);
    return null;
  }

  @Override
  public R visit(final AttributeFunction attributeFunction, final P parameter) {
    prolog(attributeFunction, parameter);

    final Identifier namespace = attributeFunction.getNamespace();
    if (namespace != null) {
      visitChild(attributeFunction, namespace, parameter);
    }

    final Identifier functionName = attributeFunction.getFunctionName();
    visitChild(attributeFunction, functionName, parameter);

    epilog(attributeFunction, parameter);
    return null;
  }

  @Override
  public R visit(final AttributeFunctionCall attributeFunctionCall, final P parameter) {
    prolog(attributeFunctionCall, parameter);

    final AttributeFunction function = attributeFunctionCall.getFunction();
    visitChild(attributeFunctionCall, function, parameter);

    final List<AttributeExpression> arguments = attributeFunctionCall.getArguments();
    for (final AttributeExpression argument : arguments) {
      visitChild(attributeFunctionCall, argument, parameter);
    }

    epilog(attributeFunctionCall, parameter);
    return null;
  }

  @Override
  public R visit(final ChildReference childReference, final P parameter) {
    prolog(childReference, parameter);

    final Identifier childName = childReference.getChildName();
    visitChild(childReference, childName, parameter);

    epilog(childReference, parameter);
    return null;
  }

  @Override
  public R visit(final GeneratorValue generatorValue, final P parameter) {
    prolog(generatorValue, parameter);
    epilog(generatorValue, parameter);
    return null;
  }

  @Override
  public R visit(final Annotation annotation, final P parameter) {
    prolog(annotation, parameter);

    final Identifier identifier = annotation.getIdentifier();
    visitChild(annotation, identifier, parameter);

    final List<Expression> arguments = annotation.getArguments();
    for (final Expression argument : arguments) {
      visitChild(annotation, argument, parameter);
    }

    epilog(annotation, parameter);
    return null;
  }

  @Override
  public R visit(final Serialization serialization, final P parameter) {
    prolog(serialization, parameter);

    final StringLiteral interpolatedString = serialization.getInterpolatedString();
    visitChild(serialization, interpolatedString, parameter);

    epilog(serialization, parameter);
    return null;
  } 

  @Override
  public R visit(final StringCharacters stringCharacters, final P parameter) {
    prolog(stringCharacters, parameter);
    epilog(stringCharacters, parameter);
    return null;
  }

  @Override
  public R visit(final EscapeSequence escapeSequence, final P parameter) {
    prolog(escapeSequence, parameter);
    epilog(escapeSequence, parameter);
    return null;
  }

  @Override
  public R visit(final ChildDeclaration childDeclaration, final P parameter) {
    prolog(childDeclaration, parameter);

    final Identifier childName = childDeclaration.getChildName();
    visitChild(childDeclaration, childName, parameter);

    final TypeName typeName = childDeclaration.getTypeName();
    visitChild(childDeclaration, typeName, parameter);

    epilog(childDeclaration, parameter);
    return null;
  }

  @Override
  public R visit(final PrintCommand printCommand, final P parameter) {
    prolog(printCommand, parameter);

    final AttributeExpression expression = printCommand.getExpression();
    visitChild(printCommand, expression, parameter);

    epilog(printCommand, parameter);
    return null;
  }

  @Override
  public R visit(final TypeName typeName, final P parameter) {
    prolog(typeName, parameter);

    final Identifier name = typeName.getName();
    visitChild(typeName, name, parameter);

    epilog(typeName, parameter);
    return null;
  }

  @Override
  public R visit(final Identifier identifier, final P parameter) {
    prolog(identifier, parameter);
    epilog(identifier, parameter);
    return null;
  }

  @Override
  public R visit(final Constant constant, final P parameter) {
    prolog(constant, parameter);
    epilog(constant, parameter);
    return null;
  }

  @Override
  public R visit(final StringLiteral stringLiteral, final P parameter) {
    prolog(stringLiteral, parameter);

    final List<StringElement> stringElements = stringLiteral.getStringElements();
    for (final StringElement stringElement : stringElements) {
      assert (stringElement instanceof LaLaASTNode);
      visitChild(stringLiteral, (LaLaASTNode) stringElement, parameter);
    }

    epilog(stringLiteral, parameter);
    return null;
  }

  @Override
  public R visit(final EntityReference entityReference, final P parameter) {
    prolog(entityReference, parameter);

    visitChild(entityReference, entityReference.getEntityName(), parameter);

    epilog(entityReference, parameter);
    return null;
  }

}
