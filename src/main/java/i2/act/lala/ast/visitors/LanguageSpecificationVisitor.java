package i2.act.lala.ast.visitors;

import i2.act.lala.ast.*;

public interface LanguageSpecificationVisitor<P, R> {

  public R visit(final LaLaSpecification specification, final P parameter);

  public R visit(final UseStatement useStatement, final P parameter);

  public R visit(final ProductionClassDeclaration productionClassDeclaration, final P parameter);

  public R visit(final LiteralClassDeclaration literalClassDeclaration, final P parameter);

  public R visit(final AttributeDeclaration attributeDeclaration, final P parameter);

  public R visit(final LocalAttributeDefinition localAttributeDefinition, final P parameter);

  public R visit(final AttributeModifier attributeModifier, final P parameter);

  public R visit(final AttributeTypeName attributeTypeName, final P parameter);

  public R visit(final TreeProductionDeclaration treeProductionDeclaration, final P parameter);

  public R visit(final GeneratorProductionDeclaration generatorProductionDeclaration,
      final P parameter);

  public R visit(final AttributeEvaluationRule attributeEvaluationRule, final P parameter);

  public R visit(final AttributeAccess attributeAccess, final P parameter);

  public R visit(final LocalAttributeAccess localAttributeAccess, final P parameter);

  public R visit(final AttributeLiteral attributeLuteral, final P parameter);

  public R visit(final AttributeFunction attributeFunction, final P parameter);

  public R visit(final AttributeFunctionCall attributeFunctionCall, final P parameter);

  public R visit(final ChildReference childReference, final P parameter);

  public R visit(final GeneratorValue generatorValue, final P parameter);

  public R visit(final Annotation annotation, final P parameter);

  public R visit(final Serialization serialization, final P parameter);

  public R visit(final StringCharacters stringCharacters, final P parameter);

  public R visit(final EscapeSequence escapeSequence, final P parameter);

  public R visit(final ChildDeclaration childDeclaration, final P parameter);

  public R visit(final PrintCommand printCommand, final P parameter);

  public R visit(final TypeName typeName, final P parameter);

  public R visit(final Identifier identifier, final P parameter);

  public R visit(final Constant constant, final P parameter);

  public R visit(final StringLiteral stringLiteral, final P parameter);

  public R visit(final EntityReference entityReference, final P parameter);

}
