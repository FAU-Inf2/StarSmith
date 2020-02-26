package i2.act.fuzzer.main;

import i2.act.fuzzer.Node;
import i2.act.fuzzer.Specification;
import i2.act.fuzzer.SpecificationFactory;
import i2.act.fuzzer.deserialization.ASTDeserializer;
import i2.act.util.options.*;

public final class Deserialization {

  public static final boolean ADD_MISSING_LITERAL_PRODUCTIONS = true;

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_HELP = "--help";
  private static final String OPTION_AST_FILE_NAME = "--ast";
  private static final String OPTION_PRINT_CODE = "--printCode";
  private static final String OPTION_PRINT_DOT = "--printDot";
  private static final String OPTION_SHORT_FORMAT = "--short";
  private static final String OPTION_DEBUG = "--debug";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_HELP, false);

    argumentsParser.addOption(OPTION_AST_FILE_NAME, true, true,
        "<file name of the serialized AST>");
    argumentsParser.addOption(OPTION_SHORT_FORMAT, false);
    argumentsParser.addOption(OPTION_PRINT_CODE, false);
    argumentsParser.addOption(OPTION_PRINT_DOT, false);
    argumentsParser.addOption(OPTION_DEBUG, false);
  }

  private static final void usage() {
    System.err.println("USAGE:");
    System.err.println(argumentsParser.usage("  "));
  }

  public static final void deserialization(final SpecificationFactory specificationFactory,
      final int defaultMaxDepth, final String[] args) {
    ProgramArguments arguments = null;

    String fileNameAST = null;
    boolean shortFormat = false;
    boolean printCode = true;
    boolean printDot = false;
    boolean debug = false;

    try {
      arguments = argumentsParser.parseArgs(args);

      if (arguments.hasOption(OPTION_HELP)) {
        usage();
        System.exit(0);
      }

      fileNameAST = arguments.getOptionOr(OPTION_AST_FILE_NAME, fileNameAST);
      shortFormat = arguments.hasOption(OPTION_SHORT_FORMAT);
      printCode = arguments.hasOption(OPTION_PRINT_CODE);
      printDot = arguments.hasOption(OPTION_PRINT_DOT);
      debug = arguments.hasOption(OPTION_DEBUG);
    } catch (final InvalidProgramArgumentsException exception) {
      System.err.println("[!] " + exception.getMessage());
      usage();
      System.exit(1);
    }

    final Specification specification = specificationFactory.createSpecification();

    Node node = ASTDeserializer.parseFile(
        fileNameAST, specification, shortFormat, ADD_MISSING_LITERAL_PRODUCTIONS);
    node.evaluateAttributesLoop();

    if (printCode) {
      System.out.println(node.printCode());
    }

    if (printDot) {
      System.out.println(node.toDot(true));
    }
  }

}
