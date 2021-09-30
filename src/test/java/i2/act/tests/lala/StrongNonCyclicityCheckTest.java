package i2.act.tests.lala;

import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.ast.LaLaSpecification;
import i2.act.lala.ast.visitors.AttributeCheck;
import i2.act.lala.ast.visitors.SemanticAnalysis;
import i2.act.lala.ast.visitors.StrongNonCyclicityCheck;
import i2.act.lala.info.SourcePosition;
import i2.act.lala.parser.LaLaParser;
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

import static org.junit.Assert.fail;

/**
 * Executes the test cases for the strong non-cyclicity check pass.
 */
@RunWith(Parameterized.class)
public final class StrongNonCyclicityCheckTest {

  private static final String[] resourcePaths = {
      "/CycleCheck/positive",
      "/CycleCheck/negative",
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
  public StrongNonCyclicityCheckTest(final String resourceNameInputFile,
      final String resourceNameTestData) {
    this.resourceNameInputFile = resourceNameInputFile;
    this.resourceNameTestData = resourceNameTestData;
  }

  /**
   * Executes a single test case for the strong non-cyclicity check pass.
   *
   * @throws Throwable  if something fails
   */
  @Test
  public final void testCycleCheck() throws Throwable {
    final File fileCode = TestUtil.getResourceFile(this.resourceNameInputFile);

    final File fileTestData = TestUtil.getResourceFile(this.resourceNameTestData);
    final TestData testData = TestUtil.readTestData(fileTestData);

    final LaLaParser parser =
        LaLaParser.constructParser(fileCode);
    final LaLaSpecification specification = parser.parseLanguageSpecification();

    SourcePosition[] actualErrorPositions = null;

    try {
      SemanticAnalysis.analyze(specification);
      AttributeCheck.analyze(specification);
    } catch (final InvalidLanguageSpecificationException exception) {
      fail("invalid test case, should not happen");
    }

    try {
      StrongNonCyclicityCheck.analyze(specification, false, false);
    } catch (final InvalidLanguageSpecificationException exception) {
      final List<LanguageSpecificationError> errors = exception.getLanguageSpecificationErrors();
      actualErrorPositions = TestUtil.getErrorPositions(errors);
    }

    // check error positions
    final SourcePosition[] expectedErrorPositions = testData.getExpectedErrorPositions();
    TestUtil.checkErrorPositions(expectedErrorPositions, actualErrorPositions);
  }

}
