package i2.act.lala.ast;

import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

import java.util.List;
import java.util.stream.Collectors;

public final class TreeProductionDeclaration extends ProductionDeclaration {

  private final Serialization serialization;

  private boolean isRecursive;

  public TreeProductionDeclaration(final SourceRange sourceRange, final Identifier productionName,
      final Serialization serialization) {
    super(sourceRange, productionName);
    this.serialization = serialization;
  }

  @Override
  public final LaLaASTNode getContent() {
    return this.serialization;
  }

  public final Serialization getSerialization() {
    return this.serialization;
  }

  @Override
  public final boolean isLeafProduction() {
    return this.serialization.getChildDeclarations().isEmpty();
  }

  @Override
  public final List<ChildDeclaration> getChildDeclarations() {
    return this.serialization.getChildDeclarations();
  }

  @Override
  public final int getNumberOfChildDeclarations() {
    return this.serialization.getChildDeclarations().size();
  }

  public final int getIndexOfChild(final ChildDeclaration childDeclaration) {
    return this.serialization.getChildDeclarations().indexOf(childDeclaration);
  }

  @Override
  public final boolean isGeneratorNode() {
    return false;
  }

  @Override
  public final void setIsRecursive(final boolean recursive) {
    this.isRecursive = recursive;
  }

  @Override
  public final boolean isRecursive() {
    return this.isRecursive;
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

  @Override
  public final List<ClassDeclaration> successors() {
    return getChildDeclarations().stream()
        .map(cd -> cd.getSymbol().getType().getDeclaration())
        .collect(Collectors.toList());
  }

}
