package i2.act.lala.ast.visitors;

import i2.act.errors.RPGException;
import i2.act.lala.ast.*;
import i2.act.lala.ast.AttributeModifier.AttributeModifierKind;
import i2.act.lala.semantics.attributes.BuiltinFunction;
import i2.act.lala.semantics.symbols.AnnotationSymbol;
import i2.act.lala.semantics.symbols.AttributeSymbol;
import i2.act.lala.semantics.symbols.ChildSymbol;
import i2.act.lala.semantics.symbols.ClassSymbol;
import i2.act.lala.semantics.symbols.LocalAttributeSymbol;
import i2.act.lala.semantics.symbols.ProductionSymbol;
import i2.act.util.FileUtil;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GenerateJavaSpec
    extends BaseLaLaSpecificationVisitor<BufferedWriter, Void> {

  private static final int DEFAULT_LITERAL_COUNT = 50;

  private final String javaClassName;
  private final int maxRecursionDepth;
  private final String packageName;
  private final Set<String> features;

  private final Map<AttributeSymbol, String> attributeNames;
  private final Map<AttributeSymbol, String> attributeAvailableNames;
  private final Map<ClassSymbol, String> classNames;
  private final Map<ClassSymbol, String> nodeClassNames;
  private final Map<ProductionSymbol, String> productionNames;

  private final Map<ChildDeclaration, Integer> childIndexes;

  private ProductionDeclaration enclosingProductionDeclaration;

  public GenerateJavaSpec(final String javaClassName, final int maxRecursionDepth,
      final String packageName, final Set<String> features) {
    this.javaClassName = javaClassName;
    this.maxRecursionDepth = maxRecursionDepth;
    this.packageName = packageName;
    this.features = features;

    this.attributeNames = new HashMap<AttributeSymbol, String>();
    this.attributeAvailableNames = new HashMap<AttributeSymbol, String>();
    this.classNames = new HashMap<ClassSymbol, String>();
    this.nodeClassNames = new HashMap<ClassSymbol, String>();
    this.productionNames = new HashMap<ProductionSymbol, String>();

    this.childIndexes = new HashMap<ChildDeclaration, Integer>();
  }

  @Override
  public final Void visit(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    writePackage(writer);

    FileUtil.write("import i2.act.fuzzer.*;\n", writer);
    FileUtil.write("import i2.act.fuzzer.main.*;\n", writer);
    FileUtil.write("import i2.act.fuzzer.runtime.*;\n", writer);

    FileUtil.write("import java.util.List;\n", writer);
    FileUtil.write("import java.util.Objects;\n\n", writer);

    writeImports(languageSpecification, writer);

    FileUtil.write(String.format("@SuppressWarnings(\"ALL\")"
        + "\npublic final class %s {\n\n", this.javaClassName), writer);

    FileUtil.write(String.format("\tpublic static final int DEFAULT_MAX_DEPTH = %d;\n\n",
        this.maxRecursionDepth), writer);

    writeDeclarations(languageSpecification, writer);

    writeConstructor(languageSpecification, writer);

    writeGenerateAttributes(languageSpecification, writer);

    writeGenerateClasses(languageSpecification, writer);

    writeGenerateProductions(languageSpecification, writer);

    writeGenerateAttributeRules(languageSpecification, writer);

    writeSpecificationFactory(languageSpecification, writer);

    writeDeserialization(writer);

    writeMain(writer);

    FileUtil.write("}\n", writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeFunction attributeFunction, final BufferedWriter writer) {
    final Identifier namespace = attributeFunction.getNamespace();
    final Identifier functionName = attributeFunction.getFunctionName();

    if (namespace != null) {
      FileUtil.write(namespace.getName(), writer);
      FileUtil.write(".", writer);
    }

    FileUtil.write(functionName.getName(), writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeFunctionCall attributeFunctionCall,
      final BufferedWriter writer) {
    final AttributeFunction attributeFunction = attributeFunctionCall.getFunction();

    final List<AttributeExpression> arguments = attributeFunctionCall.getArguments();

    if (attributeFunction.isBuiltinFunction()) {
      final BuiltinFunction builtinFunction = attributeFunction.getBuiltinFunction();

      final List<String> codeOperands = new ArrayList<>(arguments.size());
      {
        for (final AttributeExpression argument : arguments) {
          final StringWriter stringWriter = new StringWriter();
          final BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);

          argument.accept(this, bufferedWriter);
          FileUtil.flushWriter(bufferedWriter);

          codeOperands.add(stringWriter.toString());
        }
      }

      FileUtil.write(builtinFunction.generateJavaCode(codeOperands), writer);
    } else {
      attributeFunction.accept(this, writer);

      FileUtil.write("(", writer);

      boolean first = true;
      for (final AttributeExpression argument : arguments) {
        if (!first) {
          FileUtil.write(", ", writer);
        }
        first = false;

        argument.accept(this, writer);
      }

      FileUtil.write(")", writer);
    }

    return null;
  }

  @Override
  public final Void visit(final ChildReference childReference, final BufferedWriter writer) {
    final Identifier childName = childReference.getChildName();
    final ChildSymbol childSymbol = (ChildSymbol) childName.getSymbol();

    assert (this.enclosingProductionDeclaration != null);
    final String nodeAccess = getNodeAccess(childSymbol, this.enclosingProductionDeclaration);

    FileUtil.write(nodeAccess, writer);

    return null;
  }

  @Override
  public final Void visit(final GeneratorValue generatorValue, final BufferedWriter writer) {
    final AttributeTypeName typeName = generatorValue.getTypeName();
    assert (typeName != null);

    FileUtil.write(
        String.format("((%s) node.getProduction().generatorValue)", typeName.getName().getName()),
        writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeAccess attributeAccess, final BufferedWriter writer) {
    assert (this.enclosingProductionDeclaration != null);

    final String nodeAccess = getNodeAccess(attributeAccess, this.enclosingProductionDeclaration);

    assert (this.attributeNames.containsKey(attributeAccess.getSymbol()));
    final String attributeName = this.attributeNames.get(attributeAccess.getSymbol());

    FileUtil.write(
        String.format("%s.%s", nodeAccess, attributeName),
        writer);

    return null;
  }

  @Override
  public final Void visit(final LocalAttributeAccess localAttributeAccess,
      final BufferedWriter writer) {
    final LocalAttributeSymbol attributeSymbol = localAttributeAccess.getSymbol();
    assert (attributeSymbol != null);

    final LocalAttributeDefinition definition = attributeSymbol.getDeclaration();
    assert (definition != null);

    final AttributeExpression attributeExpression = definition.getAttributeExpression();
    attributeExpression.accept(this, writer);

    return null;
  }

  @Override
  public final Void visit(final AttributeLiteral attributeLiteral, final BufferedWriter writer) {
    if (attributeLiteral.isNil()) {
      FileUtil.write("null", writer);
    } else {
      final String value = attributeLiteral.getValue();
      FileUtil.write(value, writer);
    }

    return null;
  }

  private final void writePackage(final BufferedWriter writer) {
    if (this.packageName != null) {
      FileUtil.write(String.format("package %s;\n", this.packageName), writer);
    }
  }

  private final void writeImports(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    final List<UseStatement> useStatements = languageSpecification.getUseStatements();
    if (!useStatements.isEmpty()) {
      for (final UseStatement useStatement : languageSpecification.getUseStatements()) {
        FileUtil.write(
            String.format("import runtime.%s;\n", useStatement.getNamespace().getName()), writer);
      }

      FileUtil.write("\n", writer);
    }
  }

  private final void writeDeclarations(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    writeAttributeDeclarations(languageSpecification, writer);
    writeClassDeclarations(languageSpecification, writer);
    writeProductionDeclarations(languageSpecification, writer);

    writeNodeClasses(languageSpecification, writer);
  }

  private final void writeNodeClasses(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write("\t// ==========[ NODE CLASSES ]==========\n\n", writer);

    for (final ClassDeclaration classDeclaration : languageSpecification.getClassDeclarations()) {
      final String nodeClassName = String.format("Node_%s", classDeclaration.getName());
      this.nodeClassNames.put(classDeclaration.getSymbol(), nodeClassName);

      final boolean isGeneratorClass = isGeneratorClass(classDeclaration);
      final String baseClassName = isGeneratorClass ? "GeneratorNode" : "Node";

      FileUtil.write(
          String.format("\tpublic static final class %s extends %s {\n\n",
              nodeClassName, baseClassName),
          writer);

      // static member for node class (will be set later)
      {
        FileUtil.write("\t\tpublic final i2.act.fuzzer.Class nodeClass;\n\n", writer);
      }

      // constructors
      {
        // default constructor
        FileUtil.write(
            String.format("\t\tpublic %s(final i2.act.fuzzer.Class nodeClass, "
                + "final Node parent, final int allowedHeight, final int allowedWidth, "
                + "final Node expected) {\n",
                nodeClassName),
            writer);
        FileUtil.write("\t\t\tsuper(parent, allowedHeight, allowedWidth, expected);\n", writer);
        FileUtil.write("\t\t\tthis.nodeClass = nodeClass;\n", writer);
        FileUtil.write("\t\t}\n\n", writer);

        // constructor for cloning
        FileUtil.write(
            String.format("\t\tpublic %s(final i2.act.fuzzer.Class nodeClass, "
                + "final Node parent, final int allowedHeight, final int allowedWidth, "
                + "final Node expected, final int id, final Production production) {\n",
                nodeClassName),
            writer);
        FileUtil.write(
            "\t\t\tsuper(parent, allowedHeight, allowedWidth, expected, id, production);\n",
            writer);
        FileUtil.write("\t\t\tthis.nodeClass = nodeClass;\n", writer);
        FileUtil.write("\t\t}\n\n", writer);
      }

      // attributes
      {
        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          final String attributeName = this.attributeNames.get(attributeDeclaration.getSymbol());

          // actual attribute
          {
            final String typeName = getTypeName(attributeDeclaration);

            FileUtil.write(String.format("\t\tpublic %s %s;\n", typeName, attributeName), writer);
          }

          // flag that indicates if attribute has been computed
          {
            final String attributeAvailableName = "has_" + attributeName;
            this.attributeAvailableNames.put(
                attributeDeclaration.getSymbol(), attributeAvailableName);

            FileUtil.write(
                String.format("\t\tpublic boolean %s = false;\n", attributeAvailableName), writer);
          }
        }
      }

      FileUtil.write("\n", writer);

      // public abstract Node cloneNode(final Node parent);
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write(
            String.format("\t\tpublic final %s cloneNode(final Node parent) {\n", nodeClassName),
            writer);
        FileUtil.write(
            String.format("\t\t\tfinal %s clone = new %s(this.nodeClass, parent, "
                + "this.allowedHeight, this.allowedWidth, null, this.id, "
                + "this.production);\n",
                nodeClassName, nodeClassName),
            writer);

        // copy attribute values
        {
          final List<AttributeDeclaration> attributeDeclarations =
              classDeclaration.getAttributeDeclarations();
          for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
            final AttributeSymbol attributeSymbol = attributeDeclaration.getSymbol();
            final String attributeName = this.attributeNames.get(attributeSymbol);
            final String attributeAvailableName = this.attributeAvailableNames.get(attributeSymbol);

            // actual attribute
            FileUtil.write(
                String.format("\t\t\tclone.%s = this.%s;\n", attributeName, attributeName),
                writer);

            // flag that indicates if attribute has been computed
            FileUtil.write(
                String.format("\t\t\tclone.%s = this.%s;\n",
                    attributeAvailableName, attributeAvailableName),
                writer);
          }
        }

        FileUtil.write("\t\t\treturn clone;\n", writer);

        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract Class getNodeClass();
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write("\t\tpublic final i2.act.fuzzer.Class getNodeClass() {\n", writer);
        FileUtil.write("\t\t\treturn nodeClass;\n", writer);
        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract List<Production> getAvailableProductions();
      if (!isGeneratorClass) {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write("\t\tpublic final List<Production> getAvailableProductions() {\n", writer);
        FileUtil.write("\t\t\treturn nodeClass.getProductions();\n", writer);
        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract void replaceBy(final Node otherNode, final boolean replaceFailPatterns);
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write("\t\tpublic final void replaceBy(final Node _otherNode, "
            + "final boolean replaceFailPatterns) {\n", writer);
        FileUtil.write("\t\t\tsuper.replaceBy(_otherNode, replaceFailPatterns);\n\n", writer);
        FileUtil.write(
            String.format("\t\t\tassert (_otherNode instanceof %s);\n", nodeClassName), writer);
        FileUtil.write(
            String.format("\t\t\tfinal %s otherNode = (%s) _otherNode;\n\n",
                nodeClassName, nodeClassName),
            writer);

        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          if (!attributeDeclaration.getModifier().isInheritedAttribute()) {
            final String attributeName =
                this.attributeNames.get(attributeDeclaration.getSymbol());
            final String attributeAvailableName =
                this.attributeAvailableNames.get(attributeDeclaration.getSymbol());

            FileUtil.write(
                String.format("\t\t\tthis.%s = otherNode.%s;\n",
                    attributeName, attributeName),
                writer);

            FileUtil.write(
                String.format("\t\t\tthis.%s = otherNode.%s;\n",
                    attributeAvailableName, attributeAvailableName),
                writer);
          }
        }

        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract void clearAttributeValues(final boolean recursive);
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write(
            "\t\tpublic final void clearAttributeValues(final boolean recursive) {\n", writer);

        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          final String attributeAvailableName =
              this.attributeAvailableNames.get(attributeDeclaration.getSymbol());

          FileUtil.write(
              String.format("\t\t\tthis.%s = false;\n", attributeAvailableName),
              writer);
        }

        FileUtil.write("\n", writer);

        FileUtil.write("\t\t\tif (recursive) {\n", writer);
        FileUtil.write("\t\t\t\tfor (final Node child : this.children) {\n", writer);
        FileUtil.write("\t\t\t\t\tchild.clearAttributeValues(recursive);\n", writer);
        FileUtil.write("\t\t\t\t}\n", writer);
        FileUtil.write("\t\t\t}\n", writer);

        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract void clearNonInheritedAttributeValues();
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write(
            "\t\tpublic final void clearNonInheritedAttributeValues() {\n", writer);

        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          final boolean isInheritedAttribute =
              attributeDeclaration.getModifier().getModifierKind() == AttributeModifierKind.MOD_INH;

          if (!isInheritedAttribute) {
            final String attributeAvailableName =
                this.attributeAvailableNames.get(attributeDeclaration.getSymbol());

            FileUtil.write(
                String.format("\t\t\tthis.%s = false;\n", attributeAvailableName),
                writer);
          }
        }

        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract boolean allInheritedAttributesEvaluated();
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write(
            "\t\tpublic final boolean allInheritedAttributesEvaluated() {\n", writer);

        FileUtil.write("\t\t\treturn true", writer);

        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          final boolean isInheritedAttribute =
              attributeDeclaration.getModifier().getModifierKind() == AttributeModifierKind.MOD_INH;

          if (isInheritedAttribute) {
            final String attributeAvailableName =
                this.attributeAvailableNames.get(attributeDeclaration.getSymbol());

            FileUtil.write(String.format(" && this.%s", attributeAvailableName), writer);
          }
        }

        FileUtil.write(";\n", writer);

        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract boolean someSynthesizedAttributesEvaluated();
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write(
            "\t\tpublic final boolean someSynthesizedAttributesEvaluated() {\n", writer);

        FileUtil.write("\t\t\treturn false", writer);

        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          final boolean isSynthesizedAttribute =
              attributeDeclaration.getModifier().getModifierKind() == AttributeModifierKind.MOD_SYN;

          if (isSynthesizedAttribute) {
            final String attributeAvailableName =
                this.attributeAvailableNames.get(attributeDeclaration.getSymbol());

            FileUtil.write(String.format(" || this.%s", attributeAvailableName), writer);
          }
        }

        FileUtil.write(";\n", writer);

        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract boolean allGuardsSatisfied(final boolean checkChildren);
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write(
            "\t\tpublic final boolean allGuardsSatisfied(final boolean checkChildren) {\n", writer);

        FileUtil.write("\t\t\tif (checkChildren) {\n", writer);
        FileUtil.write("\t\t\t\tfor (final Node child : this.children) {\n", writer);
        FileUtil.write("\t\t\t\t\tif (!child.allGuardsSatisfied(checkChildren)) {\n", writer);
        FileUtil.write("\t\t\t\t\t\treturn false;\n", writer);
        FileUtil.write("\t\t\t\t\t}\n", writer);
        FileUtil.write("\t\t\t\t}\n", writer);
        FileUtil.write("\t\t\t}\n", writer);

        FileUtil.write("\t\t\treturn true", writer);

        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          final boolean isGuardAttribute =
              attributeDeclaration.getModifier().getModifierKind() == AttributeModifierKind.MOD_GRD;

          if (isGuardAttribute) {
            final String attributeName =
                this.attributeNames.get(attributeDeclaration.getSymbol());
            final String attributeAvailableName =
                this.attributeAvailableNames.get(attributeDeclaration.getSymbol());

            FileUtil.write(
                String.format(" && (this.%s && this.%s)", attributeAvailableName, attributeName),
                writer);
          }
        }

        FileUtil.write(";\n", writer);

        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract boolean noGuardsFailing(final boolean checkChildren);
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write(
            "\t\tpublic final boolean noGuardsFailing(final boolean checkChildren) {\n", writer);

        FileUtil.write("\t\t\tif (checkChildren) {\n", writer);
        FileUtil.write("\t\t\t\tfor (final Node child : this.children) {\n", writer);
        FileUtil.write("\t\t\t\t\tif (!child.noGuardsFailing(checkChildren)) {\n", writer);
        FileUtil.write("\t\t\t\t\t\treturn false;\n", writer);
        FileUtil.write("\t\t\t\t\t}\n", writer);
        FileUtil.write("\t\t\t\t}\n", writer);
        FileUtil.write("\t\t\t}\n", writer);

        FileUtil.write("\t\t\treturn true", writer);

        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          final boolean isGuardAttribute =
              attributeDeclaration.getModifier().getModifierKind() == AttributeModifierKind.MOD_GRD;

          if (isGuardAttribute) {
            final String attributeName =
                this.attributeNames.get(attributeDeclaration.getSymbol());
            final String attributeAvailableName =
                this.attributeAvailableNames.get(attributeDeclaration.getSymbol());

            FileUtil.write(
                String.format(" && (!this.%s || this.%s)", attributeAvailableName, attributeName),
                writer);
          }
        }

        FileUtil.write(";\n", writer);

        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract boolean allInheritedAttributesMatch(final Node otherNode);
      {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write(
            "\t\tpublic final boolean allInheritedAttributesMatch(final Node _other) {\n", writer);

        FileUtil.write(
            String.format("\t\t\tif (!(_other instanceof %s)) {\n", nodeClassName),
            writer);

        FileUtil.write("\t\t\t\treturn false;\n", writer);
        FileUtil.write("\t\t\t}\n\n", writer);

        FileUtil.write(
            String.format("\t\t\tfinal %s otherNode = (%s) _other;\n",
                nodeClassName, nodeClassName),
            writer);

        FileUtil.write("\t\t\treturn true", writer);

        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          final boolean isInheritedAttribute =
              attributeDeclaration.getModifier().getModifierKind() == AttributeModifierKind.MOD_INH;

          if (isInheritedAttribute) {
            final String attributeName =
                this.attributeNames.get(attributeDeclaration.getSymbol());
            final String attributeAvailableName =
                this.attributeAvailableNames.get(attributeDeclaration.getSymbol());

            FileUtil.write(
                String.format(
                    " && this.%s && otherNode.%s && Objects.equals(this.%s, otherNode.%s)",
                    attributeAvailableName, attributeAvailableName, attributeName, attributeName),
                writer);
          }
        }

        FileUtil.write(";\n", writer);

        FileUtil.write("\t\t}\n\n", writer);
      }

      // public abstract List<?> generatorValues();
      if (isGeneratorClass) {
        FileUtil.write("\t\t@Override\n", writer);
        FileUtil.write(
            "\t\tpublic final List<?> generatorValues(final Node node) {\n", writer);

        final ProductionClassDeclaration productionClassDeclaration =
            (ProductionClassDeclaration) classDeclaration;
        final List<ProductionDeclaration> productionDeclarations =
            productionClassDeclaration.getProductionDeclarations();

        assert (productionDeclarations.size() == 1);
        assert (productionDeclarations.get(0) instanceof GeneratorProductionDeclaration);

        final GeneratorProductionDeclaration generatorProductionDeclaration =
            (GeneratorProductionDeclaration) productionDeclarations.get(0);

        final AttributeFunctionCall generatorCall =
            generatorProductionDeclaration.getGeneratorCall();

        FileUtil.write("\t\t\treturn ", writer);

        this.enclosingProductionDeclaration = generatorProductionDeclaration;
        generatorCall.accept(this, writer);
        this.enclosingProductionDeclaration = null;

        FileUtil.write(";\n", writer);

        FileUtil.write("\t\t}\n\n", writer);
      }

      FileUtil.write("\t}\n\n", writer);
    }

  }

  private final void writeAttributeDeclarations(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write("\t// ==========[ ATTRIBUTES ]==========\n\n", writer);

    for (final ClassDeclaration classDeclaration : languageSpecification.getClassDeclarations()) {
      final List<AttributeDeclaration> attributeDeclarations =
          classDeclaration.getAttributeDeclarations();
      for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
        final String attributeName =
            String.format("attr_%s_%s",
              classDeclaration.getName(), attributeDeclaration.getName());
        this.attributeNames.put(attributeDeclaration.getSymbol(), attributeName);

        FileUtil.write(String.format("\tprivate Attribute %s;\n", attributeName), writer);
      }
    }

    FileUtil.write("\n", writer);
  }

  private final void writeClassDeclarations(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write("\t// ==========[ CLASSES ]==========\n\n", writer);

    for (final ClassDeclaration classDeclaration : languageSpecification.getClassDeclarations()) {
      final String className = String.format("class_%s", classDeclaration.getName());
      this.classNames.put(classDeclaration.getSymbol(), className);

      FileUtil.write(String.format("\tprivate i2.act.fuzzer.Class %s;\n", className), writer);
    }

    FileUtil.write("\n", writer);
  }

  private final void writeProductionDeclarations(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write("\t// ==========[ PRODUCTIONS ]==========\n\n", writer);

    for (final ClassDeclaration classDeclaration : languageSpecification.getClassDeclarations()) {
      if (classDeclaration instanceof ProductionClassDeclaration) {
        final ProductionClassDeclaration productionClassDeclaration =
            (ProductionClassDeclaration) classDeclaration;

        final List<ProductionDeclaration> productionDeclarations =
            productionClassDeclaration.getProductionDeclarations();
        for (final ProductionDeclaration productionDeclaration : productionDeclarations) {
          if (isProductionEnabled(productionDeclaration)) {
            final String productionName =
                String.format("prod_%s_%s",
                    classDeclaration.getName(), productionDeclaration.getName());
            this.productionNames.put(productionDeclaration.getSymbol(), productionName);

            FileUtil.write(String.format("\tprivate Production %s;\n", productionName), writer);
          }
        }
      }
    }

    FileUtil.write("\n", writer);
  }

  private final void writeConstructor(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write(
        String.format("\tpublic %s() {\n",
            this.javaClassName),
        writer);

    FileUtil.write("\t\tgenerateAttributes();\n", writer);
    FileUtil.write("\t\tgenerateClasses();\n", writer);
    FileUtil.write("\t\tgenerateProductions();\n", writer);
    FileUtil.write("\t\tgenerateAttributeRules();\n", writer);

    FileUtil.write("\t}\n\n", writer);
  }

  private final void writeGenerateAttributes(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write("\tprivate final void generateAttributes() {\n", writer);

    // create inherited/synthesized attributes first (since they do not have any dependencies)
    {
      FileUtil.write("\t\t// -----[ inh/syn attributes ]-----\n\n", writer);
      writeAttributeInstantiations(languageSpecification, writer, false);
    }

    // create guard attributes
    {
      FileUtil.write("\n\t\t// -----[ grd attributes ]-----\n\n", writer);
      writeAttributeInstantiations(languageSpecification, writer, true);
    }

    FileUtil.write("\t}\n\n", writer);
  }

  private final void writeAttributeInstantiations(final LaLaSpecification languageSpecification,
      final BufferedWriter writer, final boolean guardAttributes) {
    for (final ClassDeclaration classDeclaration : languageSpecification.getClassDeclarations()) {
      final String nodeClassName = this.nodeClassNames.get(classDeclaration.getSymbol());

      final List<AttributeDeclaration> attributeDeclarations =
          classDeclaration.getAttributeDeclarations();
      for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
        final AttributeModifierKind modifierKind =
            attributeDeclaration.getModifier().getModifierKind();

        final boolean isGuardAttribute = modifierKind == AttributeModifierKind.MOD_GRD;

        if ((guardAttributes && !isGuardAttribute) || (!guardAttributes && isGuardAttribute)) {
          continue;
        }

        final String attributeName = this.attributeNames.get(attributeDeclaration.getSymbol());
        final String attributeAvailableName =
            this.attributeAvailableNames.get(attributeDeclaration.getSymbol());

        FileUtil.write(
            String.format("\t\t%s = new Attribute(\"%s\", \"%s\", %s) {\n\n",
                attributeName, attributeDeclaration.getName(), classDeclaration.getName(),
                getAttributeKind(modifierKind)),
            writer);

        // public abstract boolean hasValue(final Node node);
        {
          FileUtil.write("\t\t\t@Override\n", writer);
          FileUtil.write("\t\t\tpublic final boolean hasValue(final Node node) {\n", writer);
          FileUtil.write(
              String.format("\t\t\t\treturn ((%s) node).%s;\n",
                  nodeClassName, attributeAvailableName),
              writer);

          FileUtil.write("\t\t\t}\n\n", writer);
        }

        // public abstract Object getValue(final Node node);
        {
          FileUtil.write("\t\t\t@Override\n", writer);
          FileUtil.write("\t\t\tpublic final Object getValue(final Node node) {\n", writer);
          FileUtil.write(
              String.format("\t\t\t\treturn ((%s) node).%s;\n",
                  nodeClassName, attributeName),
              writer);

          FileUtil.write("\t\t\t}\n\n", writer);
        }

        // public abstract void setValue(final Node node, final Object value);
        {
          FileUtil.write("\t\t\t@Override\n", writer);
          FileUtil.write(
              "\t\t\tpublic final void setValue(final Node node, final Object value) {\n",
              writer);

          final String attributeTypeName = getTypeName(attributeDeclaration);

          FileUtil.write(
              String.format("\t\t\t\t((%s) node).%s = (%s) value;\n",
                  nodeClassName, attributeName, attributeTypeName),
              writer);

          FileUtil.write(
              String.format("\t\t\t\t((%s) node).%s = true;\n",
                  nodeClassName, attributeAvailableName),
              writer);

          FileUtil.write("\t\t\t}\n\n", writer);
        }

        // public abstract boolean clearValue(final Node node);
        {
          FileUtil.write("\t\t\t@Override\n", writer);
          FileUtil.write("\t\t\tpublic final boolean clearValue(final Node node) {\n", writer);

          FileUtil.write(
              String.format("\t\t\t\tfinal boolean change = ((%s) node).%s;\n",
                  nodeClassName, attributeAvailableName),
              writer);

          FileUtil.write(
              String.format("\t\t\t\t((%s) node).%s = false;\n",
                  nodeClassName, attributeAvailableName),
              writer);

          FileUtil.write("\t\t\t\treturn change;\n", writer);

          FileUtil.write("\t\t\t}\n\n", writer);
        }

        FileUtil.write("\t\t};\n", writer);
      }
    }
  }

  private final void writeGenerateClasses(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write("\tprivate final void generateClasses() {\n", writer);

    int classId = 0;

    for (final ClassDeclaration classDeclaration : languageSpecification.getClassDeclarations()) {
      assert (this.classNames.containsKey(classDeclaration.getSymbol()));
      final String className = this.classNames.get(classDeclaration.getSymbol());

      assert (this.nodeClassNames.containsKey(classDeclaration.getSymbol()));
      final String nodeClassName = this.nodeClassNames.get(classDeclaration.getSymbol());

      FileUtil.write(String.format(
          "\t\t%s = new i2.act.fuzzer.Class(%d, \"%s\", %s, %s, %s, %s, new Attribute[] {",
          className, classId++, classDeclaration.getName(),
          String.valueOf(isUnit(classDeclaration)), String.valueOf(isList(classDeclaration)),
          String.valueOf(isGeneratorClass(classDeclaration)),
          String.valueOf(getGeneratorPrecedence(classDeclaration))),
          writer);

      // attributes
      {
        boolean first = true;
        final List<AttributeDeclaration> attributeDeclarations =
            classDeclaration.getAttributeDeclarations();
        for (final AttributeDeclaration attributeDeclaration : attributeDeclarations) {
          if (!first) {
            FileUtil.write(", ", writer);
          }
          first = false;

          assert (this.attributeNames.containsKey(attributeDeclaration.getSymbol()));
          final String attributeName = this.attributeNames.get(attributeDeclaration.getSymbol());

          FileUtil.write(attributeName, writer);
        }
      }

      FileUtil.write("}, ", writer);

      // regular expression for literals
      {
        if (classDeclaration instanceof LiteralClassDeclaration) {
          final String regularExpression =
              ((LiteralClassDeclaration) classDeclaration).getRegularExpressionString();
          final int literalCount = getCount(classDeclaration);
          FileUtil.write(String.format("\"%s\", %d", regularExpression, literalCount), writer);
        } else {
          assert (classDeclaration instanceof ProductionClassDeclaration);
          FileUtil.write("null, -1", writer);
        }
      }

      // max height
      {
        final int maxHeight = getMaxHeight(classDeclaration);
        FileUtil.write(String.format(", %d", maxHeight), writer);
      }

      // max width
      {
        final int maxWidth = getMaxWidth(classDeclaration);
        FileUtil.write(String.format(", %d", maxWidth), writer);
      }

      // max alternatives
      {
        final int maxAlternatives = getMaxAlternatives(classDeclaration);
        FileUtil.write(String.format(", %d", maxAlternatives), writer);
      }

      FileUtil.write(") {\n\n", writer);

      // public abstract Node createNode(final Node parent, final int allowedHeight,
      //     final int allowedWidth, final Node expected);
      {
        FileUtil.write("\t\t\t@Override\n", writer);
        FileUtil.write(
            "\t\t\tpublic final Node createNode(final Node parent, final int allowedHeight, "
            + "final int allowedWidth, final Node expected) {\n",
            writer);
        FileUtil.write(
            String.format(
                "\t\t\t\treturn new %s(this, parent, allowedHeight, allowedWidth, expected);\n",
                nodeClassName),
            writer);
        FileUtil.write("\t\t\t}\n\n", writer);
      }

      FileUtil.write("\t\t};\n\n", writer);
    }

    FileUtil.write("\t}\n\n", writer);
  }

  private final boolean isProductionEnabled(final ProductionDeclaration productionDeclaration) {
    if (this.features == null) {
      return true;
    }

    final String featureString = getFeatureString(productionDeclaration);

    if (featureString == null) {
      return true;
    }

    for (final String feature : featureString.split(",")) {
      if (this.features.contains(feature.trim())) {
        return true;
      }
    }

    return false;
  }

  private final void writeGenerateProductions(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write(
        "\tprivate final void generateProductions() {\n",
        writer);

    for (final ClassDeclaration classDeclaration : languageSpecification.getClassDeclarations()) {
      if (classDeclaration instanceof ProductionClassDeclaration) {
        final ProductionClassDeclaration productionClassDeclaration =
            (ProductionClassDeclaration) classDeclaration;

        int productionId = 0;

        final List<ProductionDeclaration> productionDelcarations =
            productionClassDeclaration.getProductionDeclarations();
        for (final ProductionDeclaration productionDeclaration : productionDelcarations) {
          if (isProductionEnabled(productionDeclaration)) {
            if (productionDeclaration instanceof TreeProductionDeclaration) {
              final TreeProductionDeclaration treeProductionDeclaration =
                  (TreeProductionDeclaration) productionDeclaration;
              writeGenerateTreeProduction(
                  treeProductionDeclaration, productionClassDeclaration, productionId++, writer);
            } else {
              assert (productionDeclaration instanceof GeneratorProductionDeclaration);
              // generator productions are instantiated dynamically at runtime
            }
          }
        }
      }
    }

    FileUtil.write("\t}\n\n", writer);
  }

  private final void writeGenerateTreeProduction(
      final TreeProductionDeclaration productionDeclaration,
      final ProductionClassDeclaration productionClassDeclaration, final int productionId,
      final BufferedWriter writer) {
    assert (this.classNames.containsKey(productionClassDeclaration.getSymbol()));
    final String className = this.classNames.get(productionClassDeclaration.getSymbol());

    assert (this.productionNames.containsKey(productionDeclaration.getSymbol()));
    final String productionName = this.productionNames.get(productionDeclaration.getSymbol());

    // determine index of each child
    {
      int index = 0;

      final List<ChildDeclaration> childDeclarations =
          productionDeclaration.getChildDeclarations();
      for (final ChildDeclaration childDeclaration : childDeclarations) {
        this.childIndexes.put(childDeclaration, index);
        ++index;
      }
    }

    final int weight = getWeight(productionDeclaration);

    FileUtil.write(
        String.format("\t\t%s = new Production(%d, \"%s\", %d, %s, new i2.act.fuzzer.Class[] {",
            productionName, productionId, productionDeclaration.getName(), weight, className),
        writer);

    // child classes
    {
      boolean first = true;
      final List<ChildDeclaration> childDeclarations =
          productionDeclaration.getChildDeclarations();
      for (final ChildDeclaration childDeclaration : childDeclarations) {
        if (!first) {
          FileUtil.write(", ", writer);
        }
        first = false;

        final ClassSymbol childClassSymbol = childDeclaration.getSymbol().getType();
        assert (this.classNames.containsKey(childClassSymbol));
        final String childClassName = this.classNames.get(childClassSymbol);

        FileUtil.write(childClassName, writer);
      }
    }

    FileUtil.write("}, new int[] {", writer);

    // child visitation order
    {
      boolean first = true;
      final List<Integer> childVisitationOrder =
          productionDeclaration.getSymbol().getChildVisitationOrder();
      assert (childVisitationOrder != null);
      for (final Integer childIndex : childVisitationOrder) {
        if (!first) {
          FileUtil.write(", ", writer);
        }
        first = false;

        FileUtil.write(childIndex.toString(), writer);
      }

      FileUtil.write("}", writer);
    }

    // isListRecursion
    {
      FileUtil.write(", " + isListRecursion(productionDeclaration), writer);
    }

    // precedence
    {
      FileUtil.write(", " + String.valueOf(getPrecedence(productionDeclaration)), writer);
    }

    FileUtil.write(") {\n\n", writer);

    // public abstract Node[] createChildrenFor(final Node node, final int maxRecursionDepth);
    {
      FileUtil.write("\t\t\t@Override\n", writer);
      FileUtil.write(
          "\t\t\tpublic final Node[] createChildrenFor(final Node node, "
          + "final int maxRecursionDepth) {\n", writer);

      FileUtil.write("\t\t\t\treturn new Node[] {", writer);

      boolean first = true;
      final List<ChildDeclaration> childDeclarations =
          productionDeclaration.getChildDeclarations();
      for (final ChildDeclaration childDeclaration : childDeclarations) {
        if (first) {
          FileUtil.write("\n\t\t\t\t\t", writer);
        } else {
          FileUtil.write(",\n\t\t\t\t\t", writer);
        }
        first = false;

        final ClassSymbol childClassSymbol = childDeclaration.getSymbol().getType();
        assert (this.classNames.containsKey(childClassSymbol));
        final String childClassName = this.classNames.get(childClassSymbol);

        assert (this.nodeClassNames.containsKey(childClassSymbol));

        FileUtil.write(
            String.format(
                "%s.createNode(node, allowedHeight(node, %s, maxRecursionDepth), "
                + " allowedWidth(node, %s))",
                childClassName, childClassName, childClassName),
            writer);
      }

      FileUtil.write("};\n\t\t\t}\n\n", writer);
    }

    // public abstract void printCode(final Node node, final StringBuilder builder,
    //    final int indentation);
    {
      final boolean isHidden =
          productionDeclaration.hasAnnotation(AnnotationSymbol.ANNOTATION_HIDDEN)
          || productionClassDeclaration.hasAnnotation(AnnotationSymbol.ANNOTATION_HIDDEN);

      FileUtil.write("\t\t\t@Override\n", writer);
      FileUtil.write(
          "\t\t\tpublic final void printCode(final Node node, "
          + "final StringBuilder builder, final int indentation) {\n", writer);

      if (isHidden) {
        FileUtil.write("\t\t\t\t// hidden\n", writer);
      } else {
        final Serialization serialization = productionDeclaration.getSerialization();
        final StringLiteral stringLiteral = serialization.getInterpolatedString();

        this.enclosingProductionDeclaration = productionDeclaration;

        final BaseLaLaSpecificationVisitor<BufferedWriter, Void> printCodeVisitor =
            new BaseLaLaSpecificationVisitor<BufferedWriter, Void>() {

          private int currentIndentation = 0;

          @Override
          public final Void visit(final EscapeSequence escapeSequence,
              final BufferedWriter writer) {
            switch (escapeSequence.getEscapeToken()) {
              case ESCAPE_NEWLINE: {
                FileUtil.write("\t\t\t\tbuilder.append(\"\\n\");\n", writer);
                FileUtil.write(
                    String.format("\t\t\t\tprintIndentation(builder, indentation + %d);\n",
                        this.currentIndentation),
                    writer);

                break;
              }
              case ESCAPE_INDENT: {
                ++this.currentIndentation;

                FileUtil.write("\t\t\t\tbuilder.append(\"\\n\");\n", writer);
                FileUtil.write(
                    String.format("\t\t\t\tprintIndentation(builder, indentation + %d);\n",
                        this.currentIndentation),
                    writer);

                break;
              }
              case ESCAPE_UNINDENT: {
                --this.currentIndentation;

                FileUtil.write("\t\t\t\tbuilder.append(\"\\n\");\n", writer);
                FileUtil.write(
                    String.format("\t\t\t\tprintIndentation(builder, indentation + %d);\n",
                        this.currentIndentation),
                    writer);

                break;
              }
              case ESCAPE_DOLLAR: {
                FileUtil.write("\t\t\t\tbuilder.append(\"$\");\n", writer);
                break;
              }
              case ESCAPE_HASH: {
                FileUtil.write("\t\t\t\tbuilder.append(\"#\");\n", writer);
                break;
              }
              case ESCAPE_QUOTE: {
                FileUtil.write("\t\t\t\tbuilder.append(\"\\\"\");\n", writer);
                break;
              }
              default: {
                throw new RPGException("unknown escape token: "
                    + escapeSequence.getStringRepresentation());
              }
            }

            return null;
          }

          @Override
          public final Void visit(final StringCharacters stringCharacters,
              final BufferedWriter writer) {
            FileUtil.write(
                String.format("\t\t\t\tbuilder.append(\"%s\");\n",
                    stringCharacters.getCharacters()),
                writer);

            return null;
          }

          @Override
          public final Void visit(final ChildDeclaration childDeclaration,
              final BufferedWriter writer) {
            assert (GenerateJavaSpec.this.childIndexes.containsKey(childDeclaration));
            final int childIndex = GenerateJavaSpec.this.childIndexes.get(childDeclaration);

            FileUtil.write("\t\t\t\t{\n", writer);
            if (childDeclaration.hasAutomaticParentheses()) {
              FileUtil.write(
                  String.format(
                      "\t\t\t\t\tfinal boolean needsParentheses = "
                      + "AutomaticParentheses.needsParentheses(node, node.getChild(%d));",
                      childIndex),
                  writer);
              FileUtil.write("\t\t\t\t\tif (needsParentheses) { builder.append('('); }\n", writer);
            }

            FileUtil.write(
                String.format("\t\t\t\t\tnode.getChild(%d).printCode(builder, indentation + %d);\n",
                    childIndex, this.currentIndentation),
                writer);

            if (childDeclaration.hasAutomaticParentheses()) {
              FileUtil.write("\t\t\t\t\tif (needsParentheses) { builder.append(')'); }\n", writer);
            }

            FileUtil.write("\t\t\t\t}\n", writer);

            return null;
          }

          @Override
          public final Void visit(final PrintCommand printCommand, final BufferedWriter writer) {
            final AttributeExpression expression = printCommand.getExpression(); 

            FileUtil.write("\t\t\t\ttry {\n", writer);

            FileUtil.write("\t\t\t\t\tfinal Object result = ", writer);
            expression.accept(GenerateJavaSpec.this, writer);
            FileUtil.write(";\n", writer);

            FileUtil.write("\t\t\t\t\tif (result instanceof EmbeddedCode) {\n", writer);
            FileUtil.write(
                String.format(
                    "\t\t\t\t\t\t((EmbeddedCode) result).printCode(builder, indentation + %d);\n",
                    this.currentIndentation),
                writer);
            FileUtil.write("\t\t\t\t\t} else if (result instanceof Printable) {\n", writer);
            FileUtil.write("\t\t\t\t\t\tbuilder.append(((Printable) result).print());\n", writer);
            FileUtil.write("\t\t\t\t\t} else {\n", writer);
            FileUtil.write("\t\t\t\t\t\tbuilder.append(result);\n", writer);
            FileUtil.write("\t\t\t\t\t}\n", writer);

            FileUtil.write("\t\t\t\t} catch (final Throwable t) {}\n", writer);

            return null;
          }

        };

        for (final StringElement stringElement : stringLiteral.getStringElements()) {
          stringElement.accept(printCodeVisitor, writer);
        }

        this.enclosingProductionDeclaration = null;
      }

      FileUtil.write("\t\t\t}\n\n", writer);
    }

    // public abstract void tokenize(final Node node, final List<String> tokens);
    {
      final boolean isHidden =
          productionDeclaration.hasAnnotation(AnnotationSymbol.ANNOTATION_HIDDEN)
          || productionClassDeclaration.hasAnnotation(AnnotationSymbol.ANNOTATION_HIDDEN);

      FileUtil.write("\t\t\t@Override\n", writer);
      FileUtil.write(
          "\t\t\tpublic final void tokenize(final Node node, final List<String> tokens) {\n",
          writer);

      if (isHidden) {
        FileUtil.write("\t\t\t\t// hidden\n", writer);
      } else {
        final Serialization serialization = productionDeclaration.getSerialization();
        final StringLiteral stringLiteral = serialization.getInterpolatedString();

        this.enclosingProductionDeclaration = productionDeclaration;

        final BaseLaLaSpecificationVisitor<BufferedWriter, Void> tokenizeVisitor =
            new BaseLaLaSpecificationVisitor<BufferedWriter, Void>() {

          @Override
          public final Void visit(final EscapeSequence escapeSequence,
              final BufferedWriter writer) {
            switch (escapeSequence.getEscapeToken()) {
              case ESCAPE_NEWLINE:
              case ESCAPE_INDENT:
              case ESCAPE_UNINDENT: {
                FileUtil.write("\t\t\t\ttokens.add(\"\\n\");\n", writer);
                break;
              }
              case ESCAPE_DOLLAR: {
                FileUtil.write("\t\t\t\ttokens.add(\"$\");\n", writer);
                break;
              }
              case ESCAPE_HASH: {
                FileUtil.write("\t\t\t\ttokens.add(\"#\");\n", writer);
                break;
              }
              case ESCAPE_QUOTE: {
                FileUtil.write("\t\t\t\ttokens.add(\"\\\"\");\n", writer);
                break;
              }
              default: {
                throw new RPGException("unknown escape token: "
                    + escapeSequence.getStringRepresentation());
              }
            }

            return null;
          }

          @Override
          public final Void visit(final StringCharacters stringCharacters,
              final BufferedWriter writer) {
            FileUtil.write(
                String.format("\t\t\t\ttokens.add(\"%s\");\n",
                    stringCharacters.getCharacters()),
                writer);

            return null;
          }

          @Override
          public final Void visit(final ChildDeclaration childDeclaration,
              final BufferedWriter writer) {
            assert (GenerateJavaSpec.this.childIndexes.containsKey(childDeclaration));
            final int childIndex = GenerateJavaSpec.this.childIndexes.get(childDeclaration);

            FileUtil.write("\t\t\t\t{\n", writer);
            if (childDeclaration.hasAutomaticParentheses()) {
              FileUtil.write(
                  String.format(
                      "\t\t\t\t\tfinal boolean needsParentheses = "
                      + "AutomaticParentheses.needsParentheses(node, node.getChild(%d));",
                      childIndex),
                  writer);
              FileUtil.write("\t\t\t\t\tif (needsParentheses) { tokens.add('('); }\n", writer);
            }

            FileUtil.write(
                String.format("\t\t\t\t\tnode.getChild(%d).tokenize(tokens);\n", childIndex),
                writer);

            if (childDeclaration.hasAutomaticParentheses()) {
              FileUtil.write("\t\t\t\t\tif (needsParentheses) { tokens.add(')'); }\n", writer);
            }

            FileUtil.write("\t\t\t\t}\n", writer);

            return null;
          }

          @Override
          public final Void visit(final PrintCommand printCommand, final BufferedWriter writer) {
            final AttributeExpression expression = printCommand.getExpression(); 

            FileUtil.write("\t\t\t\ttry {\n", writer);

            FileUtil.write("\t\t\t\t\tfinal Object result = ", writer);
            expression.accept(GenerateJavaSpec.this, writer);
            FileUtil.write(";\n", writer);

            FileUtil.write("\t\t\t\t\tif (result instanceof EmbeddedCode) {\n", writer);
            FileUtil.write("\t\t\t\t\t\ttokens.add(((EmbeddedCode) result).printCode());\n",
                writer);
            FileUtil.write("\t\t\t\t\t} else if (result instanceof Printable) {\n", writer);
            FileUtil.write("\t\t\t\t\t\ttokens.add(((Printable) result).print());\n", writer);
            FileUtil.write("\t\t\t\t\t} else {\n", writer);
            FileUtil.write("\t\t\t\t\t\ttokens.add(String.valueOf(result));\n", writer);
            FileUtil.write("\t\t\t\t\t}\n", writer);

            FileUtil.write("\t\t\t\t} catch (final Throwable t) {}\n", writer);

            return null;
          }

        };

        for (final StringElement stringElement : stringLiteral.getStringElements()) {
          stringElement.accept(tokenizeVisitor, writer);
        }

        this.enclosingProductionDeclaration = null;
      }

      FileUtil.write("\t\t\t}\n\n", writer);
    }

    // public abstract boolean isGeneratorNode();
    {
      FileUtil.write("\t\t\t@Override\n", writer);
      FileUtil.write(
          "\t\t\tpublic final boolean isGeneratorNode() {\n", writer);
      FileUtil.write("\t\t\t\treturn false;\n", writer);
      FileUtil.write("\t\t\t}\n\n", writer);
    }

    FileUtil.write("\t\t};\n", writer);

    // set weight of production
    FileUtil.write(
        String.format("\t\t%s.weight = %d;\n\n",
            productionName, getWeight(productionDeclaration)),
        writer);

    // add production to class
    FileUtil.write(
        String.format("\t\t%s.addProduction(%s);\n", className, productionName), writer);
  }

  private final void writeGenerateAttributeRules(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write("\tprivate final void generateAttributeRules() {\n", writer);

    for (final ClassDeclaration classDeclaration : languageSpecification.getClassDeclarations()) {
      if (classDeclaration instanceof ProductionClassDeclaration) {
        final ProductionClassDeclaration productionClassDeclaration =
            (ProductionClassDeclaration) classDeclaration;

        assert (this.classNames.containsKey(productionClassDeclaration.getSymbol()));
        final String className = this.classNames.get(productionClassDeclaration.getSymbol());

        final List<ProductionDeclaration> productionDeclarations =
            productionClassDeclaration.getProductionDeclarations();
        for (final ProductionDeclaration productionDeclaration : productionDeclarations) {
          if (!isProductionEnabled(productionDeclaration)) {
            continue;
          }

          assert (this.productionNames.containsKey(productionDeclaration.getSymbol()));
          final String productionName = this.productionNames.get(productionDeclaration.getSymbol());

          final List<AttributeEvaluationRule> attributeEvaluationRules =
              productionDeclaration.getAttributeEvaluationRules();
          for (final AttributeEvaluationRule attributeRule : attributeEvaluationRules) {
            final AttributeAccess targetAttribute = attributeRule.getTargetAttribute();

            assert (this.attributeNames.containsKey(targetAttribute.getSymbol()));
            final String targetAttributeName = this.attributeNames.get(targetAttribute.getSymbol());

            //public AttributeRule(final Attribute targetAttribute, final int targetNodeIndex,
            //    final Attribute[] sourceAttributes, final int[] sourceNodeIndexes) {

            final int targetNodeIndex = getChildIndex(targetAttribute, productionDeclaration);

            final String addMethodName;
            {
              if (productionDeclaration instanceof TreeProductionDeclaration) {
                addMethodName = productionName + ".addAttributeRule";
              } else {
                assert (productionDeclaration instanceof GeneratorProductionDeclaration);
                addMethodName = className + ".addGeneratorAttributeRule";
              }
            }

            FileUtil.write(
                String.format("\t\t%s(new AttributeRule(%s, %d, new Attribute[] {",
                    addMethodName, targetAttributeName, targetNodeIndex),
                writer);

            final List<AttributeAccess> sourceAttributes = attributeRule.gatherSourceAttributes();

            // source attributes
            {
              boolean first = true;
              for (final AttributeAccess sourceAttribute : sourceAttributes) {
                if (!first) {
                  FileUtil.write(", ", writer);
                }
                first = false;

                final AttributeSymbol sourceSymbol = sourceAttribute.getSymbol();

                assert (this.attributeNames.containsKey(sourceSymbol));
                final String sourceAttributeName = this.attributeNames.get(sourceSymbol);

                FileUtil.write(sourceAttributeName, writer);
              }
            }

            FileUtil.write("}, new int[] {", writer);

            // source node indexes
            {
              boolean first = true;
              for (final AttributeAccess sourceAttribute : sourceAttributes) {
                if (!first) {
                  FileUtil.write(", ", writer);
                }
                first = false;

                final int sourceChildIndex = getChildIndex(sourceAttribute, productionDeclaration);

                FileUtil.write(sourceChildIndex + "", writer);
              }
            }

            FileUtil.write("}) {\n\n", writer);

            // public abstract boolean alreadyComputed(final Node node);
            {
              FileUtil.write("\t\t\t@Override\n", writer);
              FileUtil.write(
                  "\t\t\tpublic final boolean alreadyComputed(final Node node) {\n",
                  writer);

              final String nodeAccess = getNodeAccess(targetAttribute, productionDeclaration);

              assert (this.attributeAvailableNames.containsKey(targetAttribute.getSymbol()));
              final String attributeAvailableName =
                  this.attributeAvailableNames.get(targetAttribute.getSymbol());

              FileUtil.write(
                  String.format("\t\t\t\treturn %s.%s;\n", nodeAccess, attributeAvailableName),
                  writer);

              FileUtil.write("\t\t\t}\n\n", writer);
            }

            // public abstract boolean allSourceAttributesAvailable(final Node node);
            {
              FileUtil.write("\t\t\t@Override\n", writer);
              FileUtil.write(
                  "\t\t\tpublic final boolean allSourceAttributesAvailable(final Node node) {\n",
                  writer);

              FileUtil.write("\t\t\t\treturn true", writer);

              for (final AttributeAccess sourceAttribute : sourceAttributes) {
                final String nodeAccess = getNodeAccess(sourceAttribute, productionDeclaration);

                assert (this.attributeAvailableNames.containsKey(sourceAttribute.getSymbol()));
                final String attributeAvailableName =
                    this.attributeAvailableNames.get(sourceAttribute.getSymbol());

                FileUtil.write(
                    String.format(" && (%s.%s)", nodeAccess, attributeAvailableName),
                    writer);
              }

              FileUtil.write(";\n", writer);

              FileUtil.write("\t\t\t}\n\n", writer);
            }

            // public abstract void compute(final Node node);
            {
              FileUtil.write("\t\t\t@Override\n", writer);
              FileUtil.write(
                  "\t\t\tpublic final void compute(final Node node) {\n",
                  writer);

              // actual computation
              {
                final String nodeAccess = getNodeAccess(targetAttribute, productionDeclaration);

                final AttributeSymbol attributeSymbol = targetAttribute.getSymbol();

                assert (this.attributeNames.containsKey(attributeSymbol));
                final String attributeName = this.attributeNames.get(attributeSymbol);
                final String attributeTypeName = attributeSymbol.getTypeName();

                FileUtil.write(
                    String.format("\t\t\t\t%s.%s = (%s) (",
                      nodeAccess, attributeName, attributeTypeName),
                    writer);

                this.enclosingProductionDeclaration = productionDeclaration;

                final AttributeExpression attributeExpression =
                    attributeRule.getAttributeExpression();
                attributeExpression.accept(this, writer);

                this.enclosingProductionDeclaration = null;

                FileUtil.write(");\n", writer);
              }

              // set has_<ATTRIBUTE>
              {
                final String nodeAccess = getNodeAccess(targetAttribute, productionDeclaration);

                assert (this.attributeAvailableNames.containsKey(targetAttribute.getSymbol()));
                final String attributeAvailableName =
                    this.attributeAvailableNames.get(targetAttribute.getSymbol());

                FileUtil.write(
                    String.format("\t\t\t\t%s.%s = true;\n", nodeAccess, attributeAvailableName),
                    writer);
              }

              FileUtil.write("\t\t\t}\n\n", writer);
            }

            FileUtil.write("\t\t});\n\n", writer);
          }
        }
      }
    }

    FileUtil.write("\t}\n\n", writer);
  }

  private void writeSpecificationFactory(final LaLaSpecification languageSpecification,
      final BufferedWriter writer) {
    FileUtil.write(
        "\tpublic final Specification createSpecification(long seed) {\n", writer);

    // specification
    final ClassDeclaration rootClassDeclaration =
        languageSpecification.getClassDeclarations().get(0);
    assert (this.classNames.containsKey(rootClassDeclaration.getSymbol()));
    final String rootClassName = this.classNames.get(rootClassDeclaration.getSymbol());

    FileUtil.write(String.format(
        "\t\treturn new Specification(%s, new i2.act.fuzzer.Class[] {",
        rootClassName), writer);

    // classes
    {
      boolean first = true;
      for (final ClassDeclaration classDeclaration : languageSpecification.getClassDeclarations()) {
        if (!first) {
          FileUtil.write(", ", writer);
        }
        first = false;

        assert (this.classNames.containsKey(classDeclaration.getSymbol()));
        final String className = this.classNames.get(classDeclaration.getSymbol());

        FileUtil.write(className, writer);
      }
    }
    FileUtil.write("}, DEFAULT_MAX_DEPTH, seed);\n", writer);

    FileUtil.write("\t}\n\n", writer);

    // factory
    {
      final String factoryName = getFactoryName();
      
      FileUtil.write(
          String.format(
              "\tpublic static final class %s implements SpecificationFactory {\n\n", factoryName),
          writer);

      FileUtil.write(
          String.format(
              "\t\tpublic static final %s INSTANCE = new %s();\n\n", factoryName, factoryName),
          writer);

      FileUtil.write("\t\t@Override\n", writer);
      FileUtil.write(
          "\t\tpublic final Specification createSpecification(final long seed) {\n", writer);
      FileUtil.write(
          String.format("\t\t\treturn (new %s()).createSpecification(seed);\n",
              this.javaClassName),
          writer);
      FileUtil.write("\t\t}\n\n", writer);

      FileUtil.write("\t}\n\n", writer);
    }
  }

  private final String getFactoryName() {
    return String.format("%sFactory", this.javaClassName);
  }

  private final void writeDeserialization(final BufferedWriter writer) {
    FileUtil.write("\tpublic static final class Deserialize {\n\n", writer);

    FileUtil.write("\t\tpublic static final void main(final String[] args) {\n", writer);

    // specification factory
    final String factoryInstance = String.format("%s.INSTANCE", getFactoryName());

    // call Deserialization main
    FileUtil.write(
        String.format("\t\t\tDeserialization.deserialization(%s, DEFAULT_MAX_DEPTH, args);\n",
            factoryInstance),
        writer);

    FileUtil.write("\t\t}\n\n", writer);

    FileUtil.write("\t}\n\n", writer);
  }

  private final void writeMain(final BufferedWriter writer) {
    FileUtil.write("\tpublic static final void main(final String[] args) {\n", writer);

    // specification factory
    final String factoryInstance = String.format("%s.INSTANCE", getFactoryName());

    // call fuzzer loop
    FileUtil.write(
        String.format(
            "\t\tFuzzerLoop.generatePrograms(%s, DEFAULT_MAX_DEPTH, args);\n", factoryInstance),
        writer);

    FileUtil.write("\t}\n\n", writer);
  }

  private final String getAttributeKind(final AttributeModifierKind modifierKind) {
    switch (modifierKind) {
      case MOD_INH: return "Attribute.AttributeKind.INHERITED";
      case MOD_SYN: return "Attribute.AttributeKind.SYNTHESIZED";
      case MOD_GRD: return "Attribute.AttributeKind.GUARD";
      default: {
        assert (false);
        return null;
      }
    }
  }

  private final String getTypeName(final AttributeDeclaration attributeDeclaration) {
    final boolean isGuardAttribute =
        attributeDeclaration.getModifier().getModifierKind() == AttributeModifierKind.MOD_GRD;

    if (isGuardAttribute) {
      return "boolean";
    } else {
      assert (attributeDeclaration.hasTypeName());
      return attributeDeclaration.getTypeName().getName().getName();
    }
  }

  private static final boolean isUnit(final ClassDeclaration classDeclaration) {
    // classes explicitly marked as @unit
    if (classDeclaration.isUnitClass()) {
      return true;
    }

    // generator classes
    if (isGeneratorClass(classDeclaration)) {
      return true;
    }

    return false;
  }

  private static final boolean isGeneratorClass(final ClassDeclaration classDeclaration) {
    return (classDeclaration instanceof ProductionClassDeclaration)
        && ((ProductionClassDeclaration) classDeclaration).isGeneratorClass();
  }

  private static final int getGeneratorPrecedence(final ClassDeclaration classDeclaration) {
    if (!isGeneratorClass(classDeclaration)) {
      return -1;
    }

    final ProductionClassDeclaration productionClassDeclaration =
        (ProductionClassDeclaration) classDeclaration;

    final List<ProductionDeclaration> productionDeclarations =
        productionClassDeclaration.getProductionDeclarations();

    assert (productionDeclarations.size() == 1);

    return getPrecedence(productionDeclarations.get(0));
  }

  private static final boolean isList(final ClassDeclaration classDeclaration) {
    return classDeclaration.hasAnnotation(AnnotationSymbol.ANNOTATION_LIST);
  }

  private final boolean isListRecursion(final ProductionDeclaration productionDeclaration) {
    final ClassDeclaration productionClassDeclaration = productionDeclaration.ownClassNode();

    if (isList(productionClassDeclaration)) {
      final ClassSymbol classSymbol = productionClassDeclaration.getSymbol();
      assert (classSymbol != null);

      final List<ChildDeclaration> childDeclarations = productionDeclaration.getChildDeclarations();
      for (final ChildDeclaration childDeclaration : childDeclarations) {
        final ClassSymbol childClassSymbol = childDeclaration.getSymbol().getType();
        if (classSymbol == childClassSymbol) {
          return true;
        }
      }
    }

    return false;
  }

  private static final int toIntValue(final Expression expression) {
    if (!(expression instanceof Constant)) {
      throw new RPGException("unsupported expression type: "
          + expression.getClass().getSimpleName());
    }

    final Constant constant = (Constant) expression;

    return constant.asInt().intValue();
  }

  private static final String toString(final Expression expression) {
    if (!(expression instanceof StringLiteral)) {
      throw new RPGException("unsupported expression type: "
          + expression.getClass().getSimpleName());
    }

    final StringLiteral literal = (StringLiteral) expression;

    if (literal.numberOfStringElements() != 1
        || !(literal.getStringElement(0) instanceof StringCharacters)) {
      throw new RPGException("expected plain string literal");
    }

    return ((StringCharacters) literal.getStringElement(0)).getCharacters();
  }

  private static final int getAnnotationValue(final AnnotatableDeclaration declaration,
      final AnnotationSymbol annotationSymbol, final int defaultValue) {
    final Annotation annotation = declaration.findAnnotation(annotationSymbol);

    if (annotation == null) {
      return defaultValue;
    }

    assert (annotation.getNumberOfArguments() == 1);
    final Expression annotationExpression = annotation.getArgument(0);

    final int value = toIntValue(annotationExpression);
    return value;
  }

  private static final String getAnnotationValue(final AnnotatableDeclaration declaration,
      final AnnotationSymbol annotationSymbol, final String defaultValue) {
    final Annotation annotation = declaration.findAnnotation(annotationSymbol);

    if (annotation == null) {
      return defaultValue;
    }

    assert (annotation.getNumberOfArguments() == 1);
    final Expression annotationExpression = annotation.getArgument(0);

    return toString(annotationExpression);
  }

  private static final int getWeight(final ProductionDeclaration productionDeclaration) {
    final AnnotationSymbol ANNOTATION_WEIGHT = AnnotationSymbol.ANNOTATION_WEIGHT;
    return getAnnotationValue(productionDeclaration, ANNOTATION_WEIGHT, 1);
  }

  private static final int getCount(final ClassDeclaration classDeclaration) {
    final AnnotationSymbol ANNOTATION_COUNT = AnnotationSymbol.ANNOTATION_COUNT;
    return getAnnotationValue(classDeclaration, ANNOTATION_COUNT, DEFAULT_LITERAL_COUNT);
  }

  private static final int getMaxHeight(final ClassDeclaration classDeclaration) {
    final AnnotationSymbol ANNOTATION_MAX_HEIGHT = AnnotationSymbol.ANNOTATION_MAX_HEIGHT;
    return getAnnotationValue(classDeclaration, ANNOTATION_MAX_HEIGHT, -1);
  }

  private static final int getMaxWidth(final ClassDeclaration classDeclaration) {
    final AnnotationSymbol ANNOTATION_LIST = AnnotationSymbol.ANNOTATION_LIST;
    final Annotation annotation = classDeclaration.findAnnotation(ANNOTATION_LIST);

    if (annotation == null || annotation.getNumberOfArguments() < 1) {
      return -1;
    } else {
      final Expression annotationExpression = annotation.getArgument(0);

      final int value = toIntValue(annotationExpression);
      return value;
    }
  }

  private static final int getMaxAlternatives(final ClassDeclaration classDeclaration) {
    final AnnotationSymbol ANNOTATION_MAX_ALTERNATIVES =
        AnnotationSymbol.ANNOTATION_MAX_ALTERNATIVES;
    return getAnnotationValue(classDeclaration, ANNOTATION_MAX_ALTERNATIVES, -1);
  }

  private static final String getFeatureString(final ProductionDeclaration productionDeclaration) {
    final AnnotationSymbol ANNOTATION_FEATURE = AnnotationSymbol.ANNOTATION_FEATURE;
    return getAnnotationValue(productionDeclaration, ANNOTATION_FEATURE, null);
  }

  private static final int getPrecedence(final ProductionDeclaration productionDeclaration) {
    final AnnotationSymbol ANNOTATION_PRECEDENCE = AnnotationSymbol.ANNOTATION_PRECEDENCE;
    return getAnnotationValue(productionDeclaration, ANNOTATION_PRECEDENCE, -1);
  }

  private static final int getChildIndex(final AttributeAccess attributeAccess,
      final ProductionDeclaration productionDeclaration) {
    final ChildSymbol childSymbol = (ChildSymbol) attributeAccess.getTargetName().getSymbol();
    return getChildIndex(childSymbol, productionDeclaration);
  }

  private static final int getChildIndex(final ChildSymbol childSymbol,
      final ProductionDeclaration productionDeclaration) {
    final ChildDeclaration childDeclaration = childSymbol.getDeclaration();

    if (childDeclaration == null) {
      // 'this' access
      return -1;
    }

    int index = 0;
    for (final ChildDeclaration otherChild : productionDeclaration.getChildDeclarations()) {
      if (otherChild == childDeclaration) {
        return index;
      }

      ++index;
    }

    throw new RPGException("unknown child: " + childSymbol.getName());
  }

  private final String getNodeClassName(final ChildSymbol childSymbol) {
    final ClassSymbol classSymbol = childSymbol.getType();

    assert (this.nodeClassNames.containsKey(classSymbol));
    return this.nodeClassNames.get(classSymbol);
  }

  private final String getNodeAccess(final AttributeAccess attributeAccess,
      final ProductionDeclaration productionDeclaration) {
    final ChildSymbol childSymbol = (ChildSymbol) attributeAccess.getTargetName().getSymbol();
    return getNodeAccess(childSymbol, productionDeclaration);
  }

  private final String getNodeAccess(final ChildSymbol childSymbol,
      final ProductionDeclaration productionDeclaration) {
    final String nodeClassName = getNodeClassName(childSymbol);
    final int childIndex = getChildIndex(childSymbol, productionDeclaration);

    if (childIndex == -1) {
      return String.format("((%s) node)", nodeClassName);
    } else {
      return String.format("((%s) node.getChild(%d))", nodeClassName, childIndex);
    }
  }

}
