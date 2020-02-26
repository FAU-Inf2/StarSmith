package i2.act.errors.specification.parser;

import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.lala.info.SourcePosition;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ErrorListener extends BaseErrorListener {

  private final List<LanguageSpecificationError> languageSpecificationErrors;

  public ErrorListener() {
    this.languageSpecificationErrors = new ArrayList<LanguageSpecificationError>();
  }

  @Override
  public final void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol,
      final int line, final int charPosition,
      final String message, final RecognitionException recognitionException) {
    final SourcePosition position = new SourcePosition(line, charPosition);

    final LanguageSpecificationError languageSpecificationError =
        new LanguageSpecificationError(position, message);
    this.languageSpecificationErrors.add(languageSpecificationError);
  }

  public final boolean successful() {
    return this.languageSpecificationErrors.isEmpty();
  }

  public final List<LanguageSpecificationError> getLanguageSpecificationErrors() {
    return Collections.unmodifiableList(this.languageSpecificationErrors);
  }

}
