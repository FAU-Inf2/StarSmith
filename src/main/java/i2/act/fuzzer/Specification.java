package i2.act.fuzzer;

import i2.act.fuzzer.regex.ast.RegularExpression;
import i2.act.fuzzer.regex.ast.visitors.RandomStringGenerator;
import i2.act.fuzzer.regex.parser.RegExParser;
import i2.act.gengraph.properties.MinHeightComputation;
import i2.act.gengraph.properties.RecursiveProductionsComputation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Specification {

  public static final boolean COMPUTE_MAY_GENERATE_PRODUCTIONS = false;
  public static final boolean COMPUTE_MUST_GENERATE_PRODUCTIONS = false;
  public static final boolean COMPUTE_MAY_GENERATE_EDGES = false;
  public static final boolean COMPUTE_MUST_GENERATE_EDGES = false;

  private final List<Class> classes;
  private final Map<String, Class> classNames;
  private final Class rootClass;

  private final Map<Integer, Class> classIds;

  public final int defaultMaxRecursionDepth;

  public Specification(final Class rootClass, final Class[] classes,
      final int defaultMaxRecursionDepth, final long seed) {
    this.rootClass = rootClass;
    this.classes = Collections.unmodifiableList(java.util.Arrays.asList(classes));

    // initialize map of class names
    {
      this.classNames = new HashMap<String, Class>();
      for (final Class _class : classes) {
        this.classNames.put(_class.name, _class);
      }
    }

    this.defaultMaxRecursionDepth = defaultMaxRecursionDepth;

    instantiateLiteralClasses(seed);

    // compute min heights and determine recursive productions
    computeProperties();

    this.classIds = new HashMap<Integer, Class>();
    gatherClassIds();
  }

  private final void computeProperties() {
    // compute min heights
    MinHeightComputation.computeMinHeights(this);

    // determine recursive productions
    RecursiveProductionsComputation.determineRecursiveProductions(this);
  }

  public final List<Class> getClasses() {
    return this.classes;
  }

  public final Class getRootClass() {
    return this.rootClass;
  }

  private final void instantiateLiteralClasses(final long seed) {
    final RegExParser parser = new RegExParser();
    final RandomStringGenerator randomStringGenerator = new RandomStringGenerator(seed);

    for (final Class _class : this.classes) {
      if (_class.isLiteralClass()) {
        final String regularExpressionString = _class.getRegularExpression();
        assert (regularExpressionString != null);

        final RegularExpression regularExpression = parser.parse(regularExpressionString);
        final int numberOfLiterals = _class.getLiteralCount();

        final List<String> randomStrings =
            randomStringGenerator.generateStrings(regularExpression, numberOfLiterals);
        int literalId = 0;
        for (final String randomString : randomStrings) {
          Production.createLiteralProduction(_class, randomString, literalId++);
        }
      }
    }
  }

  private final void gatherClassIds() {
    for (final Class _class : this.classes) {
      this.classIds.put(_class.id, _class);
    }
  }

  public final Class getClassByName(final String className) {
    return this.classNames.get(className);
  }

  public final Production getProductionByQualifiedName(final String className,
      final String productionName) {
    final Class _class = this.classNames.get(className);
    
    if (_class == null) {
      throw new RuntimeException(
          String.format("unknown class '%s'", className));
    }

    final Production production = _class.getProductionByName(productionName);

    if (production == null) {
      throw new RuntimeException(
          String.format("unknown production '%s' in class '%s'", productionName, className));
    }

    return production;
  }

  public final Production getProductionByQualifiedName(final String qualifiedName) {
    final String[] nameElements = qualifiedName.split("::");

    if (nameElements.length != 2) {
      throw new RuntimeException(
          String.format("invalid qualified production name '%s'", qualifiedName));
    }

    final String className = nameElements[0];
    final String productionName = nameElements[1];

    return getProductionByQualifiedName(className, productionName);
  }

  public final Class getClassById(final int id) {
    return this.classIds.get(id);
  }

}
