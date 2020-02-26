package i2.act.lala.parser;

import i2.act.antlr.LaLaParserVisitor;
import i2.act.errors.RPGException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.*;
import i2.act.lala.info.SourceFile;
import i2.act.lala.info.SourcePosition;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.attributes.BuiltinFunction;
import i2.act.lala.semantics.attributes.BuiltinFunctionTable;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

import static i2.act.antlr.LaLaParser.*;
import static i2.act.lala.ast.AttributeModifier.AttributeModifierKind;

public final class LaLaBuilder implements LaLaParserVisitor<LaLaASTNode> {

  private final SourceFile sourceFile;

  public LaLaBuilder(final SourceFile sourceFile) {
    this.sourceFile = sourceFile;
  }


  // ---------------------------------------------------------------------------------


  public static final SourceRange getSourceRange(final ParserRuleContext context) {
    final Token startToken = context.getStart();
    final Token stopToken = context.getStop();

    final SourcePosition begin = new SourcePosition(startToken.getLine(),
        startToken.getCharPositionInLine());
    final SourcePosition end = new SourcePosition(stopToken.getLine(),
        stopToken.getCharPositionInLine() + stopToken.getText().length() - 1);

    return new SourceRange(begin, end);
  }


  // ---------------------------------------------------------------------------------


  @Override
  public final LaLaASTNode visit(final ParseTree parseTree) {
    return parseTree.accept(this);
  }

  @Override
  public final LaLaASTNode visitTerminal(final TerminalNode terminalNode) {
    return terminalNode.accept(this);
  }

  @Override
  public final LaLaASTNode visitChildren(final RuleNode ruleNode) {
    return ruleNode.accept(this);
  }

  @Override
  public final LaLaASTNode visitErrorNode(final ErrorNode errorNode) {
    return errorNode.accept(this);
  }


  // ---------------------------------------------------------------------------------


  private boolean automaticParentheses;


  @Override
  public final LaLaSpecification visitLanguageSpecification(
      final LanguageSpecificationContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final LaLaSpecification specification = new LaLaSpecification(sourceRange);

    final List<UseStatementContext> useStatementContexts = context.useStatement();
    if (useStatementContexts != null) {
      for (final UseStatementContext useStatementContext : useStatementContexts) {
        final UseStatement useStatement = (UseStatement) visit(useStatementContext);
        specification.addUseStatement(useStatement);
      }
    }

    final List<ClassDeclarationContext> classDeclarationContexts =
        context.classDeclaration();
    for (final ClassDeclarationContext classDeclarationContext : classDeclarationContexts) {
      final ClassDeclaration classDeclaration = (ClassDeclaration) visit(classDeclarationContext);
      specification.addClassDeclaration(classDeclaration);
    }

    return specification;
  }

  @Override
  public final UseStatement visitUseStatement(final UseStatementContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier namespace = (Identifier) visit(context.namespace);
    
    return new UseStatement(sourceRange, namespace);
  }
  
  @Override
  public final ClassDeclaration visitClassDeclaration(final ClassDeclarationContext context) {
    assert (context.getChildCount() == 1);
    return (ClassDeclaration) visit(context.getChild(0));
  }

  @Override
  public final ClassDeclaration visitProductionClassDeclaration(
      final ProductionClassDeclarationContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    // class name
    final Identifier className = (Identifier) visit(context.className);

    final ProductionClassDeclaration classDeclaration =
        new ProductionClassDeclaration(sourceRange, className);

    // class annotations
    {
      final List<AnnotationContext> annotationContexts = context.annotation();
      for (final AnnotationContext annotationContext : annotationContexts) {
        final Annotation annotation = (Annotation) visit(annotationContext);
        classDeclaration.addAnnotation(annotation);
      }
    }

    // attributes
    {
      final List<AttributeDeclarationContext> attributeDeclarationContexts =
          context.attributeDeclaration();
      for (final AttributeDeclarationContext attributeContext : attributeDeclarationContexts) {
        final AttributeDeclaration attributeDeclaration =
            (AttributeDeclaration) visit(attributeContext);
        classDeclaration.addAttributeDeclaration(attributeDeclaration);
      }
    }

    // productions
    {
      final List<ProductionDeclarationContext> productionDeclarationContexts =
          context.productionDeclaration();
      for (final ProductionDeclarationContext productionContext : productionDeclarationContexts) {
        final ProductionDeclaration productionDeclaration =
            (ProductionDeclaration) visit(productionContext);
        classDeclaration.addProductionDeclaration(productionDeclaration);
      }
    }

    return classDeclaration;
  }

  @Override
  public final ClassDeclaration visitLiteralClassDeclaration(
      final LiteralClassDeclarationContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    // class name
    final Identifier className = (Identifier) visit(context.className);

    // regular expression
    final StringLiteral regularExpression = (StringLiteral) visit(context.regularExpression);

    final LiteralClassDeclaration classDeclaration =
        new LiteralClassDeclaration(sourceRange, className, regularExpression);

    // class annotations
    {
      final List<AnnotationContext> annotationContexts = context.annotation();
      for (final AnnotationContext annotationContext : annotationContexts) {
        final Annotation annotation = (Annotation) visit(annotationContext);
        classDeclaration.addAnnotation(annotation);
      }
    }

    return classDeclaration;
  }

  @Override
  public final AttributeDeclaration visitAttributeDeclaration(
      final AttributeDeclarationContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    @SuppressWarnings("unchecked")
    final Identifier attributeName = (Identifier) visit(context.attributeName);

    final AttributeModifier modifier = (AttributeModifier) visit(context.modifier);

    final AttributeTypeName typeName;
    {
      if (context.attributeTypeName() != null) {
        typeName = (AttributeTypeName) visit(context.attributeTypeName());
      } else {
        typeName = null;
      }
    }

    final AttributeDeclaration attributeDeclaration =
        new AttributeDeclaration(sourceRange, attributeName, modifier, typeName);

    return attributeDeclaration;
  }

  @Override
  public final AttributeModifier visitAttributeModifier(final AttributeModifierContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final String modifierKindString = context.getText();
    final AttributeModifierKind modifierKind =
        AttributeModifierKind.fromStringRepresentation(modifierKindString);

    if (modifierKind == null) {
      throw new RPGException("unknown attribute modifier: " + modifierKindString);
    }

    return new AttributeModifier(sourceRange, modifierKind);
  }

  @Override
  public final AttributeTypeName visitAttributeTypeName(final AttributeTypeNameContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    @SuppressWarnings("unchecked")
    final Identifier name = (Identifier) visit(context.name);

    return new AttributeTypeName(sourceRange, name);
  }

  @Override
  public final ProductionDeclaration visitProductionDeclaration(
      final ProductionDeclarationContext context) {
    assert (context.getChildCount() == 1);
    return (ProductionDeclaration) visit(context.getChild(0));
  }

  @Override
  public final TreeProductionDeclaration visitTreeProductionDeclaration(
      final TreeProductionDeclarationContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier productionName = (Identifier) visit(context.productionName);

    final Serialization serialization = (Serialization) visit(context.serialization());

    final TreeProductionDeclaration productionDeclaration =
        new TreeProductionDeclaration(sourceRange, productionName, serialization);
    
    // annotations
    {
      final List<AnnotationContext> annotationContexts = context.annotation();
      for (final AnnotationContext annotationContext : annotationContexts) {
        final Annotation annotation = (Annotation) visit(annotationContext);
        productionDeclaration.addAnnotation(annotation);
      }
    }

    // local attribute definitions
    {
      final List<LocalAttributeDefinitionContext> localAttributeContexts =
          context.localAttributeDefinition();
      for (final LocalAttributeDefinitionContext localAttributeContext : localAttributeContexts) {
        final LocalAttributeDefinition localAttributeDefinition =
            (LocalAttributeDefinition) visit(localAttributeContext);
        productionDeclaration.addLocalAttributeDefinition(localAttributeDefinition);
      }
    }

    // attribute evaluation rules
    {
      final List<AttributeEvaluationRuleContext> attributeEvalContexts =
          context.attributeEvaluationRule();
      for (final AttributeEvaluationRuleContext attributeEvalContext : attributeEvalContexts) {
        final AttributeEvaluationRule attributeEvaluationRule =
            (AttributeEvaluationRule) visit(attributeEvalContext);
        productionDeclaration.addAttributeEvaluationRule(attributeEvaluationRule);
      }
    }

    return productionDeclaration;
  }

  @Override
  public final GeneratorProductionDeclaration visitGeneratorProductionDeclaration(
      final GeneratorProductionDeclarationContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier productionName = (Identifier) visit(context.productionName);

    final AttributeFunctionCall generatorCall =
        (AttributeFunctionCall) visit(context.generatorCall);

    final AttributeTypeName typeName = (AttributeTypeName) visit(context.attributeTypeName());

    final GeneratorProductionDeclaration productionDeclaration =
        new GeneratorProductionDeclaration(sourceRange, productionName, generatorCall, typeName);
    
    // annotations
    {
      final List<AnnotationContext> annotationContexts = context.annotation();
      for (final AnnotationContext annotationContext : annotationContexts) {
        final Annotation annotation = (Annotation) visit(annotationContext);
        productionDeclaration.addAnnotation(annotation);
      }
    }

    // local attribute definitions
    {
      final List<LocalAttributeDefinitionContext> localAttributeContexts =
          context.localAttributeDefinition();
      for (final LocalAttributeDefinitionContext localAttributeContext : localAttributeContexts) {
        final LocalAttributeDefinition localAttributeDefinition =
            (LocalAttributeDefinition) visit(localAttributeContext);
        productionDeclaration.addLocalAttributeDefinition(localAttributeDefinition);
      }
    }

    // attribute evaluation rules
    {
      final List<AttributeEvaluationRuleContext> attributeEvalContexts =
          context.attributeEvaluationRule();
      for (final AttributeEvaluationRuleContext attributeEvalContext : attributeEvalContexts) {
        final AttributeEvaluationRule attributeEvaluationRule =
            (AttributeEvaluationRule) visit(attributeEvalContext);
        productionDeclaration.addAttributeEvaluationRule(attributeEvaluationRule);
      }
    }

    return productionDeclaration;
  }

  @Override
  public final LocalAttributeDefinition visitLocalAttributeDefinition(
      final LocalAttributeDefinitionContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier attributeName = (Identifier) visit(context.attributeName);
    final AttributeExpression attributeExpression = (AttributeExpression) visit(context.rhs);

    return new LocalAttributeDefinition(sourceRange, attributeName, attributeExpression);
  }

  @Override
  public final ChildDeclaration visitChildDeclaration(final ChildDeclarationContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    @SuppressWarnings("unchecked")
    final Identifier childName = (Identifier) visit(context.childName);
    final TypeName typeName = (TypeName) visit(context.typeName());

    return new ChildDeclaration(sourceRange, childName, typeName, this.automaticParentheses);
  }

  @Override
  public final AttributeEvaluationRule visitAttributeEvaluationRule(
      final AttributeEvaluationRuleContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final AttributeAccess targetAttribute = (AttributeAccess) visit(context.targetAttribute);
    
    final AttributeExpression attributeExpression = (AttributeExpression) visit(context.rhs);

    final AttributeEvaluationRule attributeEvaluationRule =
        new AttributeEvaluationRule(sourceRange, targetAttribute, attributeExpression);

    return attributeEvaluationRule;
  }

  @Override
  public final AttributeExpression visitAttributeExpressionAlternativeAtom(
      final AttributeExpressionAlternativeAtomContext context) {
    return (AttributeExpression) visit(context.attributeAtom());
  }

  @Override
  public final AttributeExpression visitAttributeExpressionAlternativeFunctionCall(
      final AttributeExpressionAlternativeFunctionCallContext context) {
    return (AttributeExpression) visit(context.attributeFunctionCall());
  }

  @Override
  public final AttributeExpression visitAttributeExpressionAlternativeChildReference(
      final AttributeExpressionAlternativeChildReferenceContext context) {
    return (AttributeExpression) visit(context.childReference());
  }

  @Override
  public final AttributeExpression visitAttributeAtomAlternativeAttributeAccess(
      final AttributeAtomAlternativeAttributeAccessContext context) {
    return (AttributeExpression) visit(context.attributeAccess());
  }

  @Override
  public final AttributeExpression visitAttributeAtomAlternativeLocalAttributeAccess(
      final AttributeAtomAlternativeLocalAttributeAccessContext context) {
    return (AttributeExpression) visit(context.localAttributeAccess());
  }

  @Override
  public final AttributeExpression visitAttributeAtomAlternativeLiteral(
      final AttributeAtomAlternativeLiteralContext context) {
    return (AttributeExpression) visit(context.attributeLiteral());
  }

  @Override
  public final AttributeExpression visitAttributeAtomAlternativeGeneratorValue(
      final AttributeAtomAlternativeGeneratorValueContext context) {
    return (AttributeExpression) visit(context.generatorValue());
  }

  @Override
  public final AttributeFunctionCall visitAttributeFunctionCall(
      final AttributeFunctionCallContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final AttributeFunction attributeFunction =
        (AttributeFunction) visit(context.attributeFunction());

    final AttributeFunctionCall functionCall =
        new AttributeFunctionCall(sourceRange, attributeFunction);

    if (context.attributeExpression() != null) {
      final List<AttributeExpressionContext> expressionContexts =
          context.attributeExpression();
      for (final AttributeExpressionContext attributeExpressionContext : expressionContexts) {
        final AttributeExpression argument =
            (AttributeExpression) visit(attributeExpressionContext);
        functionCall.addArgument(argument);
      }
    }

    // check number of operands in case of builtin functions
    if (attributeFunction.isBuiltinFunction()) {
      final BuiltinFunction builtinFunction = attributeFunction.getBuiltinFunction();
      final int minOperands = builtinFunction.getMinOperands();
      final int maxOperands = builtinFunction.getMaxOperands();

      final int numOperands = functionCall.getNumberOfArguments();

      final String functionName = builtinFunction.getName();

      if (minOperands != BuiltinFunction.ARBITRARY_NUMBER && numOperands < minOperands) {
        LanguageSpecificationError.fail(functionCall,
            String.format("too few arguments for builtin function '%s'", functionName));
      }

      if (maxOperands != BuiltinFunction.ARBITRARY_NUMBER && numOperands > maxOperands) {
        LanguageSpecificationError.fail(functionCall,
            String.format("too many arguments for builtin function '%s'", functionName));
      }
    }

    return functionCall;
  }

  @Override
  public final AttributeAccess visitAttributeAccess(final AttributeAccessContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier targetName = (Identifier) visit(context.targetName);
    final Identifier attributeName = (Identifier) visit(context.attributeName);

    return new AttributeAccess(sourceRange, targetName, attributeName);
  }

  @Override
  public final LocalAttributeAccess visitLocalAttributeAccess(
      final LocalAttributeAccessContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier attributeName = (Identifier) visit(context.attributeName);

    return new LocalAttributeAccess(sourceRange, attributeName);
  }

  @Override
  public final ChildReference visitChildReference(final ChildReferenceContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier childName = (Identifier) visit(context.identifier());

    return new ChildReference(sourceRange, childName);
  }

  @Override
  public final GeneratorValue visitGeneratorValue(final GeneratorValueContext context) {
    final SourceRange sourceRange = getSourceRange(context);
    return new GeneratorValue(sourceRange);
  }

  @Override
  public final AttributeLiteral visitAttributeLiteral(final AttributeLiteralContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final String value = context.getText();

    return new AttributeLiteral(sourceRange, value);
  }

  @Override
  public final AttributeFunction visitRuntimeFunction(final RuntimeFunctionContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier namespace = (Identifier) visit(context.namespace);
    final Identifier functionName = (Identifier) visit(context.functionName);

    return new AttributeFunction(sourceRange, namespace, functionName);
  }

  @Override
  public final AttributeFunction visitBuiltinFunction(final BuiltinFunctionContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier functionName = (Identifier) visit(context.functionName);

    final String functionNameString = functionName.getName();
    if (!BuiltinFunctionTable.has(functionNameString)) {
      LanguageSpecificationError.fail(functionName,
          String.format("unknown builtin function: '%s'", functionNameString));
    }

    final BuiltinFunction builtinFunction = BuiltinFunctionTable.get(functionNameString);

    return new AttributeFunction(sourceRange, functionName, builtinFunction);
  }

  @Override
  public final Identifier visitAttributeFunctionName(final AttributeFunctionNameContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final String name = context.getText();

    return new Identifier(sourceRange, name);
  }

  @Override
  public final Annotation visitAnnotation(final AnnotationContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    @SuppressWarnings("unchecked")
    final Identifier identifier = (Identifier) visit(context.annotationName);

    final Annotation annotation = new Annotation(sourceRange, identifier);

    final List<AnnotationArgumentContext> argumentContexts =
        context.annotationArgument();
    for (final AnnotationArgumentContext argumentContext : argumentContexts) {
      final Expression argumentExpression = (Expression) visit(argumentContext);
      annotation.addArgument(argumentExpression);
    }

    return annotation;
  }

  @Override
  public final Expression visitAnnotationArgument(final AnnotationArgumentContext context) {
    final ExpressionContext expressionContext = context.expression();
    return (Expression) visit(expressionContext);
  }

  @Override
  public final Serialization visitSerialization(final SerializationContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final StringLiteral interpolatedString = (StringLiteral) visit(context.interpolatedString());

    final Serialization serialization = new Serialization(sourceRange, interpolatedString);

    return serialization;
  }

  @Override
  public final StringLiteral visitPlainString(final PlainStringContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final StringLiteral plainString = new StringLiteral(sourceRange);

    final List<PlainStringElementContext> elementContexts = context.plainStringElement();
    for (final PlainStringElementContext elementContext : elementContexts) {
      final StringElement stringElement = (StringElement) visit(elementContext);
      plainString.addStringElement(stringElement);
    }

    return plainString;
  }

  @Override
  public final LaLaASTNode visitPlainStringElement(final PlainStringElementContext context) {
    assert (context.getChildCount() == 1);
    return visit(context.getChild(0));
  }

  @Override
  public final StringLiteral visitInterpolatedString(final InterpolatedStringContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final StringLiteral interpolatedString = new StringLiteral(sourceRange);

    final List<InterpolatedStringElementContext> elementContexts =
        context.interpolatedStringElement();
    for (final InterpolatedStringElementContext elementContext : elementContexts) {
      final StringElement stringElement = (StringElement) visit(elementContext);
      interpolatedString.addStringElement(stringElement);
    }

    return interpolatedString;
  }

  @Override
  public final LaLaASTNode visitInterpolatedStringElement(
      final InterpolatedStringElementContext context) {
    assert (context.getChildCount() == 1);
    return visit(context.getChild(0));
  }

  @Override
  public final StringCharacters visitStringCharacters(final StringCharactersContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final String characters = context.getText();

    return new StringCharacters(sourceRange, characters);
  }

  @Override
  public final EscapeSequence visitEscapeSequence(final EscapeSequenceContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final EscapeSequence.EscapeToken escapeToken =
        EscapeSequence.EscapeToken.fromStringRepresentation(context.getText());

    return new EscapeSequence(sourceRange, escapeToken);
  }

  @Override
  public final LaLaASTNode visitStringInterpolation(final StringInterpolationContext context) {
    assert (context.getChildCount() == 1);

    return visit(context.getChild(0));
  }

  @Override
  public final LaLaASTNode visitStringInterpolationChildDeclaration(
      final StringInterpolationChildDeclarationContext context) {
    assert ((context.ESCAPE_LPAREN() == null) == (context.ESCAPE_RPAREN() == null));
    this.automaticParentheses = (context.ESCAPE_LPAREN() != null);

    final ChildDeclaration childDeclaration = (ChildDeclaration) visit(context.childDeclaration());
    return childDeclaration;
  }

  @Override
  public final LaLaASTNode visitStringInterpolationPrintCommand(
      final StringInterpolationPrintCommandContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final AttributeExpression expression =
        (AttributeExpression) visit(context.attributeExpression());

    return new PrintCommand(sourceRange, expression);
  }

  @Override
  public final LaLaASTNode visitDot(final DotContext context) {
    // intentionally left blank
    return null;
  }

  @Override
  public final LaLaASTNode visitColon(final ColonContext context) {
    // intentionally left blank
    return null;
  }

  @Override
  public final LaLaASTNode visitLparen(final LparenContext context) {
    // intentionally left blank
    return null;
  }

  @Override
  public final LaLaASTNode visitRparen(final RparenContext context) {
    // intentionally left blank
    return null;
  }

  @Override
  public final LaLaASTNode visitNumber(final NumberContext context) {
    // intentionally left blank
    return null;
  }

  @Override
  public final LaLaASTNode visitCharLiteral(final CharLiteralContext context) {
    // intentionally left blank
    return null;
  }

  @Override
  public final LaLaASTNode visitBooleanLiteral(final BooleanLiteralContext context) {
    // intentionally left blank
    return null;
  }

  @Override
  public final LaLaASTNode visitNil(final NilContext context) {
    // intentionally left blank
    return null;
  }

  @Override
  public final TypeName visitTypeName(final TypeNameContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    @SuppressWarnings("unchecked")
    final Identifier name = (Identifier) visit(context.name);

    return new TypeName(sourceRange, name);
  }

  @Override
  public final Identifier visitIdentifier(final IdentifierContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final String name = context.getText();

    return new Identifier(sourceRange, name);
  }

  @Override
  public final Constant visitExpressionNumber(final ExpressionNumberContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final String value = context.getText();

    return new Constant(sourceRange, value);
  }

  @Override
  public final StringLiteral visitExpressionString(final ExpressionStringContext context) {
    return (StringLiteral) visit(context.plainString());
  }

  @Override
  public final EntityReference visitExpressionEntityReference(
      final ExpressionEntityReferenceContext context) {
    return (EntityReference) visit(context.entityReference());
  }

  @Override
  public final EntityReference visitEntityReference(final EntityReferenceContext context) {
    final SourceRange sourceRange = getSourceRange(context);

    final Identifier entityName = (Identifier) visit(context.entityName);

    return new EntityReference(sourceRange, entityName);
  }

}
