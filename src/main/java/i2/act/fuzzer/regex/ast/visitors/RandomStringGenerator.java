package i2.act.fuzzer.regex.ast.visitors;

import i2.act.fuzzer.regex.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RandomStringGenerator implements RegularExpressionVisitor<StringBuilder, Void> {

  private final Random random;

  public RandomStringGenerator() {
    this(System.currentTimeMillis());
  }

  public RandomStringGenerator(final long seed) {
    this.random = new Random(seed);
  }

  public final String generateString(final RegularExpression regularExpression) {
    final StringBuilder builder = new StringBuilder();
    visit(regularExpression, builder);

    return builder.toString();
  }

  public final String generateString(final Sequence sequence) {
    final StringBuilder builder = new StringBuilder();
    visit(sequence, builder);

    return builder.toString();
  }

  public final List<String> generateStrings(final RegularExpression regularExpression,
      final int count) {
    final List<String> strings = new ArrayList<>(count);

    final List<Sequence> sequencesWithAlternatives = new ArrayList<>();

    int remainingCount = count;

    // ensure that each root alternative is instantiated at least once
    {
      final List<Sequence> rootAlternatives = regularExpression.getAlternatives();
      for (final Sequence sequence : rootAlternatives) {
        final String randomString = generateString(sequence);
        strings.add(randomString);

        --remainingCount;

        if (sequence.hasAlternatives()) {
          sequencesWithAlternatives.add(sequence);
        }
      }
    }

    // create additional random instantiations
    if (!sequencesWithAlternatives.isEmpty()) {
      final int numberOfSequences = sequencesWithAlternatives.size();

      for (int i = 0; i < remainingCount; ++i) {
        final Sequence sequence = sequencesWithAlternatives.get(i % numberOfSequences);

        final String randomString = generateString(sequence);
        strings.add(randomString);
      }
    }

    return strings;
  }

  @Override
  public final Void visit(final Bounds bounds, final StringBuilder builder) {
    /* intentionally left blank */
    return null;
  }

  @Override
  public final Void visit(final i2.act.fuzzer.regex.ast.Character character,
      final StringBuilder builder) {
    builder.append(character.getChar());

    return null;
  }

  @Override
  public final Void visit(final CharacterRange characterRange, final StringBuilder builder) {
    char lowerChar = characterRange.getLowerCharacter().getChar().charAt(0);
    char upperChar = characterRange.getUpperCharacter().getChar().charAt(0);

    if (lowerChar > upperChar) {
      final char tmp = lowerChar;
      lowerChar = upperChar;
      upperChar = tmp;
    }

    final char randomChar = (char) (lowerChar + this.random.nextInt(upperChar - lowerChar + 1));
    builder.append(randomChar);

    return null;
  }

  @Override
  public final Void visit(final Group group, final StringBuilder builder) {
    int repetitions = getRandomRepetitions(group.getBounds());

    final List<Range> ranges = group.getRanges();

    while (repetitions-- > 0) {
      final int randomIndex = this.random.nextInt(ranges.size());
      final Range randomRange = ranges.get(randomIndex);

      randomRange.accept(this, builder);
    }

    return null;
  }

  @Override
  public final Void visit(final RegularExpression regularExpression, final StringBuilder builder) {
    final List<Sequence> alternatives = regularExpression.getAlternatives();

    final int randomIndex = this.random.nextInt(alternatives.size());
    final Sequence randomSequence = alternatives.get(randomIndex);

    randomSequence.accept(this, builder);

    return null;
  }

  @Override
  public final Void visit(final SingleCharacter singleCharacter, final StringBuilder builder) {
    final i2.act.fuzzer.regex.ast.Character character = singleCharacter.getCharacter();
    character.accept(this, builder);

    return null;
  }

  @Override
  public final Void visit(final Sequence sequence, final StringBuilder builder) {
    for (final Atom atom : sequence.getAtoms()) {
      atom.accept(this, builder);
    }

    return null;
  }

  @Override
  public final Void visit(final Subexpression subexpression, final StringBuilder builder) {
    final RegularExpression expression = subexpression.getExpression();

    int repetitions = getRandomRepetitions(subexpression.getBounds());
    while (repetitions-- > 0) {
      expression.accept(this, builder);
    }

    return null;
  }

  private final int getRandomRepetitions(final Bounds bounds) {
    if (bounds == null) {
      return 1;
    } else {
      final int minimum = bounds.getMinimum();
      final int maximum = bounds.getMaximum();

      if (minimum <= maximum) {
        return minimum + this.random.nextInt(maximum - minimum + 1);
      } else {
        return 0;
      }
    }
  }

}
