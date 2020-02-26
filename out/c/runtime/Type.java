package runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public abstract class Type {

  public final boolean isConst;

  public Type(final boolean isConst) {
    this.isConst = isConst;
  }

  // ===============================================================================================

  @Override
  public abstract boolean equals(final Object other);

  @Override
  public abstract int hashCode();

  private static class PrimitiveType extends Type {
    
    public final String name;

    public PrimitiveType(final String name, final boolean isConst) {
      super(isConst);

      this.name = name;
    }

    @Override
    public final String toString() {
      return (this.isConst ? "const " : "") + this.name;
    }

    @Override
    public final boolean equals(final Object other) {
      return this == other;
    }

    @Override
    public final int hashCode() {
      return System.identityHashCode(this);
    }

  }

  //private static final class IntType extends PrimitiveType {

  //  public final int width;
  //  public final boolean signed;

  //  public IntType(final int width, final boolean signed) {
  //    super(String.format("%sint%d_t", (signed ? "" : "u"), width));

  //    this.width = width;
  //    this.signed = signed;
  //  }

  //  @Override
  //  public final boolean equals(final Object other) {
  //    if (!(other instanceof IntType)) {
  //      return false;
  //    }

  //    final IntType otherIntType = (IntType) other;

  //    return (otherIntType.width == this.width)
  //        && (otherIntType.signed == this.signed);
  //  }

  //}

  // ------------------------------------------------------

  private static final PrimitiveType anyType = new PrimitiveType("<any>", false);
  private static final PrimitiveType anyNumberType = new PrimitiveType("<any number type>", false);

  private static final PrimitiveType unknownType = new PrimitiveType("<unknown>", false);
  private static final PrimitiveType voidType = new PrimitiveType("void", false);

  private static final PrimitiveType intType = new PrimitiveType("int", false);
  private static final PrimitiveType realType = new PrimitiveType("double", false);

  private static final PrimitiveType intTypeConst = new PrimitiveType("int", true);
  private static final PrimitiveType realTypeConst = new PrimitiveType("double", true);

  public static final Type anyType() {
    return anyType;
  }

  public static final Type anyNumberType() {
    return anyNumberType;
  }

  public static final Type voidType() {
    return voidType;
  }

  public static final Type intType(final boolean isConst) {
    if (isConst) {
      return intTypeConst;
    } else {
      return intType;
    }
  }

  public static final boolean isIntType(final Type type) {
    return intType.equals(type) || intTypeConst.equals(type);
  }

  //public static final Type intType(final int width, final boolean signed) {
  //  return new IntType(width, signed);
  //}

  public static final Type realType(final boolean isConst) {
    if (isConst) {
      return realTypeConst;
    } else {
      return realType;
    }
  }

  public static final boolean isRealType(final Type type) {
    return realType.equals(type) || realTypeConst.equals(type);
  }


  // ===============================================================================================


  private static final class BitFieldType extends Type {

    public final Type baseType;
    public final int width;

    public BitFieldType(final Type baseType, final int width) {
      super(baseType.isConst);
      this.baseType = baseType;
      this.width = width;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof BitFieldType)) {
        return false;
      }

      final BitFieldType otherBitFieldType = (BitFieldType) other;
      return this.baseType.equals(otherBitFieldType.baseType)
          && this.width == otherBitFieldType.width;
    }

    @Override
    public final int hashCode() {
      return Objects.hash(this.baseType, this.width);
    }

  }


  // ------------------------------------------------------


  public static final Type createBitFieldType(final Type baseType, final int width) {
    return new BitFieldType(baseType, width);
  }

  public static final boolean isBitFieldType(final Type type) {
    return (type instanceof BitFieldType);
  }

  public static final boolean bitFieldAllowed(final Type type,
      final boolean bitFieldAllowed) {
    if (bitFieldAllowed) {
      return true;
    } else {
      return !isBitFieldType(type);
    }
  }


  // ===============================================================================================


  private static final class ArrayType extends Type {

    public final Type baseType;
    public final int dimensionality;

    public ArrayType(final Type baseType, final int dimensionality) {
      super(false); // if the base type is const, this is handled in 'baseType'

      this.baseType = baseType;
      this.dimensionality = dimensionality;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof ArrayType)) {
        return false;
      }

      final ArrayType otherArrayType = (ArrayType) other;

      return (this.dimensionality == otherArrayType.dimensionality)
          && (this.baseType.equals(otherArrayType.baseType));
    }

    @Override
    public final int hashCode() {
      return Objects.hash(this.baseType, this.dimensionality);
    }

    @Override
    public final String toString() {
      return String.format("Array<%s, %d>", this.baseType, this.dimensionality);
    }

  }

 
  // ------------------------------------------------------


  public static final Type createOneDimensionalArrayType(final Type baseType) {
    return new ArrayType(baseType, 1);
  }

  public static final Type extendArrayType(final Type _arrayType) {
    final ArrayType arrayType = (ArrayType) _arrayType;
    return new ArrayType(arrayType.baseType, arrayType.dimensionality + 1);
  }

  public static final Type narrowArrayType(final Type _arrayType) {
    final ArrayType arrayType = (ArrayType) _arrayType;

    if (arrayType.dimensionality == 1) {
      throw new RuntimeException("already a one-dimensional array");
    }

    return new ArrayType(arrayType.baseType, arrayType.dimensionality - 1);
  }

  public static final Type getArrayBaseType(final Type arrayType) {
    return ((ArrayType) arrayType).baseType;
  }

  public static final int getArrayDimensionality(final Type arrayType) {
    return ((ArrayType) arrayType).dimensionality;
  }

  public static final boolean isArrayType(final Type type) {
    return (type instanceof ArrayType);
  }
  

  // ===============================================================================================


  private static final class PointerType extends Type {

    public final Type pointeeType;

    public PointerType(final Type pointeeType, final boolean isConst) {
      super(isConst);

      this.pointeeType = pointeeType;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof PointerType)) {
        return false;
      }

      final PointerType otherPointerType = (PointerType) other;
      return (this.isConst == otherPointerType.isConst)
          && (this.pointeeType.equals(otherPointerType.pointeeType));
    }

    @Override
    public final int hashCode() {
      return Objects.hash(this.isConst, this.pointeeType);
    }

  }


  // ------------------------------------------------------


  public static final Type createPointerType(final Type pointeeType, final boolean isConst) {
    return new PointerType(pointeeType, isConst);
  }

  public static final boolean isPointerType(final Type type) {
    return (type instanceof PointerType);
  }

  public static final Type getPointeeType(final Type type) {
    return ((PointerType) type).pointeeType;
  }

  public static final int getPointerDepth(final Type type) {
    if (type instanceof PointerType) {
      return 1 + getPointerDepth(((PointerType) type).pointeeType);
    }
    return 0;
  }



  // ===============================================================================================


  private static final class TupleType extends Type {

    public final List<Type> types;

    public TupleType() {
      super(false);

      this.types = new ArrayList<Type>();
    }

    public final TupleType clone() {
      final TupleType clone = new TupleType();

      for (final Type type : this.types) {
        clone.types.add(type);
      }

      return clone;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof TupleType)) {
        return false;
      }

      final TupleType otherTupleType = (TupleType) other;
      return this.types.equals(otherTupleType.types);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.types);
    }

    @Override
    public final String toString() {
      final StringBuilder builder = new StringBuilder();

      builder.append("(");

      boolean first = true;
      for (final Type type : this.types) {
        if (!first) {
          builder.append(", ");
        }
        first = false;

        builder.append(type);
      }

      builder.append(")");

      return builder.toString();
    }

  }

 
  // ------------------------------------------------------
  

  public static final Type createEmptyTupleType() {
    return new TupleType();
  }

  public static final Type createTupleType(final Type firstElement) {
    final TupleType tupleType = new TupleType();
    tupleType.types.add(firstElement);

    return tupleType;
  }

  public static final Type mergeTupleTypes(final Type first, final Type second) {
    final TupleType merged = ((TupleType) first).clone();
    merged.types.addAll(((TupleType) second).types);

    return merged;
  }

  public static final int getTupleTypeSize(final Type tupleType) {
    return ((TupleType) tupleType).types.size();
  }

  public static final Type getTupleTypeHead(final Type tupleType) {
    if (((TupleType) tupleType).types.isEmpty()) {
      throw new RuntimeException("tuple is empty");
    }
    return ((TupleType) tupleType).types.get(0);
  }

  public static final Type getTupleTypeTail(final Type tupleType) {
    if (((TupleType) tupleType).types.isEmpty()) {
      throw new RuntimeException("tuple is empty");
    }

    final TupleType tailType = new TupleType();

    final int numberOfTypes = ((TupleType) tupleType).types.size();
    for (int index = 1; index < numberOfTypes; ++index) {
      tailType.types.add(((TupleType) tupleType).types.get(index));
    }

    return tailType;
  }


  // ===============================================================================================


  private static final class FunctionType extends Type {
    
    public final Type returnType;
    public final Type parameterType;

    public FunctionType(final Type returnType, final Type parameterType) {
      super(false);

      this.returnType = returnType;
      this.parameterType = parameterType;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof FunctionType)) {
        return false;
      }

      final FunctionType otherFunctionType = (FunctionType) other;

      return this.returnType.equals(otherFunctionType.returnType)
          && this.parameterType.equals(otherFunctionType.parameterType);
    }

    @Override
    public final int hashCode() {
      return Objects.hash(this.returnType, this.parameterType);
    }

  }


  public static final Type createFunctionType(final Type returnType, final Type parameterType) {
    return new FunctionType(returnType, parameterType);
  }

  public static final Type getReturnType(final Type functionType) {
    return ((FunctionType) functionType).returnType;
  }

  public static final Type getParameterType(final Type functionType) {
    return ((FunctionType) functionType).parameterType;
  }

  public static final boolean isFunctionType(final Type type) {
    return (type instanceof FunctionType);
  }



  // ===============================================================================================


  private static class CompositeType extends Type {

    public static enum Kind {
      STRUCT,
      UNION,
      ANY;
    }

    public final Kind kind;
    public final String name;
    public final LinkedHashMap<Symbol, Type> members;

    public CompositeType(final Kind kind, final String name) {
      this(kind, name, new LinkedHashMap<Symbol, Type>());
    }

    public CompositeType(final Kind kind, final String name,
        final LinkedHashMap<Symbol, Type> members) {
      super(false);

      this.kind = kind;
      this.name = name;
      this.members = members;
    }

    public final boolean isStruct() {
      return this.kind == Kind.STRUCT;
    }

    public final boolean isUnion() {
      return this.kind == Kind.UNION;
    }

    @Override
    public boolean equals(final Object other) {
      if (!(other instanceof CompositeType)) {
        return false;
      }

      final CompositeType otherCompositeType = (CompositeType) other;

      return this.kind == otherCompositeType.kind
          && this.name.equals(otherCompositeType.name)
          && this.members.equals(otherCompositeType.members);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.kind, this.name, this.members);
    }

    @Override
    public String toString() {
      return String.format("Composite<%s, %s, %s>", this.kind, this.name, this.members);
    }

  }

  private static final class AnyCompositeType extends CompositeType {

    public final Type expectedMember;

    public AnyCompositeType() {
      this(null);
    }

    public AnyCompositeType(final Type expectedMember) {
      super(CompositeType.Kind.ANY, null);
      this.expectedMember = expectedMember;
    }

    @Override
    public final boolean equals(final Object other) {
      return other == this;
    }

    @Override
    public final int hashCode() {
      return Objects.hash(this.expectedMember);
    }

    @Override
    public String toString() {
      return String.format("AnyComposite<%s>", this.expectedMember);
    }

  }

  public static final CompositeType ANY_COMPOSITE = new AnyCompositeType();

 
  // ------------------------------------------------------


  public static final Type anyComposite() {
    return ANY_COMPOSITE;
  }

  public static final Type anyComposite(final Type expectedMember) {
    return new AnyCompositeType(expectedMember);
  }

  private static final CompositeType createCompositeType(final CompositeType.Kind kind,
      final String name, final SymbolTable memberSymbols) {
    final LinkedHashMap<Symbol, Type> members = new LinkedHashMap<>();

    assert (memberSymbols.scopes.size() == 1);
    for (final Symbol member : memberSymbols.scopes.get(0).values()) {
      members.put(member, member.type);
    }

    return new CompositeType(kind, name, members);
  }

  public static final Type createStructType(final String name, final SymbolTable memberSymbols) {
    return createCompositeType(CompositeType.Kind.STRUCT, name, memberSymbols);
  }

  public static final Type createUnionType(final String name, final SymbolTable memberSymbols) {
    return createCompositeType(CompositeType.Kind.UNION, name, memberSymbols);
  }

  public static final boolean isCompositeType(final Type type) {
    return (type instanceof CompositeType);
  }

  public static final boolean isStructType(final Type type) {
    return (type instanceof CompositeType) && (((CompositeType) type).isStruct());
  }

  public static final boolean isUnionType(final Type type) {
    return (type instanceof CompositeType) && (((CompositeType) type).isUnion());
  }

  public static final String getName(final Type type) {
    return ((CompositeType) type).name;
  }

  public static final SymbolTable getMembers(final Type type) {
    final CompositeType compositeType = (CompositeType) type;

    final SymbolTable members = new SymbolTable(true);
    for (final Symbol symbol : compositeType.members.keySet()) {
      members.put(symbol);
    }

    return members;
  }

  public static final boolean hasMemberOfType(final Type _compositeType, final Type expectedType) {
    if (!(_compositeType instanceof CompositeType)) {
      return false;
    }

    final CompositeType compositeType = (CompositeType) _compositeType;
    for (final Type memberType : compositeType.members.values()) {
      if (assignable(memberType, expectedType)) {
        return true;
      }
    }
    return false;
  }

  public static final TupleType toTupleType(final Type compositeType) {
    final TupleType tupleType = new TupleType();

    for (final Type memberType : ((CompositeType) compositeType).members.values()) {
      tupleType.types.add(memberType);
    }

    return tupleType;
  }


  // ===============================================================================================


  public static final Type fromBinaryOperator(final Type lhs, final Type rhs) {
    if (isRealType(lhs) && assignable(rhs, lhs)) {
      return realType;
    }

    if (isRealType(rhs) && assignable(lhs, rhs)) {
      return realType;
    }

    if (isIntType(lhs) && isIntType(rhs)) {
      return intType;
    }

    return unknownType;
  }

  public static final boolean equals(final Type first, final Type second) {
    return first.equals(second);
  }

  public static final boolean isConst(final Type type) {
    return type.isConst;
  }

  public static final boolean isNumberType(final Type type) {
    if (isBitFieldType(type)) {
      return isNumberType(((BitFieldType) type).baseType);
    }
    return (isIntType(type) || isRealType(type));
  }

  public static final boolean isAssignableType(final Type type) {
    //if (type.isConst) {
    //  return false;
    //}

    return isNumberType(type) || isPointerType(type) || isCompositeType(type);
  }

  public static final boolean assignable(final Type sourceType, final Type targetType) {
    //if (anyType.equals(targetType)) {
    //  return true;
    //}

    if (unknownType.equals(sourceType) || unknownType.equals(targetType)) {
      return false;
    }

    if (isFunctionType(sourceType)) {
      return false;
    }

    if (isArrayType(sourceType)) {
      return false;
    }

    if (isCompositeType(sourceType) || isCompositeType(targetType)) {
      if (targetType == ANY_COMPOSITE) {
        return isCompositeType(sourceType);
      }

      if (sourceType.equals(targetType)) {
        return true;
      }

      if (targetType instanceof AnyCompositeType) {
        final AnyCompositeType anyTargetType = (AnyCompositeType) targetType;
        assert (anyTargetType.expectedMember != null);

        return hasMemberOfType(sourceType, anyTargetType.expectedMember);
      }

      return false;
    }

    if (voidType.equals(sourceType) || voidType.equals(targetType)) {
      return false;
    }

    //if ((sourceType instanceof IntType) && (targetType instanceof IntType)) {
    //  final IntType sourceInt = (IntType) sourceType;
    //  final IntType targetInt = (IntType) targetType;

    //  return (sourceInt.signed == targetInt.signed)
    //      && (sourceInt.width <= targetInt.width);
    //}

    if (anyNumberType.equals(targetType) && isNumberType(sourceType)) {
      return true;
    }

    if (isPointerType(sourceType) && isPointerType(targetType)) {
      final PointerType sourcePointerType = (PointerType) sourceType;
      final PointerType targetPointerType = (PointerType) targetType;
      return assignable(sourcePointerType.pointeeType, targetPointerType.pointeeType);
    }

    //if ((targetType instanceof PointerType) && (isIntType(sourceType))) {
    //  return true;
    //}

    if (isBitFieldType(targetType)) {
      if (isBitFieldType(sourceType)) {
        return assignable(
            ((BitFieldType) sourceType).baseType, ((BitFieldType) targetType).baseType);
      } else {
        return assignable(sourceType, ((BitFieldType) targetType).baseType);
      }
    }

    return anyType.equals(targetType)
        || (isRealType(targetType) && (isRealType(sourceType) || isIntType(sourceType)))
        || (isIntType(targetType) && isIntType(sourceType))
        || equals(sourceType, targetType);
  }

}
