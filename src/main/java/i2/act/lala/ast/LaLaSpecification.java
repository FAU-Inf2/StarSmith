package i2.act.lala.ast;

import i2.act.errors.RPGException;
import i2.act.lala.ast.visitors.LanguageSpecificationVisitor;
import i2.act.lala.info.SourceRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LaLaSpecification extends LaLaASTNode {

  private final List<UseStatement> useStatements;
  private final List<ClassDeclaration> classDeclarations;

  public LaLaSpecification(final SourceRange sourceRange) {
    super(sourceRange);
    this.useStatements = new ArrayList<UseStatement>();
    this.classDeclarations = new ArrayList<ClassDeclaration>();
  }

  public final void addUseStatement(final UseStatement useStatement) {
    this.useStatements.add(useStatement);
  }

  public final void addClassDeclaration(final ClassDeclaration classDeclaration) {
    this.classDeclarations.add(classDeclaration);
  }

  public final List<UseStatement> getUseStatements() {
    return Collections.unmodifiableList(this.useStatements);
  }

  public final List<ClassDeclaration> getClassDeclarations() {
    return Collections.unmodifiableList(this.classDeclarations);
  }

  public final ClassDeclaration getRootClassDeclaration() {
    if (this.classDeclarations.isEmpty()) {
      throw new RPGException("specification does not have a root class");
    }
    return this.classDeclarations.get(0);
  }

  @Override
  public final <P, R> R accept(final LanguageSpecificationVisitor<P, R> visitor,
      final P parameter) {
    return visitor.visit(this, parameter);
  }

}
