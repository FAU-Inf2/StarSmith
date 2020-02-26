package runtime;

import i2.act.fuzzer.runtime.EmbeddedCode;

import java.util.LinkedHashSet;

public final class Labels {

  public final LinkedHashSet<String> definedLabels;
  public final LinkedHashSet<String> missingLabels;

  public Labels() {
    this.definedLabels = new LinkedHashSet<String>();
    this.missingLabels = new LinkedHashSet<String>();
  }

  public final Labels clone() {
    final Labels clone = new Labels();
    clone.definedLabels.addAll(this.definedLabels);
    clone.missingLabels.addAll(this.missingLabels);

    return clone;
  }

  // ===========================================================================

  public static final Labels empty() {
    return new Labels();
  }

  public static final boolean isDefined(final Labels labels, final String label) {
    return labels.definedLabels.contains(label);
  }

  public static final boolean isMissing(final Labels labels, final String label) {
    return labels.missingLabels.contains(label);
  }

  public static final Labels handleGoto(final Labels labels, final String label) {
    if (!labels.definedLabels.contains(label)) {
      final Labels newLabels = labels.clone();
      newLabels.missingLabels.add(label);

      return newLabels;
    }

    return labels;
  }

  public static final Labels handleLabel(final Labels labels, final String label) {
    final Labels newLabels = labels.clone();
    newLabels.definedLabels.add(label);

    if (newLabels.missingLabels.contains(label)) {
      newLabels.missingLabels.remove(label);
    }

    return newLabels;
  }

  public static final int definedCount(final Labels labels) {
    return labels.definedLabels.size();
  }

  public static final int missingCount(final Labels labels) {
    return labels.missingLabels.size();
  }

  public static final EmbeddedCode missingLabels(final Labels labels) {
    final EmbeddedCode code = EmbeddedCode.create();

    if (!labels.missingLabels.isEmpty()) {
      code.newline();
    }

    for (final String missingLabel : labels.missingLabels) {
      code.print(missingLabel + ": ");
    }

    return code;
  }

}
