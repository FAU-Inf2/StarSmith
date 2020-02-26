package i2.act.tests.util;

import i2.act.errors.RPGException;
import i2.act.lala.info.SourcePosition;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the test data (expected error positions, ...) that is stored in YAML files.
 */
public final class TestData {

  private static final String JSON_PARSER_RULE = "parserRule";
  private static final String JSON_EXPECTED_ERRORS = "expectedErrors";

  private String parserRule;
  private SourcePosition[] expectedErrorPositions;

  /**
   * Sets the parser rule (used by the Jackson library).
   *
   * @param parserRule  the parser rule
   */
  @JsonProperty(JSON_PARSER_RULE)
  public final void setParserRule(final String parserRule) {
    this.parserRule = parserRule;
  }

  /**
   * Sets the expected errors (used by the Jackson library).
   *
   * @param expectedErrors  the expected errors
   */
  @JsonProperty(JSON_EXPECTED_ERRORS)
  public final void setExpectedErrors(final int[][] expectedErrors) {
    boolean containsUnknownErrorPosition = false;
    boolean containsKnownErrorPosition = false;

    // convert int arrays to SourcePositions
    if (expectedErrors == null) {
      this.expectedErrorPositions = new SourcePosition[0];
    } else {
      final int numberOfExpectedErrors = expectedErrors.length;
      this.expectedErrorPositions = new SourcePosition[numberOfExpectedErrors];

      for (int index = 0; index < numberOfExpectedErrors; ++index) {
        final int[] expectedError = expectedErrors[index];

        assert (expectedError != null);
        assert (expectedError.length == 2);

        final SourcePosition expectedErrorPosition;
        {
          if (expectedError[0] == -1 && expectedError[1] == -1) {
            expectedErrorPosition = SourcePosition.UNKNOWN;
            containsUnknownErrorPosition = true;
          } else {
            expectedErrorPosition = new SourcePosition(expectedError[0], expectedError[1]);
            containsKnownErrorPosition = true;
          }
        }

        this.expectedErrorPositions[index] = expectedErrorPosition;
      }
    }

    // both known and unknown error positions were specified but this is currently not supported
    if (containsUnknownErrorPosition && containsKnownErrorPosition) {
      throw new RPGException("test case specifies both known and unknown error positions");
    }
  }

  /**
   * Returns the parser rule that should be used for parsing the test case.
   *
   * @return  the parser rule that should be used for parsing the test case
   */
  @JsonProperty(JSON_PARSER_RULE)
  public final String getParserRule() {
    return this.parserRule;
  }

  /**
   * Returns the expected error positions.
   *
   * @return  the expected error positions
   */
  @JsonProperty(JSON_EXPECTED_ERRORS)
  public final SourcePosition[] getExpectedErrorPositions() {
    return this.expectedErrorPositions;
  }

}
