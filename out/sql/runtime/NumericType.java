package runtime;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.math.BigInteger;

import i2.act.fuzzer.Node;

public class NumericType extends Type {

  private final BigInt minval;
  private final BigInt maxval;

  public Type getCastFunctionSuperTypeM() {
    return Type.getDECIMAL();
  }

  public boolean hasLengthM() {
    return false;
  }

  public int getLengthM() {
    return -1;
  }

  public Type getTypeclass() {
    return Type.getDECIMAL();
  }

  public static final BigInt getMinValue(final NumericType NumericType) {
    return NumericType.minval;
  }

  public static final BigInt getMaxValue(final NumericType NumericType) {
    return NumericType.maxval;
  }

  public static final boolean isInValueRange(final NumericType NumericType, BigInt number) {
    return NumericType.isInRange(number);
  }

  public boolean isInRange(BigInt num) {
    return (BigInt.compareTo(getMinValue(this), num) <= 0
        && BigInt.compareTo(num, getMaxValue(this)) <= 0);
  }

  public static final NumericType getSmallestIntegerNumericType(String number) {
    BigInt num = BigInt.getBigInt(number);
    for (NumericType t : orderedIntegerTypes) {
      if (t.isInRange(num)) {
        return t;
      }
    }

    throw new RuntimeException("Number is 8 Byte, but no NumericType is fitting, not even bigint?");
  }

  public static final String getMathFunctionName(final Type t1, final Type t2, final String op) {
    assert isNumericType(t1);
    assert isNumericType(t2);

    String opname;
    switch (op) {
      case "+": opname = "add";
        break;
      case "-": opname = "sub";
        break;
      case "*": opname = "mul";
        break;
      default:
        opname = "unknown";
        break;
    }

    Type nt1 = t1 == Type.getDECIMAL() ? Type.getBIGINT() : t1;
    Type nt2 = t2 == Type.getDECIMAL() ? Type.getBIGINT() : t2;

    return "HELP_FUNCTIONS." + opname + "_" + nt1 + "_" + nt2;
  }

  public static final Type getSmallerType(final NumericType t1, final NumericType t2) {
    return orderedIntegerTypes.get(
        Math.min(orderedIntegerTypes.indexOf(t1),
          orderedIntegerTypes.indexOf(t2)));
  }

  public static final Type getBiggerType(final Type t1, final Type t2) {
    assert isNumericType(t1);
    assert isNumericType(t2);

    return getBiggerType((NumericType) t1, (NumericType) t2);
  }

  public static final Type getBiggerType(final NumericType t1, final NumericType t2) {
    return orderedIntegerTypes.get(
        Math.min(orderedIntegerTypes.indexOf(Type.getBIGINT()),
        Math.max(orderedIntegerTypes.indexOf(t1),
          orderedIntegerTypes.indexOf(t2))));
  }

  public static final NumericType getRandomFeasableIntegerNumericType(String number,
      final Node node) {
    BigInt num = BigInt.getBigInt(number);
    java.util.List<NumericType> matching_NumericTypes = new java.util.ArrayList<>();
    for (NumericType t : orderedIntegerTypes) {
      if (t.isInRange(num)) {
        matching_NumericTypes.add(t);
      }
    }

    return ListUtil.chooseRandom(matching_NumericTypes, node);
  }

  public boolean convertibleTo(Type to) {
    if (to == Type.getSUPERTYPE()) return true;
    if (!(to instanceof NumericType)) {
      return false;
    }

    int ind_from = orderedIntegerTypes.indexOf(this);
    int ind_to = orderedIntegerTypes.indexOf(to);

    assert ind_from != -1;
    assert ind_to != -1;

    return ind_from <= ind_to;
  }

  protected NumericType(final String name, String min, String max) {
    super(name);
    this.minval = BigInt.getBigInt(min);
    this.maxval = BigInt.getBigInt(max);
  }

}
