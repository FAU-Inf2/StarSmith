package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;
import i2.act.lala.semantics.attributes.BuiltinFunction;

public final class AttributeFunction extends LaLaASTNode {

  private final Identifier namespace;
  private final Identifier functionName;

  private final BuiltinFunction builtinFunction;

  public AttributeFunction(final SourceRange sourceRange, final Identifier namespace,
      final Identifier functionName) {
    super(sourceRange);
    this.namespace = namespace;
    this.functionName = functionName;
    this.builtinFunction = null;
  }

  public AttributeFunction(final SourceRange sourceRange, final Identifier functionName,
      final BuiltinFunction builtinFunction) {
    super(sourceRange);
    this.namespace = null;
    this.functionName = functionName;
    this.builtinFunction = builtinFunction;
  }

  public final Identifier getNamespace() {
    return this.namespace;
  }

  public final Identifier getFunctionName() {
    return this.functionName;
  }
  
  public final boolean isBuiltinFunction() {
    return this.builtinFunction != null;
  }

  public final BuiltinFunction getBuiltinFunction() {
    return this.builtinFunction;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
