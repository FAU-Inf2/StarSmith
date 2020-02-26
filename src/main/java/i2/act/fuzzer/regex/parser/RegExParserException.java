package i2.act.fuzzer.regex.parser;

public final class RegExParserException extends RuntimeException {
  
  private final char[] characters;
  private final int position;

  public RegExParserException(final char[] characters, final int position) {
    super(String.format(
        "invalid regular expression:\n  %s\n  %" + (position + 1) + "s",
        String.valueOf(characters), "^"));
    this.characters = characters;
    this.position = position;
  }

  public final char[] getCharacters() {
    return this.characters;
  }
  
  public final int getPosition() {
    return this.position;
  }

}
