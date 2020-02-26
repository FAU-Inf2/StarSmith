package runtime;

import java.math.BigInteger;

public class BigInt {

  public static BigInt getBigInt(final String number) {
    return new BigInt(number);
  }

  public static BigInt add(final BigInt i1, final BigInt i2) {
    return new BigInt(i1.numb.add(i2.numb));
  }

  public static BigInt sub(final BigInt i1, final BigInt i2) {
    return new BigInt(i1.numb.subtract(i2.numb));
  }

  public static BigInt multiply(final BigInt i1, final BigInt i2) {
    return new BigInt(i1.numb.multiply(i2.numb));
  }

  public static BigInt addSign(final BigInt i1, final String sgn) {
    if (sgn.equals("-")) {
      return BigInt.multiply(i1, BigInt.getBigInt("-1"));
    }

    return i1;
  }

  public static BigInt calc(final BigInt i1, final String op, final BigInt i2) {
    switch (op) {
      case "+": return BigInt.add(i1, i2);
      case "-": return BigInt.sub(i1, i2);
      case "*": return BigInt.multiply(i1, i2);
      default:
        throw new AssertionError("Unknown operation!");
    }
  }

  public static int compareTo(final BigInt i1, final BigInt i2) {
    return i1.numb.compareTo(i2.numb);
  }

  /*==================================================*/

  private BigInteger numb;

  private BigInt(final String number) {
    numb = new BigInteger(number);
  }

  private BigInt(final BigInteger bigint) {
    this.numb = bigint;
  }

}
