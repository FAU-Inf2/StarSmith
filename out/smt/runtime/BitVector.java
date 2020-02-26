package runtime;

import i2.act.fuzzer.Node;

import java.util.Random;

public final class BitVector {

  private static final Random rng = new Random();

  public static final String randomBitVector(final Node node, int minWidth, int maxWidth) {
    if (minWidth < 1) {
      minWidth = 1;
    }

    if (maxWidth == -1) {
      maxWidth = minWidth + 10;
    }

    rng.setSeed(node.id);

    final int width;
    {
      if (minWidth == maxWidth) {
        width = minWidth;
      } else {
        width = minWidth + rng.nextInt(maxWidth - minWidth + 1);
      }
    }

    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < width; ++i) {
      builder.append(rng.nextInt(2));
    }

    return builder.toString();
  }

}
