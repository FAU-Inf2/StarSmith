package i2.act.fuzzer.runtime;

import i2.act.fuzzer.Node;

public final class AutomaticParentheses {
  
  public static final boolean needsParentheses(final Node parent, final Node child) {
    assert (parent.isResolved());

    if (child == null || !child.isResolved()) {
      return false;
    }

    final int parentPrecedence = parent.getProduction().precedence;
    final int childPrecedence = child.getProduction().precedence;

    if (parentPrecedence == -1 || childPrecedence == -1) {
      return true;
    }

    return childPrecedence <= parentPrecedence;
  }

}
