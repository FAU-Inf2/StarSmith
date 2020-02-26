package i2.act.fuzzer.deserialization;

import i2.act.fuzzer.GeneratorNode;
import i2.act.fuzzer.Node;
import i2.act.fuzzer.Production;
import i2.act.fuzzer.Specification;
import i2.act.util.FileUtil;
import i2.act.util.lexer.Lexer;
import i2.act.util.lexer.SyntaxError;
import i2.act.util.lexer.Token;
import i2.act.util.lexer.Token.Kind;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// TODO compute allowedWidth and allowedHeight on deserialized AST nodes
public final class ASTDeserializer {

  public static final Node parseFile(final String fileName, final Specification specification,
      final boolean shortFormat, final boolean addMissingLiteralProductions) {
    final String string = FileUtil.readFile(fileName);
    return parse(string, specification, shortFormat, addMissingLiteralProductions);
  }

  public static final Node parse(final String string, final Specification specification,
      final boolean shortFormat, final boolean addMissingLiteralProductions) {
    return parse(string.toCharArray(), specification, shortFormat, addMissingLiteralProductions);
  }

  public static final Node parse(final char[] characters, final Specification specification,
      final boolean shortFormat, final boolean addMissingLiteralProductions) {
    final Lexer lexer = new Lexer(characters, true);

    final Map<Node, Integer> generatorNodes = new HashMap<>();

    // Node <EOF>
    final Node node = parseNode(
        lexer, specification, shortFormat, generatorNodes, addMissingLiteralProductions);
    lexer.assertPop(Kind.TK_EOF);

    if (!generatorNodes.isEmpty()) {
      instantiateGeneratorNodes(node, generatorNodes);
    }

    return node;
  }

  private static final Node parseNode(final Lexer lexer, final Specification specification,
      final boolean shortFormat, final Map<Node, Integer> generatorNodes,
      final boolean addMissingLiteralProductions) {
    return parseNode(
        lexer, specification, shortFormat, null, generatorNodes, addMissingLiteralProductions);
  }

  private static final Node parseNode(final Lexer lexer, final Specification specification,
      final boolean shortFormat, Node node, final Map<Node, Integer> generatorNodes,
      final boolean addMissingLiteralProductions) {
    // '(' ID ':' ( ProductionName ( Node )* ) | ( '?' ) ')'

    lexer.assertPop(Kind.TK_LPAREN);

    final int id = parseID(lexer);

    lexer.assertPop(Kind.TK_COLON);

    if (lexer.peek().kind == Kind.TK_QMARK) {
      lexer.assertPop(Kind.TK_QMARK);

      if (node == null) {
        // root node -> crete new node
        final i2.act.fuzzer.Class rootClass = specification.getRootClass();
        node = rootClass.createNode(null, -1, -1);
      } else {
        // intentionally left blank; node has already been created
      }

      node.id = id;
    } else {
      node = parseProductionName(
          lexer, specification, shortFormat, node, generatorNodes, addMissingLiteralProductions);
      node.id = id;

      final int numberOfChildren = node.getNumberOfChildren();
      int childIndex = 0;

      while (lexer.peek().kind != Kind.TK_RPAREN) {
        if (childIndex >= numberOfChildren) {
          throw new SyntaxError(
              String.format("expected only '%d' child nodes", numberOfChildren),
              lexer.getPosition());
        }

        final Node childNode = node.getChild(childIndex);
        ++childIndex;

        parseNode(lexer, specification, shortFormat, childNode, generatorNodes,
            addMissingLiteralProductions);
      }

      if (childIndex != numberOfChildren) {
        throw new SyntaxError(
            String.format("expected '%d' child nodes but found only '%d'", 
                numberOfChildren, childIndex),
            lexer.getPosition());
      }
    }

    lexer.assertPop(Kind.TK_RPAREN);

    return node;
  }

  private static final int parseID(final Lexer lexer) {
    final Token idToken = lexer.assertPop(Kind.TK_NUMBER, true);
    return Integer.parseInt(idToken.string);
  }

  private static final Node parseProductionName(final Lexer lexer,
      final Specification specification, final boolean shortFormat, Node node,
      final Map<Node, Integer> generatorNodes, final boolean addMissingLiteralProductions) {
    final boolean isGeneratorNode = (node != null) && (node.getNodeClass().isGeneratorNode());

    Production production;
    {
      if (shortFormat) {
        // Number ':' Number
        final Token classIdToken = lexer.assertPop(Kind.TK_NUMBER, true);
        final int classId = Integer.parseInt(classIdToken.string);
        
        lexer.assertPop(Kind.TK_COLON);

        final Token productionIdToken = lexer.assertPop(Kind.TK_NUMBER, true);
        final int productionId = Integer.parseInt(productionIdToken.string);

        if (isGeneratorNode) {
          production = null;
          generatorNodes.put(node, productionId);
        } else {
          final i2.act.fuzzer.Class _class = specification.getClassById(classId);

          if (_class == null) {
            throw new SyntaxError(
                String.format("unknown class ID '%d'", classId), classIdToken.start);
          }

          production = _class.getProductionById(productionId);

          if (production == null) {
            throw new SyntaxError(
                String.format("unknown production ID '%d'", productionId), productionIdToken.start);
          }
        }
      } else {
        // Identifier ':' Identifier

        final Token classNameToken = lexer.assertPop(Kind.TK_IDENTIFIER);
        lexer.assertPop(Kind.TK_COLON);
        final Token productionNameToken = lexer.assertPop(Kind.TK_IDENTIFIER);

        final String className = classNameToken.string;
        final i2.act.fuzzer.Class _class = specification.getClassByName(className);

        if (_class == null) {
          throw new SyntaxError(
              String.format("unknown class '%s'", className), classNameToken.start);
        }

        final String productionName = productionNameToken.string;

        if (isGeneratorNode) {
          production = null;

          if (!productionName.startsWith(GeneratorNode.GENERATOR_PRODUCTION_PREFIX)) {
            throw new SyntaxError(
                String.format("invalid production name '%s' for generator class '%s'",
                    productionName, className),
                classNameToken.start);
          }

          final String idSubstring =
              productionName.substring(GeneratorNode.GENERATOR_PRODUCTION_PREFIX.length());

          final int id;
          {
            try {
              id = Integer.parseInt(idSubstring);
            } catch (final Throwable throwable) {
              throw new SyntaxError(
                  String.format("invalid production name '%s' for generator class '%s'",
                      productionName, className),
                  classNameToken.start);
            }
          } 

          generatorNodes.put(node, id);
        } else {
          production = _class.getProductionByName(productionName);

          if (production == null) {
            if (_class.isLiteralClass() && addMissingLiteralProductions) {
              System.err.format("[i] add missing literal production '%s' to class '%s'\n",
                  productionName, className);

              final int id = _class.getNumberOfProductions();
              production = Production.createLiteralProduction(_class, productionName, id);
            } else {
              throw new SyntaxError(
                  String.format("class '%s' does not have a production '%s'", className,
                      productionName), classNameToken.start);
            }
          }
        }
      }
    }

    // instantiate node
    if (production != null) {
      final i2.act.fuzzer.Class _class = production.ownClass();

      if (node == null) {
        // root node -> create new node and apply production to it
        node = _class.createNode(null, -1, -1);
      } else {
        // child node -> apply production to already created node (and check class beforehand)
        if (node.getNodeClass() != _class) {
          throw new SyntaxError(
              String.format("expected class '%s' but found class '%s' instead",
                  node.getNodeClass().name, _class.name), lexer.getPosition());
        }
      }

      node.applyProduction(production, -1);
    }
    
    return node;
  }

  private static final void instantiateGeneratorNodes(final Node rootNode,
      final Map<Node, Integer> generatorNodes) {
    boolean change = true;
    while (change && !generatorNodes.isEmpty()) {
      change = false;

      // re-evaluate attributes
      rootNode.evaluateAttributesLoop();

      // instantiate all generator nodes for which all inherited attributes are available
      final Iterator<Map.Entry<Node, Integer>> entries = generatorNodes.entrySet().iterator();
      while (entries.hasNext()) {
        final Map.Entry<Node, Integer> entry = entries.next();

        final Node generatorNode = entry.getKey();
        final int productionIndex = entry.getValue();

        if (!generatorNode.allInheritedAttributesEvaluated()) {
          continue;
        }

        final List<Production> possibleProductions = generatorNode.getPossibleProductions();

        if (productionIndex < 0 || productionIndex >= possibleProductions.size()) {
          throw new SyntaxError(
              String.format("invalid production index %d for generator node of class '%s'",
                  productionIndex, generatorNode.getNodeClass()));
        }

        final Production production = possibleProductions.get(productionIndex);
        generatorNode.applyProduction(production, -1); // TODO maxRecursionDepth?

        // generator node has already been handled -> remove it from the map
        entries.remove();
        change = true;
      }

      if (!change && !generatorNodes.isEmpty()) {
        throw new SyntaxError("could not instantiate all generator nodes");
      }
    }
  }

}
