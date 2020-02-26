package runtime;

public final class StdLib {

  public static final boolean True() {
    return true;
  }

  public static final boolean False() {
    return false;
  }

  public static final boolean or(final boolean first, final boolean second) {
    return first || second;
  }

  public static final boolean and(final boolean first, final boolean second) {
    return first && second;
  }

  public static final boolean not(final boolean value) {
    return !value;
  }

  public static final boolean ne(final boolean first, final boolean second) {
    return first != second;
  }

  public static final int zero() {
    return 0;
  }

  public static final int one() {
    return 1;
  }

  public static final int two() {
	  return 2;
  }

  public static final boolean equals(final Object first, final Object second) {
    return first.equals(second);
  }

  public static final boolean lt(final int first, final int second) {
    return first < second;
  }

  public static final boolean le(final int first, final int second) {
    return first <= second;
  }

  public static final boolean gt(final int first, final int second) {
    return first > second;
  }

  public static final boolean ge(final int first, final int second) {
    return first >= second;
  }

  public static final boolean eq(final int first, final int second) {
    return first == second;
  }

  public static final boolean ne(final int first, final int second) {
    return first != second;
  }

  public static final int add(final int a, final int b) {
    return a + b;
  }

  public static final int sub(final int a, final int b) {
    return a - b;
  }

  public static final int mul(final int a, final int b) {
    return a * b;
  }

  public static final int div(final int a, final int b) {
    return a / b;
  }

  public static final int toInt(final String string) {
    return Integer.parseInt(string);
  }

  public static final String concatStrings(final String s1, final String s2){
	return s1 + s2;
  }

  public static final int stringLen(final String s1){
	  return s1.length();
  }

  public static final boolean ne(final String first, final String second) {
    return !first.equals(second);
  }

  public static final boolean eq(final String first, final String second) {
    return first.equals(second);
  }

}
