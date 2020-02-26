package i2.act.tests.util;

import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.info.SourcePosition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Utility class that provides some helper methods for testing the implementation.
 */
public final class TestUtil {

  private static final boolean ONLY_CHECK_LINES_IN_ERROR_POSITIONS = false;

  private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

  private TestUtil() {
    /* intentionally left blank */
  }

  /**
   * Returns a {@link java.io.File} instance for the resource with the given name.
   *
   * @param resourceName  the name of the resource file
   * @return              a {@link java.io.File} instance for the resource
   */
  public static final File getResourceFile(final String resourceName) {
    try {
      return new File(TestUtil.class.getResource(resourceName.replace('\\', '/')).getFile());
    } catch (final Throwable throwable) {
      fail("unable to read resource '" + resourceName + "': " + throwable.getMessage());

      assert (false);
      return null;
    }
  }

  /**
   * Returns whether the resource with the given name is a directory.
   *
   * @param resourceName  the name of the resource
   * @return              true, if the resource with the given name is a directory
   */
  public static final boolean isDirectory(final String resourceName) {
    final File resourceFile = getResourceFile(resourceName);
    return resourceFile.isDirectory();
  }

  /**
   * Recursively finds all resource files in the given path.
   *
   * @param path            the path in which resource files should be searched for
   * @param listDirectories true, if directories should be returned instead of files
   * @return                a list containing all resource files that were found
   * @throws IOException    if some IO operation fails
   */
  public static final List<String> getResourceFileNames(String path,
      final boolean listDirectories) throws IOException {
    if (!path.endsWith(File.separator)) {
      path = path + File.separator;
    }

    final List<String> resourceFileNames = new ArrayList<>();

    InputStream inputStream = null;
    BufferedReader reader = null;

    try {
      inputStream = TestUtil.class.getResourceAsStream(path);

      if (inputStream == null) {
        // resource path does not exist
        return resourceFileNames;
      }

      reader = new BufferedReader(new InputStreamReader(inputStream));

      String resourceFileName;
      while ((resourceFileName = reader.readLine()) != null) {
        final String fullResourceFileName = path + resourceFileName;
        if (isDirectory(fullResourceFileName)) {
          if (listDirectories) {
            resourceFileNames.add(fullResourceFileName);
          }
          resourceFileNames.addAll(getResourceFileNames(fullResourceFileName, listDirectories));
        } else {
          if (!listDirectories) {
            resourceFileNames.add(fullResourceFileName);
          }
        }
      }
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
      if (reader != null) {
        reader.close();
      }
    }

    return resourceFileNames;
  }

  /**
   * Finds all test files (*.test) in the given resource path and adds them to the given list.
   *
   * @param resourcePath  the resource path in which the test files should be searched for
   * @param testResources the list to which the test files should be added
   * @throws IOException  if some IO operation fails
   */
  public static final void findTestResources(final String resourcePath,
      final List<String[]> testResources) throws IOException {
    final List<String> resourceFileNames = TestUtil.getResourceFileNames(resourcePath, false);

    for (final String resourceFileName : resourceFileNames) {
      final String FILENAME_PATTERN = "([^.]*)(.[0-9]*)?.test$";
      if (resourceFileName.matches(FILENAME_PATTERN)) {
        final String resourceNameTestData = resourceFileName;
        final String resourceNameProgram =
            resourceNameTestData.replaceAll(FILENAME_PATTERN, "$1.ls");
        testResources.add(new String[] {resourceNameProgram, resourceNameTestData});
      }
    }
  }

  /**
   * Reads the test data from the given YAML file.
   *
   * @param file          the file from which the test data should be read
   * @return              the test data
   * @throws IOException  if some IO operation fails
   */
  public static final TestData readTestData(final File file) throws IOException {
    return TestUtil.objectMapper.readValue(file, TestData.class);
  }

  private static final boolean isEmpty(final SourcePosition[] sourcePositions) {
    return (sourcePositions == null || sourcePositions.length == 0);
  }

  private static final String sourcePositionsToString(final SourcePosition[] sourcePositions) {
    final StringBuilder builder = new StringBuilder();

    boolean first = true;
    for (final SourcePosition sourcePosition : sourcePositions) {
      if (!first) {
        builder.append(", ");
      } else {
        first = false;
      }

      builder.append("[");
      builder.append(sourcePosition.toString());
      builder.append("]");
    }

    return builder.toString();
  }

  private static final boolean errorPositionsEquals(final SourcePosition[] expectedErrorPositions,
      final SourcePosition[] actualErrorPositions) {
    if (expectedErrorPositions.length != actualErrorPositions.length) {
      return false;
    }

    final int numberOfErrorPositions = expectedErrorPositions.length;
    for (int index = 0; index < numberOfErrorPositions; ++index) {
      final SourcePosition expectedErrorPosition = expectedErrorPositions[index];
      if (expectedErrorPosition == SourcePosition.UNKNOWN) {
        continue;
      }

      final SourcePosition actualErrorPosition = actualErrorPositions[index];

      if (expectedErrorPosition.getLine() != actualErrorPosition.getLine()) {
        return false;
      }

      if (!ONLY_CHECK_LINES_IN_ERROR_POSITIONS) {
        if (expectedErrorPosition.getColumn() != actualErrorPosition.getColumn()) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Compares the actual error positions with the expected error positions and fails the currently
   * running test if they do not match.
   *
   * @param expectedErrorPositions  the expected error positions
   * @param actualErrorPositions    the actual error positions
   */
  public static final void checkErrorPositions(final SourcePosition[] expectedErrorPositions,
      final SourcePosition[] actualErrorPositions) {
    if (isEmpty(expectedErrorPositions)) {
      if (isEmpty(actualErrorPositions)) {
        // did not expect errors and none occured -> success
        return;
      } else {
        // did not expect errors, but errors occured -> failure
        final String actualString = sourcePositionsToString(actualErrorPositions);
        fail("unexpected error(s) at " + actualString);
      }
    } else {
      if (isEmpty(actualErrorPositions)) {
        // expected errors, but none occured -> falure
        final String expectedString = sourcePositionsToString(expectedErrorPositions);
        fail("expected error(s) at " + expectedString);
      } else {
        // expected errors and some occured -> check in detail
        Arrays.sort(expectedErrorPositions);
        Arrays.sort(actualErrorPositions);

        // we could use assertArrayEquals() but this one gives more information
        if (!errorPositionsEquals(expectedErrorPositions, actualErrorPositions)) {
          final String expectedString = sourcePositionsToString(expectedErrorPositions);
          final String actualString = sourcePositionsToString(actualErrorPositions);
          fail("expected error(s) at " + expectedString + ", but found error(s) at "
              + actualString);
        }
      }
    }
  }

  /**
   * Returns the error positions from a given list of language specification errors.
   *
   * @param languageSpecificationErrors the list of lala errors
   * @return                            the error positions from the given language specification
   *                                    errors
   */
  public static final SourcePosition[] getErrorPositions(
      final List<LanguageSpecificationError> languageSpecificationErrors) {
    final int numberOfErrorPositions = languageSpecificationErrors.size();
    final SourcePosition[] errorPositions = new SourcePosition[numberOfErrorPositions];

    int index = 0;
    for (final LanguageSpecificationError error : languageSpecificationErrors) {
      final SourcePosition errorPosition = error.getSourcePosition();
      errorPositions[index] = errorPosition;

      ++index;
    }

    return errorPositions;
  }

}
