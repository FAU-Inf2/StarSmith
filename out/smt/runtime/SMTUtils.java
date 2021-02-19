package runtime;

import i2.act.fuzzer.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class SMTUtils {

  private static final Random rng = new Random();

  private static final class Value {

    protected final int value;
    protected final String string;

    protected Value(final int value, final String string) {
      this.value = value;
      this.string = string;
    }

    @Override
    public final String toString() {
      return this.string;
    }

  }

  public static final Value hexNumber(final Node node, final int minValue) {
    rng.setSeed(node.id);

    final int value;
    {
      if (minValue >= 255) {
        value = 255;
      } else {
        value = minValue + rng.nextInt(256 - minValue);
      }
    }

    return new Value(value, "#x" + Integer.toHexString(value));
  }

  public static final Value singletonCharacterSeq(final Node node, final int minValue) {
    rng.setSeed(node.id);

    final List<Character> characters = new ArrayList<>();
    {
      for (char character = 32; character <= 125; ++character) {
        characters.add(character);
      }
    }

    Collections.shuffle(characters, rng);

    for (final char character : characters) {
      if (character >= minValue) {
        final String string;
        {
          if (character == '"') {
            string = "\"\"";
          } else {
            string = "" + character;
          }
        }
        return new Value(character, string);
      }
    }

    assert (false);
    return null;
  }

  public static final int getValue(final Value value) {
    return value.value;
  }

}
