package i2.act.lala.ast.visitors;

import i2.act.errors.RPGException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.errors.specification.attributes.MisplacedGeneratorValue;
import i2.act.errors.specification.attributes.WrongAttributeType;
import i2.act.errors.specification.semantics.annotations.WrongAnnotationException;
import i2.act.errors.specification.semantics.types.TypeMismatchException;
import i2.act.lala.ast.*;
import i2.act.lala.ast.AttributeModifier.AttributeModifierKind;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.symbols.*;
import i2.act.lala.semantics.types.*;

import java.util.List;

public final class SemanticAnalysis extends BaseLaLaSpecificationVisitor<Void, Symbol<?>> {

  private static final String THIS_NAME = "this";

  private static final String LITERAL_ATTRIBUTE_NAME = "str";
  private static final String LITERAL_ATTRIBUTE_TYPE = "String";

  private final SymbolTable<ClassSymbol> classSymbols;
  private final SymbolTable<AnnotationSymbol> annotationSymbols;
  private final SymbolTable<ChildSymbol> childSymbols;
  private final SymbolTable<LocalAttributeSymbol> localAttributeSymbols;

  private ClassSymbol currentClassSymbol;

  private SemanticAnalysis() {
    this.classSymbols = new SymbolTable<ClassSymbol>();
    this.annotationSymbols = new SymbolTable<AnnotationSymbol>();
    addPredefinedAnnotations();
    this.childSymbols = new SymbolTable<ChildSymbol>();
    this.localAttributeSymbols = new SymbolTable<LocalAttributeSymbol>();
  }

  public static final void analyze(final LaLaSpecification specification) {
    final SemanticAnalysis semanticAnalysis = new SemanticAnalysis();
    specification.accept(semanticAnalysis, null);
  }

  private final void addPredefinedAnnotations() {
    this.annotationSymbols.enterScope();

    for (final AnnotationSymbol annotationSymbol : AnnotationSymbol.predefinedAnnotationSymbols) {
      this.annotationSymbols.putSymbol(annotationSymbol);
    }
  }

  // implements the first pass (class declarations)
  private final class AnalysisClassDeclarations
      extends BaseLaLaSpecificationVisitor<Void, Void> {

    @Override
    public final Void visit(final ProductionClassDeclaration classDeclaration,
        final Void parameter) {
      handleClassDeclaration(classDeclaration);
      return null;
    }

    @Override
    public final Void visit(final LiteralClassDeclaration classDeclaration, final Void parameter) {
      handleClassDeclaration(classDeclaration);
      return null;
    }

    private final void handleClassDeclaration(final ClassDeclaration classDeclaration) {
      final String name = classDeclaration.getName();
      final ClassSymbol classSymbol = new ClassSymbol(name, classDeclaration);

      SemanticAnalysis.this.classSymbols.putSymbol(classSymbol);
      classDeclaration.getIdentifier().setSymbol(classSymbol);
    }

  }

  // implements the second pass (class members)
  private final class AnalysisClassMembers
      extends BaseLaLaSpecificationVisitor<Type, Type> {

    @Override
    public final Type visit(final LaLaSpecification specification,
        final Type expectedType) {
      // create the implicit synthesized 'str' attribute of each literal class declaration
      final List<ClassDeclaration> classDeclarations = specification.getClassDeclarations();
      for (final ClassDeclaration classDeclaration : classDeclarations) {
        if (classDeclaration instanceof LiteralClassDeclaration) {
          final LiteralClassDeclaration literalClassDeclaration =
              (LiteralClassDeclaration) classDeclaration;

          final AttributeDeclaration strDeclaration = new AttributeDeclaration(
              SourceRange.UNKNOWN,
              new Identifier(SourceRange.UNKNOWN, LITERAL_ATTRIBUTE_NAME),
              new AttributeModifier(SourceRange.UNKNOWN,
                  AttributeModifier.AttributeModifierKind.MOD_SYN),
              new AttributeTypeName(SourceRange.UNKNOWN,
                  new Identifier(SourceRange.UNKNOWN, LITERAL_ATTRIBUTE_TYPE)));

          literalClassDeclaration.addAttributeDeclaration(strDeclaration);
        }
      }

      super.visit(specification, null);

      return null;
    }

    @Override
    public final Type visit(final ProductionClassDeclaration classDeclaration,
        final Type expectedType) {
      // check that a production class has either tree productions or a single generator production
      {
        final List<ProductionDeclaration> productionDeclarations =
            classDeclaration.getProductionDeclarations();

        if (productionDeclarations.isEmpty()) {
          LanguageSpecificationError.fail(classDeclaration,
              String.format("class '%s' does not have any productions",
                classDeclaration.getName()));
        }

        for (final ProductionDeclaration productionDeclaration : productionDeclarations) {
          if (productionDeclaration instanceof GeneratorProductionDeclaration) {
            if (productionDeclarations.size() > 1) {
              LanguageSpecificationError.fail(productionDeclaration,
                  "a class may only contain tree productions or a single generator production");
            } else {
              classDeclaration.isGeneratorClass(true);
            }
          }
        }
      }

      handleClassDeclaration(classDeclaration);

      // create symbol table for productions
      {
        final SymbolTable<ProductionSymbol> productionSymbols = new SymbolTable<>();
        productionSymbols.enterScope();

        SemanticAnalysis.this.currentClassSymbol.setProductions(productionSymbols);
      }

      // visit productions
      {
        final List<ProductionDeclaration> productionDeclarations =
            classDeclaration.getProductionDeclarations();
        for (final ProductionDeclaration productionDeclaration : productionDeclarations) {
          productionDeclaration.accept(this, null);
        }
      }

      return null;
    }

    @Override
    public final Type visit(final LiteralClassDeclaration classDeclaration,
        final Type expectedType) {
      handleClassDeclaration(classDeclaration);

      return null;
    }

    @Override
    public final Type visit(final Annotation annotation, final Type expectedType) {
      // name analysis
      final AnnotationSymbol annotationSymbol;
      {
        final Identifier identifier = annotation.getIdentifier();

        annotationSymbol = SemanticAnalysis.this.annotationSymbols.lookupSymbol(identifier);
        identifier.setSymbol(annotationSymbol);
      }

      // type analysis
      final AnnotationType expectedAnnotationType = annotationSymbol.getType();
      assert (expectedAnnotationType != null);
      final AnnotationType actualAnnotationType;
      {
        final List<Expression> arguments = annotation.getArguments();
        final Type[] actualArgumentTypes = new Type[arguments.size()];

        int index = 0;
        for (final Expression argument : arguments) {
          final Type expectedArgumentType = expectedAnnotationType.getArgumentType(index);
          // 'expectedArgumentType' may be null!
          actualArgumentTypes[index] = argument.accept(this, expectedArgumentType);
          ++index;
        }
        
        actualAnnotationType = new AnnotationType(actualArgumentTypes);

        if (!actualAnnotationType.canBeAssignedTo(expectedAnnotationType)) {
          throw new TypeMismatchException(annotation.getSourcePosition(),
              expectedAnnotationType, actualAnnotationType);
        }
      }

      return actualAnnotationType;
    }

    @Override
    public final Type visit(final Constant constant, final Type expectedType) {
      final Type inferredType = constant.getInferredType();
      constant.setType(inferredType);

      return inferredType;
    }

    @Override
    public final Type visit(final StringLiteral stringLiteral, final Type expectedType) {
      final StringType type = StringType.INSTANCE;
      stringLiteral.setType(type);

      super.visit(stringLiteral, null);

      return type;
    }

    @Override
    public final Type visit(final EntityReference entityReference, final Type expectedType) {
      if (expectedType == null) {
        LanguageSpecificationError.fail(entityReference, "no entity reference expected here");
        return null;
      } else if (expectedType instanceof AttributeReferenceType) {
        assert (SemanticAnalysis.this.currentClassSymbol != null);

        final Identifier entityName = entityReference.getEntityName();
        final AttributeSymbol attributeSymbol =
            SemanticAnalysis.this.currentClassSymbol.lookupAttribute(entityName);

        entityName.setSymbol(attributeSymbol);

        return expectedType;
      } else {
        throw new RPGException("unknown type: " + expectedType);
      }
    }

    @Override
    public final Type visit(final AttributeDeclaration attributeDeclaration,
        final Type expectedType) {
      final ClassSymbol currentClassSymbol = SemanticAnalysis.this.currentClassSymbol;
      assert (currentClassSymbol != null);

      // name analysis
      final AttributeSymbol attributeSymbol;
      {
        final SymbolTable<AttributeSymbol> attributeSymbols = currentClassSymbol.getAttributes();
        assert (attributeSymbols != null);

        final String name = attributeDeclaration.getName();
        attributeSymbol = new AttributeSymbol(name, attributeDeclaration, currentClassSymbol);

        attributeSymbols.putSymbol(attributeSymbol);
        attributeDeclaration.getIdentifier().setSymbol(attributeSymbol);
      }

      // modifier analysis
      {
        final AttributeModifierKind modifier = attributeDeclaration.getModifier().getModifierKind();

        if ((modifier == AttributeModifierKind.MOD_INH || modifier == AttributeModifierKind.MOD_SYN)
            && !attributeDeclaration.hasTypeName()) {
          throw new WrongAttributeType(attributeDeclaration);
        }

        if (modifier == AttributeModifierKind.MOD_GRD
            && attributeDeclaration.hasTypeName()) {
          throw new WrongAttributeType(attributeDeclaration);
        }
      }

      return null;
    }

    @Override
    public final Type visit(final TreeProductionDeclaration productionDeclaration,
        final Type expectedType) {
      visit((ProductionDeclaration) productionDeclaration);
      return null;
    }

    @Override
    public final Type visit(final GeneratorProductionDeclaration productionDeclaration,
        final Type expectedType) {
      visit((ProductionDeclaration) productionDeclaration);
      return null;
    }

    private final void visit(final ProductionDeclaration productionDeclaration) {
      final ClassSymbol currentClassSymbol = SemanticAnalysis.this.currentClassSymbol;
      assert (currentClassSymbol != null);

      // annotations
      {
        final List<Annotation> annotations = productionDeclaration.getAnnotations();
        for (final Annotation annotation : annotations) {
          annotation.accept(this, null);

          final AnnotationSymbol annotationSymbol = annotation.getSymbol();
          if (!annotationSymbol.forProductions()) {
            throw new WrongAnnotationException(annotation);
          }
        }
      }

      final SymbolTable<ProductionSymbol> productionSymbols = currentClassSymbol.getProductions();
      assert (productionSymbols != null);

      // name analysis for production
      final ProductionSymbol productionSymbol;
      {
        final String productionName = productionDeclaration.getName();
        productionSymbol =
            new ProductionSymbol(productionName, productionDeclaration, currentClassSymbol);

        productionSymbols.putSymbol(productionSymbol);
        productionDeclaration.getIdentifier().setSymbol(productionSymbol);
      }
    }

    private final void handleClassDeclaration(final ClassDeclaration classDeclaration) {
      SemanticAnalysis.this.currentClassSymbol = classDeclaration.getSymbol();
      assert (SemanticAnalysis.this.currentClassSymbol != null);

      // create symbol table for attributes
      {
        final SymbolTable<AttributeSymbol> attributeSymbols = new SymbolTable<>();
        attributeSymbols.enterScope();

        SemanticAnalysis.this.currentClassSymbol.setAttributes(attributeSymbols);
      }

      // visit attribute declarations (has to be done before visiting the annotations)
      {
        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          attributeDeclaration.accept(this, null);
        }
      }

      // annotations
      {
        final List<Annotation> annotations = classDeclaration.getAnnotations();
        for (final Annotation annotation : annotations) {
          annotation.accept(this, null);

          final AnnotationSymbol annotationSymbol = annotation.getSymbol();
          assert (annotationSymbol != null);

          if (!annotationSymbol.forClasses()) {
            throw new WrongAnnotationException(annotation);
          }
        }
      }
    }

  }

  // implements the third pass (production bodies, incl. serialization/children)
  private final class AnalysisProductionBodies
      extends BaseLaLaSpecificationVisitor<Boolean, Type> {

    private ProductionDeclaration enclosingProductionDeclaration;

    @Override
    public final Type visit(final ProductionClassDeclaration classDeclaration,
        final Boolean visitPrintCommands) {
      SemanticAnalysis.this.currentClassSymbol = classDeclaration.getSymbol();
      assert (SemanticAnalysis.this.currentClassSymbol != null);

      return super.visit(classDeclaration, visitPrintCommands);
    }

    @Override
    public final Type visit(final TreeProductionDeclaration productionDeclaration,
        final Boolean visitPrintCommands) {
      visit((ProductionDeclaration) productionDeclaration);
      return null;
    }

    @Override
    public final Type visit(final GeneratorProductionDeclaration productionDeclaration,
        final Boolean visitPrintCommands) {
      visit((ProductionDeclaration) productionDeclaration);
      return null;
    }

    private final void visit(final ProductionDeclaration productionDeclaration) {
      assert (SemanticAnalysis.this.currentClassSymbol != null);

      final LaLaASTNode content = productionDeclaration.getContent();
      assert ((content instanceof Serialization) || (content instanceof AttributeFunctionCall));

      // handle instances of 'generator values'
      productionDeclaration.accept(new BaseLaLaSpecificationVisitor<Void, Void>() {

          private AttributeFunctionCall generatorCall;
          private AttributeTypeName typeName;

          @Override
          public final Void visit(final GeneratorProductionDeclaration productionDeclaration,
              final Void parameter) {
            this.generatorCall = productionDeclaration.getGeneratorCall();
            this.typeName = productionDeclaration.getTypeName();

            super.visit(productionDeclaration, parameter);

            this.generatorCall = null;
            this.typeName = null;

            return null;
          }

          @Override
          public final Void visit(final GeneratorValue generatorValue, final Void parameter) {
            if (this.generatorCall == null) {
              throw new MisplacedGeneratorValue(generatorValue);
            } else {
              generatorValue.setGeneratorCall(this.generatorCall);
              generatorValue.setTypeName(this.typeName);
            }

            return null;
          }

      }, null);

      final ProductionSymbol productionSymbol = productionDeclaration.getSymbol();
      assert (productionSymbol != null);

      SemanticAnalysis.this.childSymbols.enterScope();
      SemanticAnalysis.this.localAttributeSymbols.enterScope();

      // add implicit symbol for 'this'
      {
        final ClassSymbol currentClassSymbol = SemanticAnalysis.this.currentClassSymbol;
        assert (currentClassSymbol != null);

        final ChildSymbol thisSymbol = new ChildSymbol(THIS_NAME, null, currentClassSymbol);
        productionDeclaration.setThisSymbol(thisSymbol);
        SemanticAnalysis.this.childSymbols.putSymbol(thisSymbol);
      }

      // visit serialization (child declarations; _not_ print commands)
      if (content instanceof Serialization) {
        final Serialization serialization = (Serialization) content;
        serialization.accept(this, false);
      } else {
        // the generator call of generator productions is visited later (see below)
      }

      // update 'generating productions'
      if (productionDeclaration instanceof TreeProductionDeclaration) {
        final Serialization serialization = (Serialization) content;
        final List<ChildDeclaration> childDeclarations = serialization.getChildDeclarations();
        for (final ChildDeclaration childDeclaration : childDeclarations) {
          final ChildSymbol childSymbol = childDeclaration.getSymbol();
          assert (childSymbol != null);

          final ClassDeclaration generatedClass = childSymbol.getType().getDeclaration();
          generatedClass.addGeneratingProduction(productionDeclaration);
        }
      }

      // visit local attribute definitions
      {
        this.enclosingProductionDeclaration = productionDeclaration;

        final List<LocalAttributeDefinition> localAttributeDefinitions =
            productionDeclaration.getLocalAttributeDefinitions();
        for (final LocalAttributeDefinition localAttributeDefinition : localAttributeDefinitions) {
          localAttributeDefinition.accept(this, false);
        }

        this.enclosingProductionDeclaration = null;
      }

      // visit serialization again (print commands) or visit generator call
      if (content instanceof Serialization) {
        final Serialization serialization = (Serialization) content;
        serialization.accept(this, true);
      } else {
        final AttributeFunctionCall generatorCall = (AttributeFunctionCall) content;
        generatorCall.accept(this, true);
      }

      // visit attribute evaluation rules
      {
        final List<AttributeEvaluationRule> attributeEvaluationRules =
            productionDeclaration.getAttributeEvaluationRules();
        for (final AttributeEvaluationRule attributeEvaluationRule : attributeEvaluationRules) {
          attributeEvaluationRule.accept(this, false);
        }
      }

      SemanticAnalysis.this.childSymbols.leaveScope();
      SemanticAnalysis.this.localAttributeSymbols.leaveScope();
    }

    @Override
    public final Type visit(final LocalAttributeDefinition localAttributeDefinition,
        final Boolean visitPrintCommands) {
      assert (this.enclosingProductionDeclaration != null);
      assert (this.enclosingProductionDeclaration.getSymbol() != null);

      final String name = localAttributeDefinition.getAttributeName().getName();
      final LocalAttributeSymbol attributeSymbol =
          new LocalAttributeSymbol(name, localAttributeDefinition,
          this.enclosingProductionDeclaration.getSymbol());

      SemanticAnalysis.this.localAttributeSymbols.putSymbol(attributeSymbol);
      localAttributeDefinition.getIdentifier().setSymbol(attributeSymbol);

      final AttributeExpression attributeExpression =
          localAttributeDefinition.getAttributeExpression();
      attributeExpression.accept(this, visitPrintCommands);

      return null;
    }

    @Override
    public final Type visit(final Serialization serialization, final Boolean visitPrintCommands) {
      final StringLiteral interpolatedString = serialization.getInterpolatedString();
      final List<StringElement> stringElements = interpolatedString.getStringElements();

      if (!visitPrintCommands) {
        // first pass: child declarations
        for (final StringElement stringElement : stringElements) {
          assert (stringElement instanceof LaLaASTNode);

          if (!(stringElement instanceof PrintCommand)) {
            ((LaLaASTNode) stringElement).accept(this, visitPrintCommands);
          }
        }
      } else {
        // second pass: print commands
        for (final StringElement stringElement : stringElements) {
          assert (stringElement instanceof LaLaASTNode);

          if (stringElement instanceof PrintCommand) {
            ((LaLaASTNode) stringElement).accept(this, visitPrintCommands);
          }
        }
      }

      return null;
    }

    @Override
    public final Type visit(final ChildDeclaration childDeclaration,
        final Boolean visitPrintCommands) {
      // 'type' analysis
      final TypeName typeName = childDeclaration.getTypeName();
      final ClassSymbol childClassSymbol =
          (ClassSymbol) typeName.accept(SemanticAnalysis.this, null);
      assert (childClassSymbol != null);

      // name analysis
      final ChildSymbol childSymbol;
      {
        final String name = childDeclaration.getName();
        childSymbol = new ChildSymbol(name, childDeclaration, childClassSymbol);

        SemanticAnalysis.this.childSymbols.putSymbol(childSymbol);
        childDeclaration.getIdentifier().setSymbol(childSymbol);
      }

      return null;
    }

    @Override
    public final Type visit(final ChildReference childReference,
        final Boolean visitPrintCommands) {
      final Identifier childName = childReference.getChildName();

      final ChildSymbol childSymbol = SemanticAnalysis.this.childSymbols.lookupSymbol(childName);
      childName.setSymbol(childSymbol);

      return null;
    }

    @Override
    public final Type visit(final AttributeAccess attributeAccess,
        final Boolean visitPrintCommands) {
      final Identifier targetName = attributeAccess.getTargetName();
      final Identifier attributeName = attributeAccess.getAttributeName();

      final ChildSymbol childSymbol = SemanticAnalysis.this.childSymbols.lookupSymbol(targetName);
      targetName.setSymbol(childSymbol);

      final ClassSymbol childClassSymbol = childSymbol.getType();
      assert (childClassSymbol != null);

      final SymbolTable<AttributeSymbol> classAttributes = childClassSymbol.getAttributes();
      assert (classAttributes != null);

      final AttributeSymbol attributeSymbol = classAttributes.lookupSymbol(attributeName);
      attributeName.setSymbol(attributeSymbol);
      attributeAccess.setSymbol(attributeSymbol);

      return null;
    }

    @Override
    public final Type visit(final LocalAttributeAccess localAttributeAccess,
        final Boolean visitPrintCommands) {
      final Identifier attributeName = localAttributeAccess.getAttributeName();
      final LocalAttributeSymbol attributeSymbol =
          SemanticAnalysis.this.localAttributeSymbols.lookupSymbol(attributeName);

      localAttributeAccess.setSymbol(attributeSymbol);

      return null;
    }

  }

  @Override
  public final Symbol<?> visit(final LaLaSpecification specification,
      final Void parameter) {
    // the semantic analysis needs three passes:
    //  1: handle top level class declarations
    //  2: handle class members (attributes and productions) for all classes
    //  3: handle production bodies (i.e., child declarations, print commands and attribute
    //     evaluation rules)

    this.classSymbols.enterScope();

    specification.accept(new AnalysisClassDeclarations(), null);
    specification.accept(new AnalysisClassMembers(), null);
    specification.accept(new AnalysisProductionBodies(), null);

    return null;
  }

  @Override
  public final ClassSymbol visit(final TypeName typeName, final Void parameter) {
    final Identifier name = typeName.getName();

    final ClassSymbol classSymbol = this.classSymbols.lookupSymbol(name);
    name.setSymbol(classSymbol);

    return classSymbol;
  }

}
