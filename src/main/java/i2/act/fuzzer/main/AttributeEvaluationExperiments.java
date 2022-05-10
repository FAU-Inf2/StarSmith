package i2.act.fuzzer.main;

import i2.act.fuzzer.AttributeRule;
import i2.act.fuzzer.Node;
import i2.act.fuzzer.RandomFuzzer;
import i2.act.fuzzer.Specification;
import i2.act.fuzzer.SpecificationFactory;
import i2.act.util.FileUtil;
import i2.act.util.options.*;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

public final class AttributeEvaluationExperiments {

  public static final boolean PRINT_NUMBER_OF_ROUNDS = false;

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_HELP = "--help";
  private static final String OPTION_SEED = "--seed";
  private static final String OPTION_COUNT = "--count";
  private static final String OPTION_NAIVE = "--naive";
  private static final String OPTION_NO_SHALLOW_EVALUATION = "--noShallowEval";
  private static final String OPTION_ACCUMULATED = "--accumulated";
  private static final String OPTION_SANITY_CHECKS = "--sanityChecks";
  private static final String OPTION_OUT = "--out";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_HELP, false);

    argumentsParser.addOption(OPTION_SEED, false, true, "<random seed>");
    argumentsParser.addOption(OPTION_COUNT, false, true, "<number of programs>");

    argumentsParser.addOption(OPTION_NAIVE, false);
    argumentsParser.addOption(OPTION_NO_SHALLOW_EVALUATION, false);

    argumentsParser.addOption(OPTION_ACCUMULATED, false);

    argumentsParser.addOption(OPTION_SANITY_CHECKS, false);

    argumentsParser.addOption(OPTION_OUT, false, true, "<output file name>");
  }

  private static final void usage() {
    System.err.println("USAGE:");
    System.err.println(argumentsParser.usage("  "));
  }

  public static final void run(final SpecificationFactory specificationFactory,
      final int defaultMaxDepth, final String[] args) {
    ProgramArguments arguments = null;

    long seed = System.currentTimeMillis();
    int count = 1;

    try {
      arguments = argumentsParser.parseArgs(args);

      if (arguments.hasOption(OPTION_HELP)) {
        usage();
        System.exit(0);
      }

      seed = arguments.getLongOptionOr(OPTION_SEED, seed);

      if (arguments.hasOption(OPTION_COUNT)) {
        final String countValue = arguments.getOption(OPTION_COUNT);

        if (FuzzerLoop.VALUE_INFINITE_PROGRAMS.equalsIgnoreCase(countValue)) {
          count = FuzzerLoop.INFINITE_PROGRAMS;
        } else {
          count = arguments.getIntOptionOr(OPTION_COUNT, count);
        }
      }
    } catch (final InvalidProgramArgumentsException exception) {
      System.err.println("[!] " + exception.getMessage());
      usage();
      System.exit(1);
    }

    final boolean shallowAttributeEvaluation = !arguments.hasOption(OPTION_NO_SHALLOW_EVALUATION);

    final boolean accumulated = arguments.hasOption(OPTION_ACCUMULATED);
    final String outFileName = arguments.getOptionOr(OPTION_OUT, null);
    final boolean sanityChecks = arguments.hasOption(OPTION_SANITY_CHECKS);

    final BufferedWriter outWriter;
    {
      if (arguments.hasOption(OPTION_OUT)) {
        outWriter = FileUtil.openFileForWriting(arguments.getOption(OPTION_OUT), true);
        FileUtil.write("#NODES,ATTRS,TIME\n", outWriter);
      } else {
        outWriter = null;
      }
    }

    final Specification specification = specificationFactory.createSpecification();

    final List<Node> programs;
    {
      if (accumulated) {
        programs = new ArrayList<>(count);
      } else {
        programs = null;
      }
    }

    if (accumulated) {
      System.out.println("[i] generating programs");
    }

    long totalSize = 0;
    long totalNumberOfAttributes = 0;

    // generate programs
    {
      final long timeBefore = System.currentTimeMillis();

      for (int idx = 0; idx < count || count == FuzzerLoop.INFINITE_PROGRAMS; ++idx) {
        final long thisSeed = seed + idx;

        final Node program = generateProgram(
            specification, thisSeed, defaultMaxDepth, shallowAttributeEvaluation, true);

        final int size = program.size();
        final int numberOfAttributes = program.numberOfAttributes();

        totalSize += size;
        totalNumberOfAttributes += numberOfAttributes;

        if (accumulated) {
          programs.add(program);
        } else {
          // attribute evaluation
          {
            final long timeBeforeEvaluation = System.currentTimeMillis();
            evaluateAttributes(program, arguments.hasOption(OPTION_NAIVE));
            final long timeAfterEvaluation = System.currentTimeMillis();

            final long timeEvaluation = timeAfterEvaluation - timeBeforeEvaluation;

            System.out.format("=> attribute evaluation took %d ms (%d nodes, %d attributes)\n",
                timeEvaluation, size, numberOfAttributes);

            if (outWriter != null) {
              FileUtil.write(
                  String.format("%d,%d,%d\n", size, numberOfAttributes, timeEvaluation),
                  outWriter);
            }
          }

          // sanity check
          if (sanityChecks) {
            sanityCheck(program);
            System.out.println("passed sanity check");
          }
        }
      }

      final long timeAfter = System.currentTimeMillis();

      if (accumulated) {
        System.out.format("=> program generation took %d ms\n", timeAfter - timeBefore);
      }
    }

    if (accumulated) {
      System.out.println("[i] evaluating attributes");

      // evaluate attributes
      {
        final long timeBeforeEvaluation = System.currentTimeMillis();

        for (final Node program : programs) {
          evaluateAttributes(program, arguments.hasOption(OPTION_NAIVE));
        }

        final long timeAfterEvaluation = System.currentTimeMillis();

        final long timeEvaluation = timeAfterEvaluation - timeBeforeEvaluation;

        System.out.format("=> attribute evaluation took %d ms\n", timeEvaluation);

        if (outWriter != null) {
          FileUtil.write(
              String.format("%d,%d,%d\n", totalSize, totalNumberOfAttributes, timeEvaluation),
              outWriter);
        }
      }

      if (outWriter != null) {
        FileUtil.closeWriter(outWriter);
      }

      // sanity check
      if (sanityChecks) {
        System.out.println("[i] performing sanity check...");

        for (final Node program : programs) {
          sanityCheck(program);
        }

        System.out.println("=> passed");
      }
    } else {
      if (outWriter != null) {
        FileUtil.closeWriter(outWriter);
      }
    }
  }

  private static final Node generateProgram(final Specification specification, final long seed,
      final int maxDepth, final boolean shallowAttributeEvaluation,
      final boolean clearAttributeValues) {
    final RandomFuzzer fuzzer = RandomFuzzer.createFor(specification, seed,
            0.f, false, false, -1, -1, false, null, null);
    fuzzer.shallowAttributeEvaluation = shallowAttributeEvaluation;

    final Node program = fuzzer.generateProgram(maxDepth, false);

    if (clearAttributeValues) {
      program.clearAttributeValues(true);
    }

    return program;
  }

  private static final void evaluateAttributes(final Node program, final boolean naive) {
    if (naive) {
      evaluateAttributesLoopNaive(program);
    } else {
      program.evaluateAttributesLoop();
    }
  }

  private static final void sanityCheck(final Node program) {
    if (!program.allGuardsSatisfied(true)) {
      System.err.println("===[ INVALID NODES ]===");
      program.findInvalidNodes();
      System.err.println("=======================");

      throw new RuntimeException("program contains failing guards!");
    }
  }

  private static final void evaluateAttributesLoopNaive(final Node node) {
    int rounds = 0;

    boolean anotherRound;

    do {
      anotherRound = evaluateAttributesNaive(node);
      ++rounds;

    } while (anotherRound);

    if (PRINT_NUMBER_OF_ROUNDS) {
      System.out.println(rounds);
    }
  }

  private static final boolean evaluateAttributesNaive(final Node node) {
    boolean change = false;

    change |= evaluateOwnAttributesNaive(node);

    for (final Node child : node.getChildren()) {
      change |= evaluateAttributesNaive(child);
    }

    change |= evaluateOwnAttributesNaive(node);

    return change;
  }

  private static final boolean evaluateOwnAttributesNaive(final Node node) {
    boolean someRuleEvaluated = false;

    if (node.getProduction() != null) {
      final List<AttributeRule> attributeRules = node.getProduction().getAttributeRules();
      for (final AttributeRule attributeRule : attributeRules) {
        if (attributeRule.alreadyComputed(node)) {
          // attribute value has already been computed => do nothing
        } else {
          someRuleEvaluated |= attributeRule.evaluate(node, false);
        }
      }
    }

    return someRuleEvaluated;
  }

}
