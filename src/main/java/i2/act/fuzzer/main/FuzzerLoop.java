package i2.act.fuzzer.main;

import i2.act.fuzzer.Node;
import i2.act.fuzzer.RandomFuzzer;
import i2.act.fuzzer.Specification;
import i2.act.fuzzer.SpecificationFactory;
import i2.act.util.ArgumentSplitter;
import i2.act.util.FileUtil;
import i2.act.util.ProcessExecutor;
import i2.act.util.options.*;

import java.io.BufferedWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FuzzerLoop {

  public static final String VALUE_INFINITE_PROGRAMS = "inf";
  public static final int INFINITE_PROGRAMS = -1;

  public static final int DEFAULT_BATCH_SIZE = 100_000;

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_HELP = "--help";

  private static final String OPTION_SEED = "--seed";
  private static final String OPTION_SEED_INC = "--seedInc";
  private static final String OPTION_COUNT = "--count";
  private static final String OPTION_FILE_NAME_PATTERN = "--out";
  private static final String OPTION_AST_FILE_NAME_PATTERN = "--ast";
  private static final String OPTION_SHORT_FORMAT = "--short";
  private static final String OPTION_FIND_BUGS = "--findBugs";
  private static final String OPTION_STATISTICS_FILE = "--stats";
  private static final String OPTION_DIAGNOSTICS_FILE = "--diagnostics";
  private static final String OPTION_ERROR_FILE = "--log";
  private static final String OPTION_SMALL_PROBABILITY = "--small";
  private static final String OPTION_SYNTAX_ONLY = "--syntaxOnly";
  private static final String OPTION_RESTART_ON_FAILURE = "--restartOnFailure";
  private static final String OPTION_TIMEOUT = "--timeout";
  private static final String OPTION_MAX_ALTERNATIVES = "--maxAlternatives";
  private static final String OPTION_DEBUG = "--debug";
  private static final String OPTION_SANITY_CHECKS = "--sanityChecks";

  private static final String OPTION_MAX_DEPTH = "--maxDepth";

  private static final String OPTION_BATCH_SIZE = "--batchSize";

  private static final String OPTION_USE_SPECIFIC_PATTERNS = "--specificFPs";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_HELP, false);

    argumentsParser.addOption(OPTION_SEED, false, true, "<random seed>");
    argumentsParser.addOption(OPTION_SEED_INC, false, true, "<increment of random seed>");
    argumentsParser.addOption(OPTION_COUNT, false, true, "<number of programs>");
    argumentsParser.addOption(OPTION_FILE_NAME_PATTERN, false, true,
        "<file name pattern for the generated programs>");
    argumentsParser.addOption(OPTION_AST_FILE_NAME_PATTERN, false, true,
        "<file name pattern for the ASTs of the generated programs>");
    argumentsParser.addOption(OPTION_SHORT_FORMAT, false);
    argumentsParser.addOption(OPTION_FIND_BUGS, false, true,
        "<command that checks for a bug>");
    argumentsParser.addOption(OPTION_STATISTICS_FILE, false, true,
        "<file name of the statistics file>");
    argumentsParser.addOption(OPTION_DIAGNOSTICS_FILE, false, true,
        "<file name of the diagnostics file>");
    argumentsParser.addOption(OPTION_ERROR_FILE, false, true,
        "<file name of the error file>");
    argumentsParser.addOption(OPTION_DEBUG, false);

    argumentsParser.addOption(OPTION_SANITY_CHECKS, false);

    argumentsParser.addOption(OPTION_MAX_DEPTH, false, true, "<max. program depth>");
    argumentsParser.addOption(
        OPTION_SMALL_PROBABILITY, false, true, "<probability to choose 'small' productions>");
    argumentsParser.addOption(OPTION_SYNTAX_ONLY, false);
    argumentsParser.addOption(OPTION_RESTART_ON_FAILURE, false);
    argumentsParser.addOption(
        OPTION_TIMEOUT, false, true, "<timeout in ms>");
    argumentsParser.addOption(
        OPTION_MAX_ALTERNATIVES, false, true, "<max. number of alternatives>");

    argumentsParser.addOption(OPTION_BATCH_SIZE, false, true, "<batch size");

    argumentsParser.addOption(OPTION_USE_SPECIFIC_PATTERNS, false);
  }

  private static final void usage() {
    System.err.println("USAGE:");
    System.err.println(argumentsParser.usage("  "));
  }

  public static final void generatePrograms(final SpecificationFactory specificationFactory,
      final int defaultMaxDepth, final String[] args) {
    ProgramArguments arguments = null;

    long seed = System.currentTimeMillis();
    int seedInc = 1;
    int count = 1;
    String fileNamePattern = null;
    String fileNamePatternASTs = null;
    boolean shortFormat = false;
    String findBugsCommand = null;
    String statsFileName = null;
    String diagnosticsFileName = null;
    String errorFileName = null;
    float smallProbability = 0.f;
    boolean syntaxOnly = false;
    boolean restartOnFailure = false;
    int maxAlternatives = -1;
    boolean debug = false;
    boolean sanityChecks = false;
    int maxDepth = defaultMaxDepth;
    int timeout = -1;
    int batchSize = DEFAULT_BATCH_SIZE;
    boolean useSpecificPatterns = false;

    try {
      arguments = argumentsParser.parseArgs(args);

      if (arguments.hasOption(OPTION_HELP)) {
        usage();
        System.exit(0);
      }

      seed = arguments.getLongOptionOr(OPTION_SEED, seed);
      seedInc = arguments.getIntOptionOr(OPTION_SEED_INC, seedInc);

      if (arguments.hasOption(OPTION_COUNT)) {
        final String countValue = arguments.getOption(OPTION_COUNT);
        if (VALUE_INFINITE_PROGRAMS.equalsIgnoreCase(countValue)) {
          count = INFINITE_PROGRAMS;
        } else {
          count = arguments.getIntOptionOr(OPTION_COUNT, count);
        }
      }

      fileNamePattern = arguments.getOptionOr(OPTION_FILE_NAME_PATTERN, fileNamePattern);
      fileNamePatternASTs =
          arguments.getOptionOr(OPTION_AST_FILE_NAME_PATTERN, fileNamePatternASTs);
      shortFormat = arguments.hasOption(OPTION_SHORT_FORMAT);
      findBugsCommand = arguments.getOptionOr(OPTION_FIND_BUGS, findBugsCommand);

      statsFileName = arguments.getOptionOr(OPTION_STATISTICS_FILE, statsFileName);
      diagnosticsFileName = arguments.getOptionOr(OPTION_DIAGNOSTICS_FILE, diagnosticsFileName);
      errorFileName = arguments.getOptionOr(OPTION_ERROR_FILE, errorFileName);
      smallProbability = arguments.getFloatOptionOr(OPTION_SMALL_PROBABILITY, smallProbability);
      syntaxOnly = arguments.hasOption(OPTION_SYNTAX_ONLY);
      restartOnFailure = arguments.hasOption(OPTION_RESTART_ON_FAILURE);
      timeout = arguments.getIntOptionOr(OPTION_TIMEOUT, timeout);
      maxAlternatives = arguments.getIntOptionOr(OPTION_MAX_ALTERNATIVES, maxAlternatives);
      debug = arguments.hasOption(OPTION_DEBUG);
      sanityChecks = arguments.hasOption(OPTION_SANITY_CHECKS);
      maxDepth = arguments.getIntOptionOr(OPTION_MAX_DEPTH, maxDepth);
      batchSize =
          arguments.getIntOptionOr(OPTION_BATCH_SIZE, batchSize);
      useSpecificPatterns = arguments.hasOption(OPTION_USE_SPECIFIC_PATTERNS);
    } catch (final InvalidProgramArgumentsException exception) {
      System.err.println("[!] " + exception.getMessage());
      usage();
      System.exit(1);
    }

    if (fileNamePattern == null && findBugsCommand != null) {
      System.err.println("[!] cannot find bugs if output path is not specified");
      usage();
      System.exit(1);
    }

    generatePrograms(specificationFactory, maxDepth, seed, seedInc, count, fileNamePattern,
        fileNamePatternASTs, shortFormat, findBugsCommand, statsFileName, diagnosticsFileName,
        errorFileName, smallProbability, syntaxOnly, restartOnFailure, timeout,
        maxAlternatives, debug, sanityChecks, batchSize, useSpecificPatterns);
  }

  public static final Node generatePrograms(final SpecificationFactory specificationFactory,
      final int maxDepth, final long seed, final int seedInc, final int numberOfPrograms,
      final String fileNamePattern, final String fileNamePatternASTs, final boolean shortFormat,
      final String findBugsCommand, final String statsFileName, final String diagnosticsFileName,
      final String errorFileName, final double smallProbability, final boolean syntaxOnly,
      final boolean restartOnFailure, final int timeout, final int maxAlternatives,
      final boolean debug, final boolean sanityChecks, final int batchSize,
      final boolean useSpecificPatterns) {

    final BufferedWriter statsWriter;
    {
      if (statsFileName != null) {
        statsWriter = FileUtil.openFileForWriting(statsFileName, true);
      } else {
        statsWriter = null;
      }
    }

    final BufferedWriter diagnosticsWriter;
    {
      if (diagnosticsFileName != null) {
        diagnosticsWriter = FileUtil.openFileForWriting(diagnosticsFileName, true);
      } else {
        diagnosticsWriter = null;
      }
    }

    final BufferedWriter errorWriter;
    {
      if (errorFileName != null) {
        errorWriter = FileUtil.openFileForWriting(errorFileName, true);
      } else {
        errorWriter = null;
      }
    }

    final Specification specification = specificationFactory.createSpecification();

    Node result = null;

    for (int idx = 0; idx < numberOfPrograms || numberOfPrograms == INFINITE_PROGRAMS; ++idx) {
      final long thisSeed = seed + (idx * seedInc);

      final RandomFuzzer fuzzer = RandomFuzzer.createFor(specification, thisSeed,
              smallProbability, syntaxOnly, restartOnFailure, timeout, maxAlternatives, debug,
              diagnosticsWriter, errorWriter);
      fuzzer.useSpecificPatterns = useSpecificPatterns;

      final long timeBefore = System.currentTimeMillis();

      final Node program;

      try {
        program = fuzzer.generateProgram(maxDepth);
      } catch (final Exception exception) {
        if (diagnosticsWriter != null) {
          FileUtil.closeWriter(diagnosticsWriter);
        }

        if (errorWriter != null) {
          FileUtil.closeWriter(errorWriter);
        }

        throw exception;
      }

      if (sanityChecks) {
        program.clearAttributeValues(true);
        program.evaluateAttributesLoop();

        if (!program.allGuardsSatisfied(true)) {
          System.err.println("===[ INVALID NODES ]===");
          program.findInvalidNodes();
          System.err.println("=======================");

          throw new RuntimeException("program contains failing guards!");
        }
      }

      result = program;

      final long timeAfter = System.currentTimeMillis();

      final String fileNameProgram;

      // write program to disk or stdout
      if (fileNamePattern == null) {
        fileNameProgram = null;
        System.out.println(program.printCode());
      } else {
        fileNameProgram =
            replaceFileNamePattern(fileNamePattern, maxDepth, idx, thisSeed, batchSize);

        final BufferedWriter writer = FileUtil.openFileForWriting(fileNameProgram, true);
        FileUtil.write(program.printCode(), writer);
        FileUtil.closeWriter(writer);
      }

      // write statistics to file (if enabled)
      if (statsWriter != null) {
        final int programSize = program.size();
        final long time = timeAfter - timeBefore;
        final int programDepth = program.depth();

        FileUtil.write(String.format("%d, %d, %d\n", programSize, time, programDepth), statsWriter);
        FileUtil.flushWriter(statsWriter);
      }

      // flush diagnostics writer (if enabled)
      if (diagnosticsFileName != null) {
        FileUtil.flushWriter(diagnosticsWriter);
      }

      boolean writeAST = (fileNamePatternASTs != null);

      // check if generated program triggers a bug
      if (findBugsCommand != null) {
        assert (fileNameProgram != null);

        final String[] findBugsCommandLine = ArgumentSplitter.splitArguments(findBugsCommand);
        final String[] initialCheckCommandLine =
            ArgumentSplitter.appendArgument(findBugsCommandLine, fileNameProgram);

        if (ProcessExecutor.executeAndCheck(initialCheckCommandLine)) {
          // generated program does not trigger a bug
          System.out.println("[i] program does not trigger a bug => discard program");
          FileUtil.deleteFile(fileNameProgram);

          // do not write AST to file
          writeAST = false;
        } else {
          // generated program triggers a bug \o/
          System.out.println("[i] program triggers a bug => keep program");
        }
      }

      // write AST to file (if enabled)
      if (writeAST) {
        assert (fileNamePatternASTs != null);
        final String fileName =
            replaceFileNamePattern(fileNamePatternASTs, maxDepth, idx, thisSeed, batchSize);

        final BufferedWriter writer = FileUtil.openFileForWriting(fileName, true);
        FileUtil.write(program.serialize(shortFormat), writer);
        FileUtil.closeWriter(writer);
      }
    }

    // close writers
    {
      if (statsWriter != null) {
        FileUtil.closeWriter(statsWriter);
      }

      if (diagnosticsWriter != null) {
        FileUtil.closeWriter(diagnosticsWriter);
      }

      if (errorWriter != null) {
        FileUtil.closeWriter(errorWriter);
      }
    }

    return result;
  }

  private static final String replaceFileNamePattern(final String fileNamePattern,
      final int maxDepth, final int index, final long seed, final int batchSize) {
    return fileNamePattern
        .replaceAll(Pattern.quote("#{MAX_DEPTH}"), Matcher.quoteReplacement("" + maxDepth))
        .replaceAll(Pattern.quote("#{IDX}"), Matcher.quoteReplacement("" + index))
        .replaceAll(Pattern.quote("#{SEED}"), Matcher.quoteReplacement("" + seed))
        .replaceAll(Pattern.quote("#{BATCH}"), Matcher.quoteReplacement("" + (index / batchSize)));
  }

}
