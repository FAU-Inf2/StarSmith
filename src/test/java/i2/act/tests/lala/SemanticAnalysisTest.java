package i2.act.tests.lala;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.*;
import i2.act.lala.ast.visitors.BaseLaLaSpecificationVisitor;
import i2.act.lala.ast.visitors.SemanticAnalysis;
import i2.act.lala.info.SourcePosition;
import i2.act.lala.parser.LaLaParser;
import i2.act.lala.semantics.symbols.ChildSymbol;
import i2.act.lala.semantics.symbols.Symbol;
import i2.act.tests.util.TestData;
import i2.act.tests.util.TestUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Executes the test cases for the semantic analysis pass.
 */
@RunWith(Parameterized.class)
public final class SemanticAnalysisTest {

  private static final String[] resourcePaths = {
      "/SemanticAnalysis/positive",
      "/SemanticAnalysis/negative",
  };

  /**
   * Generates the list of test cases that should be executed.
   *
   * @return              the list of test cases that should be executed
   * @throws IOException  if some IO operation fails
   */
  @Parameters(name = "{0}")
  public static final List<String[]> testData() throws IOException {
    final List<String[]> testParameters = new ArrayList<String[]>();

    for (final String resourcePath : resourcePaths) {
      TestUtil.findTestResources(resourcePath, testParameters);
    }

    return testParameters;
  }


  // ==========================================================


  private final String resourceNameInputFile;
  private final String resourceNameTestData;

  /**
   * Creates a new instance that represents a single test case.
   *
   * @param resourceNameInputFile the name of the input program
   * @param resourceNameTestData  the file name of the test data file
   */
  public SemanticAnalysisTest(final String resourceNameInputFile,
      final String resourceNameTestData) {
    this.resourceNameInputFile = resourceNameInputFile;
    this.resourceNameTestData = resourceNameTestData;
  }

  /**
   * Executes a single test case for the semantic analysis pass.
   *
   * @throws Throwable  if something fails
   */
  @Test
  public final void testSemanticAnalysis() throws Throwable {
    final File fileCode = TestUtil.getResourceFile(this.resourceNameInputFile);

    final File fileTestData = TestUtil.getResourceFile(this.resourceNameTestData);
    final TestData testData = TestUtil.readTestData(fileTestData);

    final LaLaParser parser =
        LaLaParser.constructParser(fileCode);
    final LaLaSpecification specification = parser.parseLanguageSpecification();

    SourcePosition[] actualErrorPositions = null;

    try {
      SemanticAnalysis.analyze(specification);
    } catch (final InvalidLanguageSpecificationException exception) {
      final List<LanguageSpecificationError> errors = exception.getLanguageSpecificationErrors();
      actualErrorPositions = TestUtil.getErrorPositions(errors);
    }

    // check error positions
    final SourcePosition[] expectedErrorPositions = testData.getExpectedErrorPositions();
    TestUtil.checkErrorPositions(expectedErrorPositions, actualErrorPositions);

    if (actualErrorPositions != null && actualErrorPositions.length > 0) {
      // do not check remaining properties as they may be violated in case of an error
      return;
    }

    // check that all declarations have a symbol (and that a type is assigned to it)
    specification.accept(new BaseLaLaSpecificationVisitor<Void, Void>() {

      @Override
      public final Void prolog(final LaLaASTNode astNode, final Void parameter) {
        if (astNode instanceof Declaration) {
          final Declaration declaration = (Declaration) astNode;

          final Symbol<?> symbol = declaration.getSymbol();
          assertTrue(
              String.format("declaration without symbol ('%s')", declaration.getName()),
              symbol != null);
        }

        return null;
      }

    }, null);

    // check that all identifiers have a symbol (and that a type is assigned to it)
    specification.accept(new BaseLaLaSpecificationVisitor<Void, Void>() {

      @Override
      public final Void visit(final UseStatement useStatement, final Void parameter) {
        // identifiers of use statements have no symbols assigned
        return null;
      }

      @Override
      public final Void visit(final AttributeFunction attributeFunction, final Void parameter) {
        // identifiers of attribute functions have no symbols assigned
        return null;
      }

      @Override
      public final Void visit(final AttributeTypeName attributeTypeName, final Void parameter) {
        // identifiers of attribute type names have no symbols assigned
        return null;
      }

      @Override
      public final Void visit(final Identifier identifier, final Void parameter) {
        final Symbol<?> symbol = identifier.getSymbol();
        assertTrue(
            String.format("identifier without symbol ('%s')", identifier.getName()),
            symbol != null);

        return null;
      }

    }, null);

    // check that all productions have a 'this' symbol (and that a type is assigned to it)
    specification.accept(new BaseLaLaSpecificationVisitor<Void, Void>() {

      @Override
      public final Void visit(final TreeProductionDeclaration productionDeclaration,
          final Void parameter) {
        return visit((ProductionDeclaration) productionDeclaration);
      }

      @Override
      public final Void visit(final GeneratorProductionDeclaration productionDeclaration,
          final Void parameter) {
        return visit((ProductionDeclaration) productionDeclaration);
      }

      private final Void visit(final ProductionDeclaration productionDeclaration) {
        final ChildSymbol symbol = productionDeclaration.getThisSymbol();
        assertTrue(
            String.format("production without 'this' symbol ('%s')",
                productionDeclaration.getName()),
            symbol != null);

        return null;
      }

    }, null);
  }

}
