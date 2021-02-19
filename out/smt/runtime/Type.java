package runtime;

import i2.act.fuzzer.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class Type {

  @Override
  public abstract boolean equals(final Object other);
  
  private static final class PrimitiveType extends Type {

    public final String name;

    public PrimitiveType(final String name) {
      this.name = name;
    }

    @Override
    public final boolean equals(final Object other) {
      return this == other;
    }

  }

  private static final Type unknownSort = new PrimitiveType("<unknown>");

  private static final Type anySort = new PrimitiveType("<any>");

  private static final Type boolSort = new PrimitiveType("Bool");
  private static final Type intSort = new PrimitiveType("Int");
  private static final Type realSort = new PrimitiveType("Real");
  private static final Type stringSort = new PrimitiveType("String");
  private static final Type regLanSort = new Type() {

    @Override
    public final boolean equals(final Object other) {
      return this == other;
    }

  };

  public static final Type unknownSort() {
    return unknownSort;
  }

  public static final Type anySort() {
    return anySort;
  }

  public static final Type boolSort() {
    return boolSort;
  }

  public static final boolean isBoolSort(final Type sort) {
    return boolSort.equals(sort); 
  }

  public static final Type intSort() {
    return intSort;
  }

  public static final boolean isIntSort(final Type sort) {
    return intSort.equals(sort); 
  }

  public static final Type realSort() {
    return realSort;
  }

  public static final boolean isRealSort(final Type sort) {
    return realSort.equals(sort); 
  }

  public static final Type stringSort() {
    return stringSort;
  }

  public static final boolean isStringSort(final Type sort) {
    return stringSort.equals(sort);
  }

  public static final Type regLanSort() {
    return regLanSort;
  }

  public static final boolean isRegLanSort(final Type sort) {
    return regLanSort.equals(sort);
  }

  public static final boolean isNumberSort(final Type sort) {
    return isIntSort(sort) || isRealSort(sort);
  }

  public static final boolean isPrimitiveSort(final Type sort) {
    return (sort instanceof PrimitiveType);
  }


  // ===============================================================================================


  private static final class TupleType extends Type {

    public final List<Type> types;

    public TupleType() {
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


  public static final class FunctionType extends Type {
    
    public final Type returnType;
    public final Type parameterType;

    public FunctionType(final Type returnType, final Type parameterType) {
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

  public static final boolean isNullaryFunctionType(final Type type) {
    return isFunctionType(type) && getTupleTypeSize(getParameterType(type)) == 0;
  }

  public static final boolean isNonNullaryFunctionType(final Type type) {
    return isFunctionType(type) && getTupleTypeSize(getParameterType(type)) > 0;
  }


  // ===============================================================================================

  
  private static final class ArrayType extends Type {

    public final Type indexType;
    public final Type valueType;

    public ArrayType(final Type indexType, final Type valueType) {
      this.indexType = indexType;
      this.valueType = valueType;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof ArrayType)) {
        return false;
      }

      final ArrayType otherArrayType = (ArrayType) other;

      return this.indexType.equals(otherArrayType.indexType)
          && this.valueType.equals(otherArrayType.valueType);
    }

  }

 
  // ------------------------------------------------------


  public static final Type createArraySort(final Type indexType, final Type valueType) {
    return new ArrayType(indexType, valueType);
  }

  public static final boolean isArraySort(final Type type) {
    return (type instanceof ArrayType);
  }

  public static final Type getIndexSort(final Type arrayType) {
    return ((ArrayType) arrayType).indexType;
  }

  public static final Type getValueSort(final Type arrayType) {
    return ((ArrayType) arrayType).valueType;
  }


  // ===============================================================================================

  
  private static final class BitVectorType extends Type {

    public final int minWidth;
    public final int maxWidth;

    public BitVectorType(final int width) {
      this.minWidth = width;
      this.maxWidth = width;
    }

    public BitVectorType(final int minWidth, final int maxWidth) {
      this.minWidth = minWidth;
      this.maxWidth = maxWidth;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof BitVectorType)) {
        return false;
      }

      final BitVectorType otherBitVectorType = (BitVectorType) other;

      return (this.minWidth == otherBitVectorType.minWidth)
          && (this.maxWidth == otherBitVectorType.maxWidth);
    }

  }

 
  // ------------------------------------------------------


  private static final BitVectorType anyBitVector = new BitVectorType(-1, -1);

  public static final Type anyBitVector() {
    return anyBitVector;
  }

  public static final Type createBitVectorSort(final int width) {
    return new BitVectorType(width);
  }

  public static final Type createBitVectorRange(final int minWidth, final int maxWidth) {
    return new BitVectorType(minWidth, maxWidth);
  }

  public static final boolean isBitVectorSort(final Type type) {
    return (type instanceof BitVectorType);
  }

  public static final int getWidth(final Type type) {
    final BitVectorType bitVectorType = (BitVectorType) type;

    if (bitVectorType.minWidth != bitVectorType.maxWidth) {
      throw new RuntimeException("width of bit vector undefinded");
    }

    return bitVectorType.minWidth;
  }

  public static final int getMinWidth(final Type type) {
    return ((BitVectorType) type).minWidth;
  }

  public static final int getMaxWidth(final Type type) {
    return ((BitVectorType) type).maxWidth;
  }

  public static final BitVectorType concat(final Type bitVectorOne, final Type bitVectorTwo) {
    final int widthOne = getWidth(bitVectorOne);
    final int widthTwo = getWidth(bitVectorTwo);

    return new BitVectorType(widthOne + widthTwo);
  }

  private static final Random rng = new Random();
  private static final int MAX_UPPER_RANDOM = 32;

  public static final int randomUpper(final Node node, final Type expectedType) {
    final int minWidth = getMinWidth(expectedType);

    final int minUpper;
    {
      if (minWidth == -1) {
        minUpper = 0;
      } else {
        minUpper = minWidth - 1;
      }
    }

    rng.setSeed(node.id);

    return minUpper + rng.nextInt(MAX_UPPER_RANDOM);
  }

  public static final int computeLower(final Node node, final Type expectedType,
      final int upper) {
    final int minWidth = getMinWidth(expectedType);
    final int maxWidth = getMaxWidth(expectedType);

    rng.setSeed(node.id);

    if (minWidth == -1 && maxWidth == -1) {
      return rng.nextInt(upper + 1);
    }

    if (minWidth == maxWidth) {
      return upper - maxWidth + 1;
    }

    if (minWidth == -1) {
      return rng.nextInt(upper + 1);
    }

    if (maxWidth == -1) {
      return rng.nextInt(upper - minWidth + 2);
    }

    final int minLower = Math.max(0, upper - maxWidth + 1);
    final int maxLower = upper - minWidth + 1;

    return minLower + rng.nextInt(maxLower - minLower + 1);
  }


  // ===============================================================================================

  private static final class FloatingPointType extends Type {

    public final int minExpBits;  // bits of the exponent
    public final int maxExpBits;  // bits of the exponent
    public final int minSigfBits; // bits of the significant
    public final int maxSigfBits; // bits of the significant

    public FloatingPointType(final int expBits, final int sigfBits) {
      this(expBits, expBits, sigfBits, sigfBits);
    }

    public FloatingPointType(final int minExpBits, final int maxExpBits, final int minSigfBits,
        final int maxSigfBits) {
      this.minExpBits = minExpBits;
      this.maxExpBits = maxExpBits;
      this.minSigfBits = minSigfBits;
      this.maxSigfBits = maxSigfBits;
    }

    @Override
    public final boolean equals(final Object other) {
      if (other instanceof FloatingPointType) {

        final FloatingPointType otherFloatingPointType = (FloatingPointType) other;

        return (this.minExpBits == otherFloatingPointType.minExpBits)
            && (this.maxExpBits == otherFloatingPointType.maxExpBits)
            && (this.minSigfBits == otherFloatingPointType.minSigfBits)
            && (this.maxSigfBits == otherFloatingPointType.maxSigfBits);
      }

      return false;
    }
  }

  // ------------------------------------------------------

  private static final FloatingPointType anyFloatingPoint = new FloatingPointType(-1, -1);

  public static final Type anyFloatingPoint() {
    return anyFloatingPoint;
  }

  public static final Type createFloatingPointSort(final int expBits, final int sigfBits) {
    return new FloatingPointType(expBits, sigfBits);
  }

  public static final boolean isFloatingPointSort(final Type type) {
    return (type instanceof FloatingPointType);
  }

  public static final int getMinExponentWidth(final Type type) {
    return ((FloatingPointType) type).minExpBits;
  }

  public static final int getMaxExponentWidth(final Type type) {
    return ((FloatingPointType) type).maxExpBits;
  }

  public static final int getMinSignificantWidth(final Type type) {
    return ((FloatingPointType) type).minSigfBits;
  }

  public static final int getMaxSignificantWidth(final Type type) {
    return ((FloatingPointType) type).maxSigfBits;
  }

  public static final int randomFPWidth(final Node node, final int lower, final int upper) {
    if ((lower == -1) && (upper == -1)) {
      return randomFPWidth(node, 2, 15);
    }
    if (lower == -1) {
      return randomFPWidth(node, 2, upper);
    }
    if (upper == -1) {
      return randomFPWidth(node, lower, lower + 15);
    }

    if (lower >= upper) {
      return upper;
    }

    if (lower <= 1) {
      throw new IllegalArgumentException("Width must be greater than 1");
    }

    rng.setSeed(node.id);

    return lower + rng.nextInt(upper - lower + 1);
  }



  // ===============================================================================================


  public static final boolean equals(final Type first, final Type second) {
    return first.equals(second);
  }

  public static final boolean assignable(final Type source, final Type target) {
    if (isArraySort(target)) {
      if (!isArraySort(source)) {
        return false;
      }

      final ArrayType targetArray = (ArrayType) target;
      final ArrayType sourceArray = (ArrayType) source;

      return assignable(sourceArray.indexType, targetArray.indexType)
          && assignable(sourceArray.valueType, targetArray.valueType);
    }

    if (isBitVectorSort(target)) {
      if (!isBitVectorSort(source)) {
        return false;
      }

      if (anyBitVector.equals(target)) {
        return true;
      }

      final BitVectorType targetBitVector = (BitVectorType) target;
      final BitVectorType sourceBitVector = (BitVectorType) source;

      final int targetMinWidth = targetBitVector.minWidth;
      final int targetMaxWidth = targetBitVector.maxWidth;

      final int sourceMinWidth = sourceBitVector.minWidth;
      final int sourceMaxWidth = sourceBitVector.maxWidth;

      return ((targetMinWidth == -1) || (sourceMinWidth >= targetMinWidth))
          && ((targetMaxWidth == -1) || (sourceMaxWidth <= targetMaxWidth));
    }

    if (isFloatingPointSort(target)) {
      if (!isFloatingPointSort(source)) {
        return false;
      }

      if (anyFloatingPoint.equals(target)) {
        return true;
      }

      final FloatingPointType targetFloatingPoint = (FloatingPointType) target;
      final FloatingPointType sourceFloatingPoint = (FloatingPointType) source;

      final int targetMinExp = targetFloatingPoint.minExpBits;
      final int targetMaxExp = targetFloatingPoint.maxExpBits;
      final int targetMinSigf = targetFloatingPoint.minSigfBits;
      final int targetMaxSigf = targetFloatingPoint.maxSigfBits;

      final int sourceMinExp = sourceFloatingPoint.minExpBits;
      final int sourceMaxExp = sourceFloatingPoint.maxExpBits;
      final int sourceMinSigf = sourceFloatingPoint.minSigfBits;
      final int sourceMaxSigf = sourceFloatingPoint.maxSigfBits;

      return ((targetMinExp == -1) || (sourceMinExp >= targetMinExp))
          && ((targetMaxExp == -1) || (sourceMaxExp <= targetMaxExp))
          && ((targetMinSigf == -1) || (sourceMinSigf >= targetMinSigf))
          && ((targetMaxSigf == -1) || (sourceMaxSigf <= targetMaxSigf));
    }

    return (anySort.equals(target) && isPrimitiveSort(source)) || source.equals(target);
  }

}
