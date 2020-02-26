package i2.act.errors.specification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InvalidLanguageSpecificationException extends RuntimeException {

  private final List<LanguageSpecificationError> languageSpecificationErrors;

  public InvalidLanguageSpecificationException(
      final LanguageSpecificationError languageSpecificationError) {
    this.languageSpecificationErrors = new ArrayList<LanguageSpecificationError>(1);
    this.languageSpecificationErrors.add(languageSpecificationError);
  }

  public InvalidLanguageSpecificationException(
      final List<LanguageSpecificationError> languageSpecificationErrors) {
    this.languageSpecificationErrors = languageSpecificationErrors;
  }

  public final List<LanguageSpecificationError> getLanguageSpecificationErrors() {
    return Collections.unmodifiableList(this.languageSpecificationErrors);
  }

  @Override
  public final String toString() {
    final StringBuilder builder = new StringBuilder();

    boolean first = true;
    for (final LanguageSpecificationError error : this.languageSpecificationErrors) {
      if (!first) {
        builder.append("\n");
      } else {
        first = false;
      }

      builder.append(error.toString());
    }

    return builder.toString();
  }

  @Override
  public final String getMessage() {
    return this.toString();
  }

}
