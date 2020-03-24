package i2.act.tests;

import i2.act.util.ArgumentSplitter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public final class ArgumentSplitterTest {

  @Parameters(name = "{0}")
  public static final Collection<Object[]> testData() {
    return Arrays.<Object[]>asList(
      new Object[] {"foo", new String[] {"foo"}},
      new Object[] {"foo bar", new String[] {"foo", "bar"}},
      new Object[] {"  foo bar  ", new String[] {"foo", "bar"}},
      new Object[] {"foo\tbar", new String[] {"foo", "bar"}},
      new Object[] {"foo \t bar", new String[] {"foo", "bar"}},
      new Object[] {"foo\\ bar", new String[] {"foo bar"}},
      new Object[] {"\"foo\"", new String[] {"foo"}},
      new Object[] {"'foo'", new String[] {"foo"}},
      new Object[] {"'foo'", new String[] {"foo"}},
      new Object[] {"\"'foo'\"", new String[] {"'foo'"}},
      new Object[] {"'\"foo\"'", new String[] {"\"foo\""}},
      new Object[] {"\"\\\"\"", new String[] {"\""}},
      new Object[] {"foo\"bar\"", new String[] {"foobar"}},
      new Object[] {"\"foo bar\"", new String[] {"foo bar"}},
      new Object[] {"\"\\'\"", new String[] {"\\'"}},
      new Object[] {"'\\\"'", new String[] {"\\\""}}
    );
  }


  // ==========================================================


  private final String argumentsString;
  private final String[] expectedSplit;

  public ArgumentSplitterTest(final String argumentsString, final String[] expectedSplit) {
    this.argumentsString = argumentsString;
    this.expectedSplit = expectedSplit;
  }

  @Test
  public final void testSplit() {
    final String[] actualSplit = ArgumentSplitter.splitArguments(this.argumentsString);
    assertArrayEquals(this.expectedSplit, actualSplit);
  }

}
