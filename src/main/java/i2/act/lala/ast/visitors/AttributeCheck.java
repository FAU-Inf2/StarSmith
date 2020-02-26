package i2.act.lala.ast.visitors;

import i2.act.errors.specification.attributes.AttributeRedefinitionException;
import i2.act.errors.specification.attributes.MisplacedGeneratorValue;
import i2.act.errors.specification.attributes.MissingAttributeEvaluationRule;
import i2.act.errors.specification.attributes.WrongAttributeEvaluation;
import i2.act.errors.specification.attributes.WrongSourceAttributeException;
import i2.act.lala.ast.*;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.attributes.AttributeInstance;
import i2.act.lala.semantics.symbols.AnnotationSymbol;
import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.lala.semantics.symbols.ChildSymbol;
import i2.act.lala.semantics.symbols.ClassSymbol;
import i2.act.lala.semantics.symbols.LocalAttributeSymbol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static i2.act.lala.ast.AttributeModifier.AttributeModifierKind;

public final class AttributeCheck extends BaseLaLaSpecificationVisitor<Void, Void> {

  private AttributeCheck() {
    // intentionally left blank
  }

  public static final void analyze(final LaLaSpecification specification) {
    specification.accept(new AttributeCheck(), null);
  }

  private static final List<AttributeInstance> getAutoCopyCandidates(
      final AttributeInstance targetAttribute, final ChildSymbol sourceChildSymbol) {
    final List<AttributeInstance> candidates = new ArrayList<>();
    
    final AttributeSymbol targetAttributeSymbol = targetAttribute.attributeSymbol;

    final AttributeModifierKind targetModifierKind = targetAttributeSymbol.getModifierKind();
    final String targetAttributeName = targetAttributeSymbol.getName();
    final String targetAttributeType =
        targetAttributeSymbol.getDeclaration().getTypeName().getName().getName();

    final ClassSymbol sourceClassSymbol = sourceChildSymbol.getType();
    final List<AttributeSymbol> sourceAttributeSymbols =
        sourceClassSymbol.getAttributes().gatherSymbols();

    for (final AttributeSymbol sourceAttributeSymbol : sourceAttributeSymbols) {
      final AttributeModifierKind sourceModifierKind = sourceAttributeSymbol.getModifierKind();

      if (targetModifierKind != sourceModifierKind) {
        continue;
      }

      final String sourceAttributeName = sourceAttributeSymbol.getName();
      final String sourceAttributeType =
          sourceAttributeSymbol.getDeclaration().getTypeName().getName().getName();

      if (targetAttributeName.equals(sourceAttributeName)
          && targetAttributeType.equals(sourceAttributeType)) {
        final AttributeInstance sourceAttributeInstance =
            new AttributeInstance(sourceChildSymbol, sourceAttributeSymbol);
        candidates.add(sourceAttributeInstance);
      }
    }

    return candidates;
  }

  private static final AttributeAccess createAttributeAccess(
      final AttributeInstance attributeInstance) {
    final Identifier targetName =
        new Identifier(SourceRange.UNKNOWN, attributeInstance.childSymbol.getName());
    targetName.setSymbol(attributeInstance.childSymbol);

    final Identifier attributeName =
        new Identifier(SourceRange.UNKNOWN, attributeInstance.attributeSymbol.getName());
    attributeName.setSymbol(attributeInstance.attributeSymbol);

    final AttributeAccess attributeAccess =
        new AttributeAccess(SourceRange.UNKNOWN, targetName, attributeName);
    attributeAccess.setSymbol(attributeInstance.attributeSymbol);

    return attributeAccess;
  }

  private static final AttributeEvaluationRule createAttributeEvaluationRule(
      final AttributeInstance targetAttributeInstance,
      final AttributeInstance sourceAttributeInstance) {
    final AttributeAccess targetAttributeAccess = createAttributeAccess(targetAttributeInstance);
    final AttributeAccess sourceAttributeAccess = createAttributeAccess(sourceAttributeInstance);

    return new AttributeEvaluationRule(SourceRange.UNKNOWN, targetAttributeAccess,
        sourceAttributeAccess, true);
  }

  private static final void checkAllEvaluated(final ChildSymbol childSymbol,
      final AttributeModifierKind modifier, final ProductionDeclaration productionDeclaration,
      final Set<AttributeInstance> computedAttributes) {
    final ClassSymbol classSymbol = childSymbol.getType();

    final List<AttributeSymbol> attributes = classSymbol.getAttributes().gatherSymbols();
    for (final AttributeSymbol attribute : attributes) {
      final AttributeModifierKind declaredModifier =
          attribute.getDeclaration().getModifier().getModifierKind();
      if (declaredModifier == modifier) {
        final AttributeInstance attributeInstance = new AttributeInstance(childSymbol, attribute);

        if (!computedAttributes.contains(attributeInstance)) {
          boolean implicitComputation = false;

          if (autoCopy(productionDeclaration, attribute.getName())) {
            final List<AttributeInstance> autoCopyCandidates = new ArrayList<>();
            {
              if (modifier == AttributeModifierKind.MOD_INH) {
                final ChildSymbol thisSymbol = productionDeclaration.getThisSymbol();
                autoCopyCandidates.addAll(
                    getAutoCopyCandidates(attributeInstance, thisSymbol));
              } else if (modifier == AttributeModifierKind.MOD_SYN) {
                final List<ChildDeclaration> childDeclarations =
                    productionDeclaration.getChildDeclarations();
                for (final ChildDeclaration childDeclaration : childDeclarations) {
                  final ChildSymbol productionChildSymbol = childDeclaration.getSymbol();
                  autoCopyCandidates.addAll(
                      getAutoCopyCandidates(attributeInstance, productionChildSymbol));
                }
              } else {
                // do not auto-copy guard attributes (this does not really make sense)
              }
            }

            if (!autoCopyCandidates.isEmpty()) {
              final AttributeInstance autoCopySource =
                  autoCopyCandidates.get(autoCopyCandidates.size() - 1);

              if (autoCopyCandidates.size() > 1) {
                System.err.format(
                    "[i] multiple alternatives for auto-copying attribute %s in production %s;"
                        + " choosing last one (%s)!\n",
                        attributeInstance.format(), productionDeclaration.getName(true),
                        autoCopySource.format());
              }

              final AttributeEvaluationRule copyRule =
                  createAttributeEvaluationRule(attributeInstance, autoCopySource);
              productionDeclaration.addAttributeEvaluationRule(copyRule);

              implicitComputation = true;
            }
          }

          if (!implicitComputation) {
            throw new MissingAttributeEvaluationRule(productionDeclaration, childSymbol, attribute);
          }
        }
      }
    }
  }

  @Override
  public final Void visit(final TreeProductionDeclaration productionDeclaration,
      final Void parameter) {
    return visit((ProductionDeclaration) productionDeclaration, parameter);
  }

  @Override
  public final Void visit(final GeneratorProductionDeclaration productionDeclaration,
      final Void parameter) {
    final AttributeFunctionCall generatorCall = productionDeclaration.getGeneratorCall();

    // check that generator call does not use local attributes that in turn use the generator value
    {
      generatorCall.accept(new BaseLaLaSpecificationVisitor<Void, Void>() {

          @Override
          public final Void visit(final LocalAttributeAccess localAttributeAccess,
              final Void parameter) {
            final LocalAttributeSymbol localAttributeSymbol = localAttributeAccess.getSymbol();
            assert (localAttributeSymbol != null);

            final AttributeExpression localAttributeDefinition =
                localAttributeSymbol.getDeclaration().getAttributeExpression();
            localAttributeDefinition.accept(this, null);

            return null;
          }

          @Override
          public final Void visit(final GeneratorValue generatorValue, final Void parameter) {
            throw new MisplacedGeneratorValue(generatorValue);
          }

      }, null);
    }

    // ensure that generator call only uses inherited attributes of 'this' node
    {
      // check that generator call does not use generator value as argument
      generatorCall.accept(new BaseLaLaSpecificationVisitor<Void, Void>() {

          @Override
          public final Void visit(final GeneratorValue generatorValue, final Void parameter) {
            throw new MisplacedGeneratorValue(generatorValue);
          }

      }, null);

      // check that all source attributes are inherited attributes of 'this'
      final List<AttributeAccess> sourceAttributes = generatorCall.gatherSourceAttributes();

      for (final AttributeAccess sourceAttribute : sourceAttributes) {
        final AttributeModifierKind modifierKind =
            sourceAttribute.getSymbol().getDeclaration().getModifier().getModifierKind();
        if (modifierKind != AttributeModifierKind.MOD_INH) {
          throw new WrongSourceAttributeException(sourceAttribute, productionDeclaration);
        }
      }
    }
    
    // check body of production
    visit((ProductionDeclaration) productionDeclaration, parameter);

    return null;
  }

  private final Void visit(final ProductionDeclaration productionDeclaration,
      final Void parameter) {
    final Set<AttributeInstance> computedAttributes = new HashSet<>();

    final ChildSymbol thisSymbol = productionDeclaration.getThisSymbol();
    assert (thisSymbol != null);

    // gather all attributes that are computed in the production's body 
    {
      final List<AttributeEvaluationRule> attributeEvaluationRules =
          productionDeclaration.getAttributeEvaluationRules();
      for (final AttributeEvaluationRule attributeEvaluationRule : attributeEvaluationRules) {
        final AttributeAccess attributeAccess = attributeEvaluationRule.getTargetAttribute();

        final ChildSymbol targetChild = (ChildSymbol) attributeAccess.getTargetName().getSymbol();
        assert (targetChild != null);

        final AttributeDeclaration targetAttribute =
            (AttributeDeclaration) attributeAccess.getAttributeName().getSymbol().getDeclaration();
        assert (targetAttribute != null);

        final AttributeSymbol targetAttributeSymbol = targetAttribute.getSymbol();
        assert (targetAttributeSymbol != null);

        // check that only synthesized attributes of 'this' resp. inherited attributes of children
        // are computed
        {
          final AttributeModifierKind modifierKind =
              targetAttributeSymbol.getDeclaration().getModifier().getModifierKind();
          if (targetChild == thisSymbol) {
            if ((modifierKind != AttributeModifierKind.MOD_SYN)
                && (modifierKind != AttributeModifierKind.MOD_GRD)) {
              throw new WrongAttributeEvaluation(targetChild, targetAttributeSymbol,
                  productionDeclaration);
            }
          } else {
            if (modifierKind != AttributeModifierKind.MOD_INH) {
              throw new WrongAttributeEvaluation(targetChild, targetAttributeSymbol,
                  productionDeclaration);
            }
          }
        }

        // check that the computation of ...
        // - ... synthesized/guard attributes only uses
        //   - inherited attributes of 'this'
        //   - synthesized attributes of children
        // - ... inherited attributes only uses
        //   - inherited attributes of 'this'
        //   - synthesized attributes of children
        {
          final List<AttributeAccess> sourceAttributes =
              attributeEvaluationRule.gatherSourceAttributes();
          for (final AttributeAccess sourceAttribute : sourceAttributes) {
            final AttributeModifierKind modifierKind =
                sourceAttribute.getSymbol().getDeclaration().getModifier().getModifierKind();
            final ChildSymbol sourceChild =
                (ChildSymbol) sourceAttribute.getTargetName().getSymbol();

            final boolean isChildAttribute = (sourceChild != thisSymbol);

            if ((isChildAttribute && (modifierKind != AttributeModifierKind.MOD_SYN))
                || ((!isChildAttribute && (modifierKind != AttributeModifierKind.MOD_INH)))) {
              throw new WrongSourceAttributeException(sourceAttribute, productionDeclaration);
            }
          }
        }

        final AttributeInstance attributeInstance =
            new AttributeInstance(targetChild, targetAttributeSymbol);

        // check if same attribute is already defined
        {
          if (computedAttributes.contains(attributeInstance)) {
            // redefinition of the same symbol
            throw new AttributeRedefinitionException(targetChild, targetAttributeSymbol,
                productionDeclaration);
          }
        }

        computedAttributes.add(attributeInstance);
      }
    }

    // check that all synthesized attributes of 'this' are computed
    {
      checkAllEvaluated(thisSymbol, AttributeModifierKind.MOD_SYN, productionDeclaration,
          computedAttributes);
    }

    // check that all guard attributes of 'this' are computed
    {
      checkAllEvaluated(thisSymbol, AttributeModifierKind.MOD_GRD, productionDeclaration,
          computedAttributes);
    }

    // check that all inherited attributes of all children are computed
    {
      final List<ChildDeclaration> childDeclarations = productionDeclaration.getChildDeclarations();
      for (final ChildDeclaration childDeclaration : childDeclarations) {
        final ChildSymbol childSymbol = childDeclaration.getSymbol();
        assert (childSymbol != null);

        checkAllEvaluated(childSymbol, AttributeModifierKind.MOD_INH, productionDeclaration,
            computedAttributes);
      }
    }

    return null;
  }

  private static final boolean autoCopy(final Annotation annotation, final String attributeName) {
    if (annotation == null) {
      return false;
    }

    if (annotation.getNumberOfArguments() == 0) {
      return true; // @copy without arguments -> copy all attribute values 
    }

    for (final Expression argument : annotation.getArguments()) {
      assert (argument instanceof EntityReference);
      final EntityReference entityReference = (EntityReference) argument;
      if (attributeName.equals(entityReference.getEntityName().getName())) {
        return true;
      }
    }

    return false;
  }

  private static final boolean autoCopy(final ProductionDeclaration productionDeclaration,
      final String attributeName) {
    final ClassDeclaration classDeclaration =
        productionDeclaration.getSymbol().getOwnClassSymbol().getDeclaration();

    final AnnotationSymbol ANNOTATION_COPY = AnnotationSymbol.ANNOTATION_COPY;

    return autoCopy(productionDeclaration.findAnnotation(ANNOTATION_COPY), attributeName)
        || autoCopy(classDeclaration.findAnnotation(ANNOTATION_COPY), attributeName);
  }

}
