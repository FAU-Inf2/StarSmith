package i2.act.lala.parser;

import i2.act.errors.RPGException;
import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.errors.specification.parser.ErrorListener;
import i2.act.lala.ast.LaLaSpecification;
import i2.act.lala.info.SourceFile;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public final class LaLaParser {

  private final SourceFile sourceFile;

  private final ErrorListener errorListener;

  private final i2.act.antlr.LaLaLexer lexer;
  private final i2.act.antlr.LaLaParser parser;

  private LaLaParser(final SourceFile sourceFile, final ErrorListener errorListener,
      final i2.act.antlr.LaLaLexer lexer, final i2.act.antlr.LaLaParser parser) {
    this.sourceFile = sourceFile;
    this.errorListener = errorListener;
    this.lexer = lexer;
    this.parser = parser;
  }

  private static final LaLaParser constructParser(
      final CharStream inputStream, final SourceFile sourceFile) {
    final ErrorListener errorListener = new ErrorListener();

    final i2.act.antlr.LaLaLexer lexer = new i2.act.antlr.LaLaLexer(inputStream);
    {
      lexer.removeErrorListeners();
      lexer.addErrorListener(errorListener);
    }

    final CommonTokenStream tokenStream = new CommonTokenStream(lexer);

    final i2.act.antlr.LaLaParser parser = new i2.act.antlr.LaLaParser(tokenStream);
    {
      parser.removeErrorListeners();
      parser.addErrorListener(errorListener);
    }
    

    return new LaLaParser(sourceFile, errorListener, lexer, parser);
  }

  public static final LaLaParser constructParser(
      final SourceFile sourceFile) {
    final String inputFileName = sourceFile.getFileName();
    try {
      final CharStream inputStream = CharStreams.fromStream(new FileInputStream(inputFileName));
      return constructParser(inputStream, sourceFile);
    } catch (final IOException exception) {
      throw new RPGException("unable to read input file: " + exception.getMessage(), exception);
    }
  }

  public static final LaLaParser constructParser(
      final File inputFile) {
    final String inputFileName = inputFile.getName();
    final SourceFile sourceFile = new SourceFile(inputFileName);
    try {
      final CharStream inputStream = CharStreams.fromStream(new FileInputStream(inputFile));
      return constructParser(inputStream, sourceFile);
    } catch (final IOException exception) {
      throw new RPGException("unable to read input file: " + exception.getMessage(), exception);
    }
  }

  public final LaLaSpecification parseLanguageSpecification()
      throws InvalidLanguageSpecificationException {
    final i2.act.antlr.LaLaParser.LanguageSpecificationContext
        languageSpecificationContext = this.parser.languageSpecification();

    if (!this.errorListener.successful()) {
      final List<LanguageSpecificationError> languageSpecificationErrors =
          this.errorListener.getLanguageSpecificationErrors();
      throw new InvalidLanguageSpecificationException(languageSpecificationErrors);
    }

    final LaLaBuilder builder = new LaLaBuilder(this.sourceFile);
    return builder.visitLanguageSpecification(languageSpecificationContext);
  }

}
