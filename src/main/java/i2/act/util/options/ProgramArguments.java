package i2.act.util.options;

import java.util.LinkedHashMap;

public final class ProgramArguments {

  private final LinkedHashMap<String, String> options;

  public ProgramArguments() {
    this.options = new LinkedHashMap<String, String>();
  }

  public final boolean hasOption(final String option) {
    return this.options.containsKey(option);
  }

  public final String getOption(final String option) {
    return this.options.get(option);
  }

  public final String getOptionOr(final String option, final String or) {
    if (this.options.containsKey(option)) {
      return this.options.get(option);
    } else {
      return or;
    }
  }

  public final int getIntOption(final String option) {
    final String value = this.options.get(option);

    try {
      return Integer.valueOf(value);
    } catch (final Throwable throwable) {
      throw new InvalidProgramArgumentsException(
          String.format("illegal value for option '%s': %s", option, value));
    }
  }

  public final int getIntOptionOr(final String option, final int or) {
    if (this.options.containsKey(option)) {
      return getIntOption(option);
    } else {
      return or;
    }
  }

  public final long getLongOption(final String option) {
    final String value = this.options.get(option);

    try {
      return Long.valueOf(value);
    } catch (final Throwable throwable) {
      throw new InvalidProgramArgumentsException(
          String.format("illegal value for option '%s': %s", option, value));
    }
  }

  public final long getLongOptionOr(final String option, final long or) {
    if (this.options.containsKey(option)) {
      return getLongOption(option);
    } else {
      return or;
    }
  }

  public final float getFloatOption(final String option) {
    final String value = this.options.get(option);

    try {
      return Float.valueOf(value);
    } catch (Exception ex) {
      throw new InvalidProgramArgumentsException(
          String.format("illegal value for option '%s': %s", option, value));
    }
  }

  public final float getFloatOptionOr(final String option, final float or) {
    if (this.options.containsKey(option)) {
      return getFloatOption(option);
    } else {
      return or;
    }
  }

  public final void requireOption(final String option) {
    if (!hasOption(option)) {
      throw new InvalidProgramArgumentsException(
        String.format("missing option '%s'", option));
    }
  }

  final void addOption(final String option) {
    addOption(option, null);
  }

  final void addOption(final String option, final String value) {
    this.options.put(option, value);
  }
}
