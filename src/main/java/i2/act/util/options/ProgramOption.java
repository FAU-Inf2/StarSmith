package i2.act.util.options;

public final class ProgramOption {

  public final String optionKey;
  public final boolean required;
  public final boolean takesArgument;
  public final String argumentDescription;

  public ProgramOption(final String optionKey, final boolean required, final boolean takesArgument,
      final String argumentDescription) {
    this.optionKey = optionKey;
    this.required = required;
    this.takesArgument = takesArgument;
    this.argumentDescription = argumentDescription;
  }

}
