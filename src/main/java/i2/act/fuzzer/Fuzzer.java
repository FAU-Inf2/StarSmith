package i2.act.fuzzer;

import i2.act.util.FileUtil;
import i2.act.util.Pair;

import java.io.BufferedWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Fuzzer {

  public static final boolean PRINT_DURATION = true;

  public static final boolean DETERMINE_EXCEPTIONAL_NODES = true;

  public static final Set<Integer> DEBUG_PRINTS_AT = new HashSet<Integer>(Arrays.asList(
      new Integer[] { }
  ));

  public static final class Candidate extends Pair<Node, List<FailPattern>> {

    public Candidate(final Node node, final List<FailPattern> failPatterns) {
      super(node, failPatterns);
    }

    @Override
    public final String toString() {
      return String.format("(%s<%x>, %s)",
          getFirst().getNodeClass().getName(), getFirst().hashCode(), getSecond());
    }

  }

  public static interface ProductionSelection {

    public Production choose(final Node node, final List<Production> applicableProductions);

  }

  public static enum FuzzingResult {

    FUZZ_SUCCESS("SUCCESS"),
    FUZZ_FAIL("FAIL"),
    FUZZ_HEIGHT_LIMIT("HEIGHT_LIMIT");

    private final String stringRepresentation;

    private FuzzingResult(final String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }

    @Override
    public final String toString() {
      return this.stringRepresentation;
    }

  }

  public static final class TimeoutException extends RuntimeException {

    public TimeoutException(final long duration) {
      super(String.format("timeout after %d ms", duration));
    }

  }

  public static final class FuzzingFailedException extends RuntimeException {

    public FuzzingFailedException(final String message) {
      super(message);
    }

  }

  // -----------------------------------------------------------------------------------------------

  private final Specification specification;

  private final ProductionSelection productionSelection;

  private final boolean syntaxOnly;

  private final boolean restartOnFailure;

  private final int timeout;

  private final int maxAlternatives;

  private final boolean debug;

  private final BufferedWriter diagnosticsWriter;

  private final BufferedWriter errorWriter;

  public boolean useSpecificPatterns = false;

  public boolean shallowAttributeEvaluation = true;

  public Fuzzer(
      final Specification specification,
      final ProductionSelection productionSelection,
      final boolean syntaxOnly,
      final boolean restartOnFailure,
      final int timeout,
      final int maxAlternatives,
      final boolean debug,
      final BufferedWriter diagnosticsWriter,
      final BufferedWriter errorWriter) {
    this.specification = specification;
    this.productionSelection = productionSelection;
    this.syntaxOnly = syntaxOnly;
    this.restartOnFailure = restartOnFailure;
    this.timeout = timeout;
    this.maxAlternatives = maxAlternatives;
    this.debug = debug;
    this.diagnosticsWriter = diagnosticsWriter;
    this.errorWriter = errorWriter;
  }

  public final Node generateProgram(final int maxRecursionDepth) {
    return generateProgram(maxRecursionDepth, PRINT_DURATION);
  }

  public final Node generateProgram(final int maxRecursionDepth, final boolean printDuration) {
    return generateProgram(maxRecursionDepth, printDuration, null);
  }

  public final Node generateProgram(final int maxRecursionDepth, final boolean printDuration,
      final Node expected) {
    final int allowedWidth = this.specification.getRootClass().getMaxWidth();

    final Node rootNode =
        this.specification.getRootClass().createNode(null, -1, allowedWidth, expected);

    if (generateTree(rootNode, maxRecursionDepth, printDuration)) {
      return rootNode;
    } else {
      throw new FuzzingFailedException("fuzzing failed");
    }
  }

  public final boolean generateSubTree(final Node subRootNode) {
    return generateSubTree(subRootNode, this.specification.defaultMaxRecursionDepth,
        PRINT_DURATION);
  }

  public final boolean generateSubTree(final Node subRootNode, final int maxRecursionDepth,
      final boolean printDuration) {
    return generateTree(subRootNode, maxRecursionDepth, printDuration);
  }

  private final boolean generateTree(final Node rootNode, final int maxRecursionDepth,
      final boolean printDuration) {
    final long startTime = System.currentTimeMillis();

    this.programRootNode = rootNode;

    while (true) {
      Node.resetIdCounter();
      this.fuzzCount = 0;
      
      // setup counters for timeout checks
      {
        this.deadlineCounter = 0;

        if (this.timeout < 0) {
          this.deadline = -1;
        } else {
          this.deadline = System.currentTimeMillis() + this.timeout;
        }
      }

      this.startTime = System.currentTimeMillis();

      FuzzingResult fuzzingResult;
      int numberOfAlternatives = 0;
      
      try {
        final Pair<FuzzingResult, Integer> returnValue =
            fuzz(rootNode, new ArrayList<>(), maxRecursionDepth, 0, this.maxAlternatives);

        fuzzingResult = returnValue.getFirst();
        numberOfAlternatives = returnValue.getSecond();
      } catch (final Exception | AssertionError exception) {
        System.err.println("[i] " + exception.getMessage());
        exception.printStackTrace();

        if (this.errorWriter != null) {
          FileUtil.write(
              String.format("%s | %s\n", getTimeStamp(), exception.getMessage()), this.errorWriter);
        }

        fuzzingResult = FuzzingResult.FUZZ_FAIL;
      }

      if (fuzzingResult != FuzzingResult.FUZZ_SUCCESS) {
        rootNode.clearFailPatterns();
        rootNode.deconstructButKeepInheritedAttributeValues();

        if (this.restartOnFailure) {
          System.err.println("[i] fuzzing failed -> restart");
          continue;
        } else {
          return false;
        }
      }

      if (printDuration) {
        final long endTime = System.currentTimeMillis();
        System.err.format("[i] program generation took %d ms (and %d alternatives)\n",
            endTime - startTime, numberOfAlternatives);
      }

      return true;
    }
  }

  private long startTime;
  private long deadline;
  private long deadlineCounter;

  private Node programRootNode;
  private int fuzzCount;

  private final Pair<FuzzingResult, Integer> fuzz(final Node rootNode,
      final List<FailPattern> failPatterns, final int maxRecursionDepth, final int debugDepth,
      int maxAlternatives) {
    assert (rootNode.getFailPatterns().isEmpty());
    rootNode.setFailPatterns(failPatterns);

    final int ownFuzzCount = ++this.fuzzCount;

    int numberOfAlternatives = 0;

    // decrease 'maxAlternatives' if root class allows less alternatives than are currently allowed
    {
      final int maxAlternativesClass = rootNode.getNodeClass().getMaxAlternatives();

      if (maxAlternatives == -1) {
        // no limit set yet -> set to the limit that has been specified for the root class
        maxAlternatives = maxAlternativesClass;
      } else if (maxAlternativesClass == -1) {
        // limit has already been set, but root class does not define a new limit
        // -> keep original limit
      } else {
        // limit has already been set and root class defines an additional limit
        // -> take minimum as new limit
        maxAlternatives = Math.min(maxAlternatives, maxAlternativesClass);
      }
    }

    if (maxAlternatives == 0) {
      return new Pair<FuzzingResult, Integer>(FuzzingResult.FUZZ_FAIL, numberOfAlternatives);
    }

    if (this.debug) {
      printDebugEnter(debugDepth, rootNode, ownFuzzCount);
    }

    // generator classes may have no possible productions
    if (rootNode.getPossibleProductions().isEmpty()) {
      handleReturn(FuzzingResult.FUZZ_FAIL, debugDepth, rootNode, ownFuzzCount, 0);
      return new Pair<FuzzingResult, Integer>(FuzzingResult.FUZZ_FAIL, numberOfAlternatives);
    }

    assert (!rootNode.isResolved());

    if (!constructGuardFree(rootNode, true, maxRecursionDepth, debugDepth, failPatterns)) {
      // NOTE: root node has already been deconstructed by constructGuardFree()

      handleReturn(FuzzingResult.FUZZ_FAIL, debugDepth, rootNode, ownFuzzCount, 0);

      return new Pair<FuzzingResult, Integer>(FuzzingResult.FUZZ_FAIL, numberOfAlternatives);
    }

    boolean childFailedDueToHeightLimit = false;

    enumerateAlternatives: do {
      // check if timeout occurred
      {
        ++this.deadlineCounter;

        if ((this.deadline > -1) && ((this.deadlineCounter & 0xFF) == 0)
            && (System.currentTimeMillis() > this.deadline)) {
          throw new TimeoutException(System.currentTimeMillis() - (this.deadline - this.timeout));
        }
      }

      ++numberOfAlternatives;

      evaluateAttributes(rootNode, true);

      if (!this.syntaxOnly && !rootNode.allGuardsOnInheritedAttributesSatisfied()) {
        final FailPattern failPattern = FailPattern.forRootAlternative(rootNode);
        rootNode.addFailPattern(failPattern);
        continue enumerateAlternatives;
      }

      if (!this.syntaxOnly && !rootNode.noGuardsFailing(false)) {
        final FailPattern failPattern =
            FailPattern.fromFailingRoot(rootNode, this.useSpecificPatterns);
        rootNode.addFailPattern(failPattern);
        continue enumerateAlternatives;
      }

      Candidate resolvableNode;

      while ((resolvableNode = findFirstResolvableGuardedNode(rootNode)) != null) {
        final Candidate toResolve = resolvableNode;

        final Node nodeToResolve = toResolve.getFirst();
        final List<FailPattern> failPatternsToResolve = toResolve.getSecond();

        final int remainingAlternatives =
            (maxAlternatives == -1) ? (-1) : (maxAlternatives - numberOfAlternatives);

        final Pair<FuzzingResult, Integer> returnValue =
            fuzz(nodeToResolve, failPatternsToResolve, maxRecursionDepth, debugDepth + 1,
                remainingAlternatives);

        final FuzzingResult fuzzingResult = returnValue.getFirst();
        numberOfAlternatives += returnValue.getSecond();

        assert (maxAlternatives == -1 || returnValue.getSecond() <= maxAlternatives);

        if (fuzzingResult != FuzzingResult.FUZZ_SUCCESS) {
          if (fuzzingResult == FuzzingResult.FUZZ_FAIL) {
            final FailPattern failPattern =
                FailPattern.fromFailingChild(rootNode, nodeToResolve, failPatternsToResolve,
                    this.useSpecificPatterns);
            rootNode.addFailPattern(failPattern);
          } else {
            assert (fuzzingResult == FuzzingResult.FUZZ_HEIGHT_LIMIT);

            childFailedDueToHeightLimit = true;

            final FailPattern failPattern =
                FailPattern.fromHeightLimit(rootNode, nodeToResolve);
            rootNode.addFailPattern(failPattern);
          }

          continue enumerateAlternatives;
        } // end-if "recursive call not successful"

        // -> recursive call successful
        // more attribute values may be available now that make the next node 'resolvable'
        evaluateAttributes(rootNode, true);

        if (!this.syntaxOnly && !rootNode.noGuardsFailing(false)) {
          final FailPattern failPattern =
              FailPattern.fromFailingRoot(rootNode, this.useSpecificPatterns);
          rootNode.addFailPattern(failPattern);
          rootNode.clearNonInheritedAttributeValues();
          continue enumerateAlternatives;
        }
      } // end-while "unresolved guarded nodes available"

      // -> all (resolvable) guarded nodes have been instantiated successfully
      // -> sub-tree should be complete and valid

      // a shallow evaluation of the attributes suffices
      evaluateAttributes(rootNode, true);

      if (!rootNode.subtreeFinished()) {
        if (DETERMINE_EXCEPTIONAL_NODES) {
          determineExceptionalNodes(rootNode);
        }

        throw new FuzzingFailedException(
            "sub-tree not finished (probably due to unresolvable guarded nodes)");
      }

      if (this.syntaxOnly || rootNode.allGuardsSatisfied(false)) {
        rootNode.clearFailPatterns();

        handleReturn(FuzzingResult.FUZZ_SUCCESS, debugDepth, rootNode, ownFuzzCount,
            numberOfAlternatives);

        // \o/
        return new Pair<FuzzingResult, Integer>(FuzzingResult.FUZZ_SUCCESS, numberOfAlternatives);
      } else {
        final FailPattern failPattern =
            FailPattern.fromFailingRoot(rootNode, this.useSpecificPatterns);
        rootNode.addFailPattern(failPattern);
        rootNode.clearNonInheritedAttributeValues();
        continue enumerateAlternatives;
      }
    } while ((maxAlternatives == -1 || numberOfAlternatives < maxAlternatives)
        && chooseAlternative(rootNode, rootNode.getFailPatterns(), maxRecursionDepth, debugDepth));

    // alternatives exhausted -> fuzzing failed
    final boolean failedDueToHeightLimit =
        childFailedDueToHeightLimit || failedDueToHeightLimit(rootNode);

    rootNode.clearFailPatterns();
    rootNode.deconstructButKeepInheritedAttributeValues();

    final FuzzingResult fuzzingResult;
    {
      if (failedDueToHeightLimit) {
        fuzzingResult = FuzzingResult.FUZZ_HEIGHT_LIMIT;
      } else {
        fuzzingResult = FuzzingResult.FUZZ_FAIL;
      }
    }

    handleReturn(fuzzingResult, debugDepth, rootNode, ownFuzzCount, numberOfAlternatives);

    return new Pair<FuzzingResult, Integer>(fuzzingResult, numberOfAlternatives);
  }

  private static final boolean isGuardedOrUnit(final Class _class) {
    return _class.hasGuardAttribute() || _class.isUnit;
  }

  private final void printDebugEnter(final int debugDepth, final Node rootNode,
      final int ownFuzzCount) {
    for (int i = 0; i < debugDepth; ++i) {
      System.err.print("  ");
    }

    final String className = rootNode.getNodeClass().getName();

    System.err.format(Locale.US,
        "=> [%3d] (%8d)@(%5.1f) %s<%x>\n", debugDepth, ownFuzzCount,
        (System.currentTimeMillis() - this.startTime) / 1000., className, rootNode.hashCode());

    if (DEBUG_PRINTS_AT.contains(ownFuzzCount)) {
      System.err.println("\n=============[ CODE ]=============");
      System.err.println(this.programRootNode.printCode());
      System.err.println("==================================\n");
      System.err.println("\n=============[ AST ]=============");
      System.err.println(rootNode.printTree());
      System.err.println("==================================\n");
    }
  }

  private final void handleReturn(final FuzzingResult result, final int debugDepth,
      final Node rootNode, final int ownFuzzCount, final int numberOfAlternatives) {
    if (this.debug) {
      printDebugReturn(result, debugDepth, rootNode, ownFuzzCount);
    }

    final String className = rootNode.getNodeClass().getName();
    final int numberOfCalls = this.fuzzCount - ownFuzzCount;

    if (this.diagnosticsWriter != null) {
      FileUtil.write(String.format(
          "%7d | %7d | %-30s | %s\n", numberOfCalls, numberOfAlternatives, className, result),
          this.diagnosticsWriter);
    }
  }

  private final void printDebugReturn(final FuzzingResult result, final int debugDepth,
      final Node rootNode, final int ownFuzzCount) {
    for (int i = 0; i < debugDepth; ++i) {
      System.err.print("  ");
    }

    final String className = rootNode.getNodeClass().getName();
    final int numberOfCalls = this.fuzzCount - ownFuzzCount;

    System.err.format(Locale.US,
        "<= [%3d] (%8d)@(%5.1f) %s<%x>: %s (took %d calls, ratio: %.5f) -> %s\n",
        debugDepth, ownFuzzCount, (System.currentTimeMillis() - this.startTime) / 1000.,
        className, rootNode.hashCode(), result, numberOfCalls,
        ((double) rootNode.size()) / numberOfCalls, rootNode.getProduction());
  }

  private final void evaluateAttributes(final Node rootNode, final boolean shallow) {
    if (!this.syntaxOnly) {
      rootNode.evaluateAttributesLoop(this.shallowAttributeEvaluation && shallow);
    }
  }

  private final boolean productionApplicable(final Node node, final Production production) {
    if (production.weight == 0) {
      return false;
    }

    final Class nodeClass = node.getNodeClass();
    if (nodeClass.isLiteralClass() || nodeClass.isGeneratorClass()) {
      return true;
    }

    if (node.getAllowedWidth() == 1 && production.isListRecursion) {
      return false;
    }

    if ((!production.isRecursive()) || (node.getAllowedHeight() == -1)) {
      return true;
    }
    return production.minHeight <= node.getAllowedHeight();
  }

  private final List<Production> applicableProductions(final Node node) {
    // handle fast cases first
    {
      final Class nodeClass = node.getNodeClass();
      if (nodeClass.isLiteralClass() || nodeClass.isGeneratorClass()) {
        return node.getPossibleProductions();
      }

      if (!nodeClass.isList && node.getAllowedHeight() == -1) {
        final List<Production> applicableProductions =
            new LinkedList<>(node.getPossibleProductions());
        for (final Iterator<Production> iterator = applicableProductions.iterator();
            iterator.hasNext(); ) {
          final Production production = iterator.next();
          if (production.weight == 0) {
            iterator.remove();
          }
        }
        return applicableProductions;
      }
    }

    final List<Production> applicableProductions = new ArrayList<>();

    for (final Production production : node.getPossibleProductions()) {
      if (productionApplicable(node, production)) {
        applicableProductions.add(production);
      }
    }

    return applicableProductions;
  }

  private final void constructGuardFree(final Node rootNode,
      final boolean forceRootNodeConstruction, final int maxRecursionDepth) {
    if (forceRootNodeConstruction || !rootNode.isResolved()) {
      final List<Production> applicableProductions = applicableProductions(rootNode);

      if (applicableProductions.isEmpty()) {
        throw new FuzzingFailedException(
            String.format("no applicable productions for class '%s' left",
                rootNode.getNodeClass().getName()));
      }

      final Production production =
          this.productionSelection.choose(rootNode, applicableProductions);

      rootNode.applyProduction(production, maxRecursionDepth);
    }

    for (final Node childNode : rootNode.getChildren()) {
      if (this.syntaxOnly || !isGuardedOrUnit(childNode.getNodeClass())) {
        constructGuardFree(childNode, false, maxRecursionDepth);
      }
    }
  }

  private final boolean constructGuardFree(final Node rootNode,
      final boolean forceRootNodeConstruction, final int maxRecursionDepth, final int debugDepth,
      final List<FailPattern> failPatterns) {
    constructGuardFree(rootNode, forceRootNodeConstruction, maxRecursionDepth);

    for (final FailPattern failPattern : failPatterns) {
      if (failPattern.matches(rootNode)) {
        if (!chooseAlternative(rootNode, failPatterns, maxRecursionDepth, debugDepth)) {
          rootNode.clearFailPatterns();
          rootNode.deconstructButKeepInheritedAttributeValues();
          return false;
        }
        return true; // already constructed an alternative that does not violate any fail patterns
      }
    }

    return true;
  }

  private final boolean allListElementNodesFinished(final Node node) {
    final Class nodeClass = node.getNodeClass();
    assert (nodeClass.isList);

    for (final Node child : node.children) {
      if (child.getNodeClass() == nodeClass) {
        // recursion node -> ignore
      } else {
        if (!child.subtreeFinished()) {
          return false;
        }
      }
    }

    return true;
  }

  private final Node getListRecursionNode(final Node node) {
    final Class nodeClass = node.getNodeClass();

    assert (nodeClass.isList);
    assert (node.isResolved());
    assert (node.getProduction().isListRecursion);

    for (final Node child : node.getChildren()) {
      if (child.getNodeClass() == nodeClass) {
        return child;
      }
    }

    assert (false) : "list without recursion node";
    return null;
  }

  private final boolean isRootOfPartialList(final Node node) {
    assert (node.isResolved());

    final Class nodeClass = node.getNodeClass();
    final boolean isListRecursion = node.getProduction().isListRecursion;

    if (!nodeClass.isList || !isListRecursion) {
      return false;
    }

    if (!allListElementNodesFinished(node)) {
      return false;
    }

    final Node recursionNode = getListRecursionNode(node);

    if (!recursionNode.isResolved()) {
      return true;
    }

    return !allListElementNodesFinished(recursionNode);
  }

  private final Production getListBaseProduction(final Class nodeClass) {
    assert (nodeClass.isList);

    for (final Production production : nodeClass.getProductions()) {
      if (!production.isListRecursion) {
        return production;
      }
    }

    assert (false);
    return null;
  }

  private final boolean chooseAlternative(final Node rootNode,
      final List<FailPattern> failPatterns, final int maxRecursionDepth, final int debugDepth) {
    return chooseAlternative(rootNode, rootNode, failPatterns, maxRecursionDepth, debugDepth, true);
  }

  private final boolean chooseAlternative(final Node subRootNode, final Node rootNode,
      final List<FailPattern> failPatterns, final int maxRecursionDepth,
      final int debugDepth, final boolean replaceSubRootNode) {
    if (subRootNode != rootNode && isGuardedOrUnit(subRootNode.getNodeClass())) {
      deconstructSubtree(subRootNode, rootNode, true);
      evaluateAttributes(rootNode, true);

      return true;
    }

    if (containsWildcardPattern(failPatterns)) {
      return false;
    }

    if (subRootNode.expected != null && subRootNode.expected.subtreeFinished()) {
      return false;
    }

    // try to "shrink" partially constructed lists
    if (isRootOfPartialList(subRootNode) && subRootNode.expected == null) {
      assert (subRootNode.getProduction().isListRecursion);

      if (shrinkList(subRootNode, rootNode, failPatterns, maxRecursionDepth, debugDepth)) {
        return true;
      }
    }

    final int numberOfChildren = subRootNode.getChildren().size();

    // replace child (if possible)
    {
      assert (subRootNode.isResolved());
      final Production subRootNodeProduction = subRootNode.getProduction();

      final int[] childVisitationOrder = subRootNodeProduction.getChildVisitationOrder();

      for (final int replaceableChildIndex : childVisitationOrder) {
        final Node replaceableChild = subRootNode.getChild(replaceableChildIndex);

        final List<FailPattern> matchingFailPatterns =
            filterMatchingFailPatterns(subRootNode, failPatterns, replaceableChildIndex);

        if (containsWildcardPattern(matchingFailPatterns)) {
          continue;
        }

        final boolean childReplaced =
            chooseAlternative(replaceableChild, rootNode, matchingFailPatterns, maxRecursionDepth,
                debugDepth, true);

        if (childReplaced) {
          return true;
        }
      }
    }

    if (!replaceSubRootNode) {
      return false;
    }

    // handle simple/inexpensive case first: if there is a production that is not the root of any
    // fail pattern, this production can be used as an alternative
    final List<Production> alternatives = getAlternativesFor(subRootNode, failPatterns);

    if (alternatives == null || alternatives.isEmpty()) {
      // there is no production left that is not the root of any fail pattern,
      // but there may still be alternatives left!
      // -> search 'extensively'
      final int numberOfProductions = subRootNode.getPossibleProductions().size();
      if (numberOfChildren > 0 && numberOfProductions > 1) {
        final boolean replacedSubRoot = chooseAlternativeExtensive(subRootNode, rootNode,
            failPatterns, maxRecursionDepth, debugDepth);
        return replacedSubRoot;
      } else {
        return false;
      }
    }

    final Production alternative = this.productionSelection.choose(subRootNode, alternatives);

    deconstructSubtree(subRootNode, rootNode, true);
    subRootNode.applyProduction(alternative, maxRecursionDepth);
    constructGuardFree(subRootNode, false, maxRecursionDepth, debugDepth, new ArrayList<>());

    rootNode.clearNonInheritedAttributeValues();
    evaluateAttributes(rootNode, true);

    return true;
  }

  private final boolean shrinkList(final Node node, final Node rootNode,
      final List<FailPattern> failPatterns, final int maxRecursionDepth, final int debugDepth) {
    final Class nodeClass = node.getNodeClass();
    final boolean isListRecursion = node.getProduction().isListRecursion;

    assert (nodeClass.isList && isListRecursion);

    final Production listBaseProduction = getListBaseProduction(node.getNodeClass());

    if (isShrinkableList(node.getProduction(), listBaseProduction)) {
      // try to replace original (recursive) node with a new base node
      final Node replacement = node.cloneNode(null);
      replacement.applyProduction(listBaseProduction, maxRecursionDepth);

      // copy children of original (recursive) node to new base node
      final List<Class> originalChildClasses = node.getProduction().childClasses();
      final List<Class> baseChildClasses = listBaseProduction.childClasses();

      final int numberOfBaseChildren = baseChildClasses.size();
      final int numberOfRecursionChildren = originalChildClasses.size();

      int originalChildIndex = 0;
      for (int baseChildIndex = 0; baseChildIndex < numberOfBaseChildren; ++baseChildIndex) {
        final Class baseChildClass = baseChildClasses.get(baseChildIndex);

        while (originalChildClasses.get(originalChildIndex) != baseChildClass) {
          ++originalChildIndex;
        }

        final Node originalChildNode = node.getChild(originalChildIndex);

        replacement.replaceChild(baseChildIndex, originalChildNode.cloneTree(replacement));

        ++originalChildIndex;
      }

      // check if new sub-tree is prohibited by a fail pattern
      for (final FailPattern failPattern : failPatterns) {
        if (failPattern.matches(replacement)) {
          // shortened list is prohibited by a fail pattern -> discard
          return false;
        }
      }

      // check that guards in replacement are satisfied
      {
        replacement.clearNonInheritedAttributeValues();
        for (final Node child : replacement.getChildren()) {
          child.clearAttributeValues(true);
        }

        evaluateAttributes(replacement, false);

        if (!replacement.allGuardsSatisfied(true)) {
          // we cannot guarantee that the replacement is valid -> discard
          return false;
        }
      }

      // safely remove the original sub-tree (including all attribute values that depend on it)
      if (node == rootNode) {
        rootNode.deconstructButKeepInheritedAttributeValues();
      } else {
        deconstructSubtree(node, rootNode, true);
      }

      // replace original sub-tree with replacement
      assert (node.getParent() != null);
      replacement.setParent(node.getParent());
      node.replaceBy(replacement, true);

      evaluateAttributes(rootNode, true);

      // list shrinking successful
      return true;
    }

    return false;
  }

  private final boolean isShrinkableList(final Production recursionProduction,
      final Production baseProduction) {
    final List<Class> recursionChildClasses = recursionProduction.childClasses();
    final List<Class> baseChildClasses = baseProduction.childClasses();

    if (recursionChildClasses.size() > recursionChildClasses.size()) {
      return false;
    }

    // check if list of base child classes is a subsequence of the list of recursion child classes
    int indexRecursion = 0;
    final int numberOfRecursionChildren = recursionChildClasses.size();

    for (final Class baseChildClass : baseChildClasses) {
      while ((indexRecursion < numberOfRecursionChildren)
          && (recursionChildClasses.get(indexRecursion) != baseChildClass)) {
        ++indexRecursion;
      }

      if (indexRecursion == numberOfRecursionChildren) {
        // did not find matching class in recursion child classes -> no subsequence
        return false;
      }

      // found matching class in recursion child classes -> advance to next element
      ++indexRecursion;
    }

    return true;
  }

  private final boolean chooseAlternativeExtensive(final Node subRootNode, final Node rootNode,
      final List<FailPattern> failPatterns, final int maxRecursionDepth, final int debugDepth) {
    final Production currentProduction = subRootNode.getProduction();
    final List<Production> productions = subRootNode.getPossibleProductions();

    rootProductionAlternative: for (final Production production : productions) {
      if (production == currentProduction) {
        continue;
      }

      // XXX NOTE: this assumes that the reference of the generator value does not change!
      if (production.generatorValue != null
          && production.generatorValue == currentProduction.generatorValue) {
        continue;
      }

      final int allowedHeight = subRootNode.getAllowedHeight();
      final int allowedWidth = subRootNode.getAllowedWidth();

      if (productionApplicable(subRootNode, production)) {
        final Node alternativeSubTree =
            subRootNode.getNodeClass().createNode(null, allowedHeight, allowedWidth);
        alternativeSubTree.applyProduction(production, maxRecursionDepth);

        constructGuardFree(alternativeSubTree, false, maxRecursionDepth, debugDepth,
            new ArrayList<>());

        for (final FailPattern failPattern : failPatterns) {
          if (failPattern.matches(alternativeSubTree)) {
            if (!chooseAlternative(alternativeSubTree, alternativeSubTree, failPatterns,
                maxRecursionDepth, debugDepth, false)) {
              continue rootProductionAlternative;
            }
            break; // already constructed an alternative that does not violate any fail pattern
          }
        }

        for (final Attribute attribute : subRootNode.getNodeClass().getInheritedAttributes()) {
          if (attribute.hasValue(subRootNode)) {
            attribute.setValue(alternativeSubTree, attribute.getValue(subRootNode));
          }
        }

        deconstructSubtree(subRootNode, rootNode, true);

        // replace original sub-tree with new alternative (but keep the original fail patterns)
        subRootNode.replaceBy(alternativeSubTree, false);

        rootNode.clearNonInheritedAttributeValues();
        evaluateAttributes(rootNode, true);

        return true;
      }
    }

    return false;
  }

  private final boolean containsWildcardPattern(final List<FailPattern> failPatterns) {
    for (final FailPattern failPattern : failPatterns) {
      if (failPattern.getRootProduction() == FailPattern.WILDCARD) {
        return true;
      }
    }
    return false;
  }

  private final List<Production> getAlternativesFor(final Node subRootNode,
      final List<FailPattern> failPatterns) {
    final Set<Production> failedProductions = getFailedProductions(failPatterns);
    final Set<Object> failedGeneratorValues = new HashSet<>();
    {
      for (final Production failedProduction : failedProductions) {
        if (failedProduction.generatorValue != null) {
          failedGeneratorValues.add(failedProduction.generatorValue);
        }
      }
    }

    if (failedProductions.size() == subRootNode.getPossibleProductions().size()) {
      return null;
    }

    final List<Production> alternatives = new ArrayList<>();

    for (final Production production : subRootNode.getPossibleProductions()) {
      if (!failedProductions.contains(production)
          && !failedGeneratorValues.contains(production.generatorValue)
          && productionApplicable(subRootNode, production)) {
        alternatives.add(production);
      }
    }

    return alternatives;
  }

  private final Set<Production> getFailedProductions(final List<FailPattern> failPatterns) {
    final Set<Production> failedProductions = new HashSet<>();

    for (final FailPattern failPattern : failPatterns) {
      failedProductions.add(failPattern.getRootProduction());
    }

    return failedProductions;
  }

  // currently also used by reduction algorithm
  public static final void deconstructSubtree(final Node subRootNode, final Node rootNode,
      final boolean deconstructSubRootNode) {
    if (deconstructSubRootNode && subRootNode != rootNode) {
      subRootNode.deconstruct();
    }

    if (subRootNode.getParent() != null && subRootNode != rootNode) {
      deconstructSubtreeHelper(subRootNode.getParent(), rootNode);
    }
  }

  private static final void deconstructSubtreeHelper(final Node currentNode, final Node rootNode) {
    if (!currentNode.isResolved()) {
      return;
    }

    final Set<Node> childrenToVisit = new HashSet<>();
    boolean visitParent = false;
    boolean guardInvalidated = false;

    boolean generatorNodeDeconstructed;
    do {
      generatorNodeDeconstructed = false;

      for (final AttributeRule attributeRule : currentNode.getProduction().getAttributeRules()) {
        if (!attributeRule.alreadyComputed(currentNode)) {
          // attribute value does not exist (either because it has not been computed before or
          // because it has already been deleted during deconstruction) -> stop search here
        } else {
          // attribute value does (still) exist
          // -> check if attribute value could still be computed
          if (!attributeRule.allSourceAttributesAvailable(currentNode)) {
            // source values are missing -> remove attribute value and visit target node (again)
            final Node targetNode = attributeRule.getTargetNode(currentNode);
            attributeRule.getTargetAttribute().clearValue(targetNode);

            if (targetNode instanceof GeneratorNode) {
              targetNode.deconstruct();
              ((GeneratorNode) targetNode).clearPossibleProductions();
              generatorNodeDeconstructed = true;
            }

            if (attributeRule.isGuardRule()) {
              guardInvalidated = true;
            } else if (attributeRule.isInheritedRule()) {
              childrenToVisit.add(targetNode);
            } else {
              assert (attributeRule.isSynthesizedRule());
              visitParent = true;
            }
          }
        }
      }
    } while (generatorNodeDeconstructed);

    if (guardInvalidated) {
      if (currentNode != rootNode) {
        currentNode.deconstruct();
      }
    } else {
      for (final Node child : childrenToVisit) {
        deconstructSubtreeHelper(child, rootNode);
      }
    }

    if ((visitParent || guardInvalidated)
        && currentNode.getParent() != null
        && currentNode != rootNode) {
      deconstructSubtreeHelper(currentNode.getParent(), rootNode);
    }
  }

  private final Candidate findFirstResolvableGuardedNode(final Node rootNode) {
    return findFirstResolvableGuardedNode(rootNode, rootNode.getFailPatterns(), rootNode);
  }

  private final Candidate findFirstResolvableGuardedNode(final Node currentNode,
      final List<FailPattern> failPatterns, final Node rootNode) {
    if (currentNode != rootNode && isGuardedOrUnit(currentNode.getNodeClass())) {
      if (currentNode.isResolved()) {
        // found guard/unit node that has already been resolved
        // -> do not search any further in this sub-tree 
        return null;
      } else {
        if (currentNode.allInheritedAttributesEvaluated()) {
          // found resolvable node
          return new Candidate(currentNode, failPatterns);
        } else {
          // cannot resolve this guard/unit node
          return null;
        }
      }
    } else {
      final int numberOfChildren = currentNode.getNumberOfChildren();
      for (int childIndex = 0; childIndex < numberOfChildren; ++childIndex) {
        final Node childNode = currentNode.getChild(childIndex);

        final List<FailPattern> matchingFailPatterns =
            filterMatchingFailPatterns(currentNode, failPatterns, childIndex);

        final Candidate candidate =
            findFirstResolvableGuardedNode(childNode, matchingFailPatterns, rootNode);

        if (candidate != null) {
          return candidate;
        }
      }
    }

    return null;
  }

  private final List<FailPattern> filterMatchingFailPatterns(final Node node,
      final List<FailPattern> failPatterns, final int childIndex) {
    final List<FailPattern> matchingFailPatterns = new ArrayList<>();

    for (final FailPattern failPattern : failPatterns) {
      if (failPattern.matches(node, childIndex)) {
        matchingFailPatterns.add(failPattern.getChild(childIndex));
      }
    }

    return matchingFailPatterns;
  }

  private final boolean failedDueToHeightLimit(final Node rootNode) {
    // if the root node has an expected production, the construction did not fail due to the height
    // limit (even without a height limit we would not apply all productions)
    if (rootNode.expected != null) {
      return false;
    }

    final Set<Production> failedProductions = new HashSet<>();
    {
      for (final FailPattern failPattern : rootNode.getFailPatterns()) {
        final Production failedProduction = failPattern.getRootProduction();

        if (failedProduction == FailPattern.WILDCARD) {
          return false;
        }

        failedProductions.add(failedProduction);
      }
    }

    for (final Production production : rootNode.getPossibleProductions()) {
      if (!failedProductions.contains(production)) {
        return true;
      }
    }

    return false;
  }

  private final void determineExceptionalNodes(final Node rootNode) {
    System.err.println("----[ no resolvable nodes left ]----");

    rootNode.clearNonInheritedAttributeValues();
    for (final Node childNode : rootNode.getChildren()) {
      childNode.clearAttributeValues(true);
    }
    rootNode.evaluateAttributesLoop(false, true);

    final List<Candidate> unresolvedNodes = findUnresolvedGuardedNodes(rootNode);
    for (final Candidate candidate : unresolvedNodes) {
      final Node unresolvedNode = candidate.getFirst();
      assert (!unresolvedNode.allInheritedAttributesEvaluated());

      System.err.format("[i] missing INH attributes of node %s: ", unresolvedNode.getNodeName());

      boolean first = true;
      for (final Attribute attribute : unresolvedNode.getNodeClass().getInheritedAttributes()) {
        if (attribute.hasValue(unresolvedNode)) {
          continue;
        }

        if (first) {
          first = false;
        } else {
          System.err.print(", ");
        }

        System.err.print(attribute.getName());
      }

      System.err.println();
    }


    System.err.println("------------------------------------");
  }

  private final List<Candidate> findUnresolvedGuardedNodes(final Node rootNode) {
    final List<Candidate> unresolvedNodes = new ArrayList<>();
    findUnresolvedGuardedNodes(rootNode, rootNode.getFailPatterns(), unresolvedNodes);

    return unresolvedNodes;
  }

  private final void findUnresolvedGuardedNodes(final Node rootNode,
      final List<FailPattern> failPatterns, final List<Candidate> unresolvedNodes) {
    if ((!rootNode.isResolved()) && isGuardedOrUnit(rootNode.getNodeClass())) {
      unresolvedNodes.add(new Candidate(rootNode, failPatterns));
    }

    final int numberOfChildren = rootNode.getNumberOfChildren();
    for (int childIndex = 0; childIndex < numberOfChildren; ++childIndex) {
      final Node childNode = rootNode.getChild(childIndex);

      final List<FailPattern> matchingFailPatterns =
          filterMatchingFailPatterns(rootNode, failPatterns, childIndex);

      findUnresolvedGuardedNodes(childNode, matchingFailPatterns, unresolvedNodes);
    }
  }

  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private final String getTimeStamp() {
    return this.dateFormat.format(new Date());
  }

}
