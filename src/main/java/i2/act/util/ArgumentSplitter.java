package i2.act.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArgumentSplitter {

  public static final String[] splitArguments(final String argumentsString) {
    final List<String> argumentList = new ArrayList<>();

    final char[] argumentsChars = argumentsString.toCharArray();
    int lookaheadPosition = 0;

    while (true) {
      if (lookaheadPosition >= argumentsChars.length) {
        break;
      }

      final char firstChar = argumentsChars[lookaheadPosition];

      // skip whitespace
      {
        if (isWhitespaceCharacter(firstChar)) {
          while (lookaheadPosition < argumentsChars.length) {
            final char nextChar = argumentsChars[lookaheadPosition];

            if (!isWhitespaceCharacter(nextChar)) {
              break;
            }

            ++lookaheadPosition;
          }

          continue;
        }
      }

      // consume next argument
      {
        final StringBuilder builder = new StringBuilder();

        boolean isEscaped = false;
        char quoteCharacter = 0;

        while (lookaheadPosition < argumentsChars.length) {
          final char nextChar = argumentsChars[lookaheadPosition];

          if (isEscaped) {
            if ((quoteCharacter == '"' && nextChar == '\'')
                || (quoteCharacter == '\'' && nextChar == '"')) {
              builder.append('\\');
            }

            builder.append(nextChar);
            isEscaped = false;
          } else {
            if (isWhitespaceCharacter(nextChar) && quoteCharacter == 0) {
              break;
            } else if (nextChar == quoteCharacter) {
              quoteCharacter = 0;
            } else if (quoteCharacter == 0 && (nextChar == '"' || nextChar == '\'')) {
              quoteCharacter = nextChar;
            } else if (nextChar == '\\') {
              isEscaped = true;
            } else {
              builder.append(nextChar);
            }
          }

          ++lookaheadPosition;
        }

        final String argument = builder.toString();
        assert (!argument.isEmpty());

        argumentList.add(argument);
      }
    }

    final String[] arguments = argumentList.toArray(new String[argumentList.size()]);
    return arguments;
  }

  private static final boolean isWhitespaceCharacter(final char c) {
    return c == ' ' || c == '\n' || c == '\r' || c == '\t';
  }

  public static final String[] appendArgument(final String[] previousArguments,
      final String newArgument) {
    final String[] newArguments = Arrays.copyOf(previousArguments, previousArguments.length + 1);
    newArguments[newArguments.length - 1] = newArgument;

    return newArguments;
  }

}
