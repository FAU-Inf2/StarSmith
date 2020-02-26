package i2.act;

import i2.act.errors.RPGException;
import i2.act.errors.specification.InvalidLanguageSpecificationException;
import i2.act.errors.specification.LanguageSpecificationError;
import i2.act.gengraph.GeneratorGraph;
import i2.act.gengraph.GeneratorGraphNode.ProductionNode;
import i2.act.gengraph.properties.*;
import i2.act.gengraph.properties.dominance.*;
import i2.act.lala.ast.LaLaSpecification;
import i2.act.lala.ast.ProductionDeclaration;
import i2.act.lala.ast.visitors.*;
import i2.act.lala.info.SourceFile;
import i2.act.lala.parser.LaLaParser;
import i2.act.util.FileUtil;
import i2.act.util.options.*;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

public final class StarSmithTranslate {

  public static final int DEFAULT_MAX_PROGRAM_DEPTH = 11;

  public static final boolean ALL_FEATURES_ENABLED_BY_DEFAULT = false;

  private static final ProgramArgumentsParser argumentsParser;

  private static final String OPTION_SPECIFICATION = "--spec";
  private static final String OPTION_PRETTY_PRINT = "--pretty";
  private static final String OPTION_PRINT_GENERATOR_GRAPH = "--printGG";
  private static final String OPTION_PRINT_DOMINATOR_TREE = "--printDT";
  private static final String OPTION_PRINT_ATTRIBUTE_DEPENDENCIES = "--printDependencies";
  private static final String OPTION_PRINT_DEPTHS = "--printDepths";
  private static final String OPTION_PRINT_ISSI_GRAPHS = "--printISSI";
  private static final String OPTION_MAX_DEPTH = "--maxDepth";
  private static final String OPTION_TRANSLATE_TO_JAVA = "--toJava";
  private static final String OPTION_PACKAGE = "--package";
  private static final String OPTION_FEATURES = "--features";
  private static final String OPTION_ALL_FEATURES = "--allFeatures";

  static {
    argumentsParser = new ProgramArgumentsParser();

    argumentsParser.addOption(OPTION_SPECIFICATION, true, true, "<path to LaLa specification>");
    argumentsParser.addOption(OPTION_MAX_DEPTH, false, true, "<max. program depth>");
    argumentsParser.addOption(OPTION_PRETTY_PRINT, false);
    argumentsParser.addOption(OPTION_PRINT_GENERATOR_GRAPH, false);
    argumentsParser.addOption(OPTION_PRINT_DOMINATOR_TREE, false);
    argumentsParser.addOption(OPTION_PRINT_ATTRIBUTE_DEPENDENCIES, false);
    argumentsParser.addOption(OPTION_PRINT_DEPTHS, false);
    argumentsParser.addOption(OPTION_PRINT_ISSI_GRAPHS, false);
    argumentsParser.addOption(OPTION_TRANSLATE_TO_JAVA, false, true, "<path to Java file>");
    argumentsParser.addOption(OPTION_PACKAGE, false, true, "<package of the generated class>");
    argumentsParser.addOption(OPTION_FEATURES, false, true, "<comma separated list of features>");
    argumentsParser.addOption(OPTION_ALL_FEATURES, false);
  }

  private static final void usage() {
    System.err.format("USAGE: java %s\n", StarSmithTranslate.class.getSimpleName());
    System.err.println(argumentsParser.usage("  "));
  }

  private static final Set<String> getFeatureSet(final String featureString,
      final boolean allFeatures) {
    if (allFeatures) {
      return null;
    }

    if (featureString == null) {
      if (ALL_FEATURES_ENABLED_BY_DEFAULT) {
        return null;
      } else {
        return new HashSet<String>();
      }
    }

    final Set<String> featureSet = new HashSet<>();

    for (final String feature : featureString.split(",")) {
      featureSet.add(feature.trim());
    }

    return featureSet;
  }

  public static final void main(final String[] args) {
    ProgramArguments arguments = null;
  
    int maxDepth = DEFAULT_MAX_PROGRAM_DEPTH;

    try {
      arguments = argumentsParser.parseArgs(args);

      if (arguments.hasOption(OPTION_MAX_DEPTH)) {
        maxDepth = arguments.getIntOption(OPTION_MAX_DEPTH);
      }
    } catch (final Exception exception) {
      System.err.println("[!] " + exception.getMessage());
      usage();
      System.exit(1);
    }

    final String inputFileName = arguments.getOption(OPTION_SPECIFICATION);

    final StarSmithTranslate starSmithTranslate = new StarSmithTranslate(inputFileName, maxDepth);

    final boolean prettyPrint = arguments.hasOption(OPTION_PRETTY_PRINT);
    final boolean printGeneratorGraph = arguments.hasOption(OPTION_PRINT_GENERATOR_GRAPH);
    final boolean printDominatorTree = arguments.hasOption(OPTION_PRINT_DOMINATOR_TREE);
    final boolean printAttributeDependencies =
        arguments.hasOption(OPTION_PRINT_ATTRIBUTE_DEPENDENCIES);
    final boolean printDepths = arguments.hasOption(OPTION_PRINT_DEPTHS);
    final boolean printISSI = arguments.hasOption(OPTION_PRINT_ISSI_GRAPHS);
    final String toJavaFileName = arguments.getOptionOr(OPTION_TRANSLATE_TO_JAVA, null);
    final String packageName = arguments.getOptionOr(OPTION_PACKAGE, null);
    final Set<String> features =
        getFeatureSet(
            arguments.getOptionOr(OPTION_FEATURES, null),
            arguments.hasOption(OPTION_ALL_FEATURES));

    try {
      starSmithTranslate.run(prettyPrint, printGeneratorGraph, printDominatorTree,
          printAttributeDependencies, printDepths, toJavaFileName, printISSI, packageName,
          features);
    } catch (final InvalidLanguageSpecificationException exception) {
      System.err.println("[!] invalid language specification");
      System.err.println(exception.getMessage());
      System.exit(1);
    } catch (final RPGException exception) {
      System.err.println("[!] an error occured: " + exception.getMessage());
      exception.printStackTrace(System.err);
      System.exit(1);
    }
  }

  // ---------------------------------------------------------------------------------

  private final SourceFile sourceFile;

  private final int maxDepth;

  public StarSmithTranslate(final String inputFileName, final int maxDepth) {
    this.sourceFile = new SourceFile(inputFileName);
    this.maxDepth = maxDepth;
  }

  public final void run(final boolean prettyPrint, final boolean printGeneratorGraph,
      final boolean printDominatorTree, final boolean printAttributeDependencies,
      final boolean printDepths, final String toJavaFileName, final boolean printISSI,
      final String packageName, final Set<String> features) {
    final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));

    final LaLaParser parser =
        LaLaParser.constructParser(this.sourceFile);

    final LaLaSpecification specification = parser.parseLanguageSpecification();

    // semantic analysis
    SemanticAnalysis.analyze(specification);
    AttributeCheck.analyze(specification);
    StrongNonCyclicityCheck.analyze(specification, printISSI);

    // warn on unused classes
    UnusedClasses.emitWarnings(specification);

    // pretty print language specification
    if (prettyPrint) {
      final PrettyPrinter prettyPrinter = new PrettyPrinter();
      prettyPrinter.prettyPrint(specification, writer);
    }

    // print attribute dependency graph as Dot
    if (printAttributeDependencies) {
      final AttributeDependenciesToDot printer = new AttributeDependenciesToDot();
      specification.accept(printer, writer);
    }

    // generate generator graph
    final GeneratorGraph generatorGraph = GeneratorGraph.fromAST(specification);

    // compute depths
    MinHeightComputation.computeMinHeights(generatorGraph);

    // check that each production has a sub-tree with finite height
    for (final ProductionNode production : generatorGraph.getProductionNodes()) {
      if (production.getMinHeight() >= Integer.MAX_VALUE) {
        LanguageSpecificationError.fail((ProductionDeclaration) production,
            String.format("production '%s' does not have finite height", production.getName()));
      }
    }

    // print depths (if enabled)
    if (printDepths) {
      generatorGraph.printMinHeights();
    }

    // print generator graph as dot
    if (printGeneratorGraph) {
      generatorGraph.printAsDot(writer);
    }

    // print dominator tree as dot
    if (printDominatorTree) {
      final DominatorTree dominatorTree = DominatorComputation.computeDominators(specification);
      dominatorTree.printAsDot(writer);
    }

    if (toJavaFileName != null) {
      final BufferedWriter javaFileWriter;
      final String javaClassName;
      {
        if (toJavaFileName.equals("-")) {
          javaFileWriter = writer;
          javaClassName = "LanguageSpecification";
        } else {
          javaFileWriter = FileUtil.openFileForWriting(toJavaFileName);
          javaClassName = FileUtil.getStrippedBaseName(toJavaFileName);
        }
      }

      final GenerateJavaSpec javaSpecGenerator = new GenerateJavaSpec(javaClassName,
          this.maxDepth, packageName, features);
      javaSpecGenerator.visit(specification, javaFileWriter);

      FileUtil.flushWriter(javaFileWriter);
    }

    FileUtil.closeWriter(writer);
  }

}
