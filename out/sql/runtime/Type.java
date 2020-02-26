package runtime;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Arrays;

import java.math.BigInteger;

import i2.act.fuzzer.Node;

/* Data type ranges:
 *
 *  https://stackoverflow.com/questions/2991405/what-is-the-difference-between-tinyint-smallint-mediumint-bigint-and-int-in-m
 *    #             | Bytes    Range (signed)                               Range (unsigned)
 *    # tinyint     | 1 byte   -128 to 127                                  0 to 255 // not implemented by postgres
 *    # smallint    | 2 bytes  -32768 to 32767                              0 to 65535
 *    # mediumint   | 3 bytes  -8388608 to 8388607                          0 to 16777215 // not implemented by postgres
 *    # int/integer | 4 bytes  -2147483648 to 2147483647                    0 to 4294967295
 *    # bigint      | 8 bytes  -9223372036854775808 to 9223372036854775807  0 to 18446744073709551615
 *
 */

public abstract class Type {

  private static NumericType SMALLINT = new SMALLINTType();
  private static NumericType INTEGER = new INTEGERType();
  private static NumericType BIGINT = new BIGINTType();
  private static NumericType DECIMAL = new DECIMALType();
  private static Type CHARACTERSTRINGLITERAL = new CHARACTERSTRINGLITERALType();
  private static Type BOOLEAN = new BOOLEANType();
  private static Type SUPERTYPE = new SUPERTYPE();

  protected static ArrayList<NumericType> orderedIntegerTypes =
      new ArrayList<NumericType>(Arrays.asList(new NumericType[]{
        SMALLINT,
        INTEGER,
        BIGINT,
        DECIMAL,
      }));


  public static final String getCastFunction(Type t) {
    return "HELP_FUNCTIONS.convert_" + t.toString().replace(' ', '_');
  }

  public static final Type getCastFunctionSuperType(Type t) {
    return t.getCastFunctionSuperTypeM();
  }

  public abstract boolean hasLengthM();

  public abstract int getLengthM();

  public static final boolean hasLength(Type t) {
    return t.hasLengthM();
  }

  public static final int getLength(Type t) {
    return t.getLengthM();
  }

  public abstract Type getCastFunctionSuperTypeM();

  public static final Type getSUPERTYPE() {
    return SUPERTYPE;
  }

  public static final Type getSMALLINT() {
    return SMALLINT;
  }

  public static final Type getINTEGER() {
    return INTEGER;
  }

  public static final Type getTypeclass(Type t) {
    return t.getTypeclass();
  }

  public abstract Type getTypeclass();

  public static final Type getBIGINT() {
    return BIGINT;
  }

  public static final Type getBOOLEAN() {
    return BOOLEAN;
  }

  public static final Type getDECIMAL() {
    return DECIMAL;
  }

  public static final Type getCHARACTERSTRINGLITERAL() {
    return CHARACTERSTRINGLITERAL;
  }

  public static final Type getCHARACTER(int length) {
    return new CHARACTERType(length);
  }

  public static final Type getCHARACTERVARYING(int length) {
    return new CHARACTERVARYINGType(length);
  }

  public static final boolean isNumericType(Type t) {
    return t == getSUPERTYPE() || t instanceof NumericType;
  }

  public static final boolean isCharacterStringType(Type t) {
    return t == getSUPERTYPE() || t instanceof StringType;
  }

  public static final boolean isBooleanType(Type t) {
    return t == getSUPERTYPE() || t instanceof BOOLEANType;
  }

  public static final String getName(final Type type) {
    return type.type_name;
  }

  public String toString() {
    return getName(this);
  }

  public abstract boolean convertibleTo(Type to);

  public static final boolean isImplicitConvertible(final Type from, final Type to) {
    return from.convertibleTo(to);
  }

  // =======

  private final String type_name;

  protected Type(final String name) {
    this.type_name = name;
  }

}

class SMALLINTType extends NumericType {

  public SMALLINTType() {
    super("SMALLINT", "-32768", "32767");
  }

}

class INTEGERType extends NumericType {

  public INTEGERType() {
    super("INTEGER", "-2147483648", "2147483647");
  }

}

class BIGINTType extends NumericType {

  public BIGINTType() {
    super("BIGINT", "-9223372036854775808", "9223372036854775807");
  }

}

class DECIMALType extends NumericType {

  public DECIMALType() {
    super("DECIMAL",
        "-100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
        "+100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
        );
  }

}

abstract class StringType extends Type {

  public boolean convertibleTo(Type to) {
    if (to == Type.getSUPERTYPE()) return true;
    if (!(to instanceof StringType)) {
      return false;
    }

    return true;
  }

  protected StringType(final String name) {
    super(name);
  }

  public Type getTypeclass() {
    return Type.getCHARACTERSTRINGLITERAL();
  }

}

abstract class CharacterStringType extends StringType {

  protected int charstringlen;

  public Type getCastFunctionSuperTypeM() {
    return Type.getCHARACTERSTRINGLITERAL();
  }

  public boolean hasLengthM() {
    return true;
  }

  public int getLengthM() {
    return charstringlen;
  }

  @Override
  public boolean convertibleTo(Type to) {
    return super.convertibleTo(to);
  }

  public CharacterStringType(final String name) {
    this(name, -1);
  }

  public CharacterStringType(final String name, int len) {
    super(name);
    this.charstringlen = len;
  }

}

class CHARACTERType extends CharacterStringType {

  public CHARACTERType(int length) {
    super("CHARACTER", length);
  }

}

class CHARACTERVARYINGType extends CharacterStringType {

  public CHARACTERVARYINGType(int length) {
    super("CHARACTER VARYING", length);
  }

}

class CHARACTERSTRINGLITERALType extends CharacterStringType {

  public CHARACTERSTRINGLITERALType() {
    super("CHARACTERSTRINGLITERAL_NEVER_DISPLAYED!");
  }

}

class BOOLEANType extends Type {

  public BOOLEANType(){
    super("BOOLEAN");
  }

  public boolean hasLengthM() {
    return false;
  }

  public int getLengthM() {
    return -1;
  }

  public Type getCastFunctionSuperTypeM() {
    return Type.getBOOLEAN();
  }

  public Type getTypeclass() {
    return Type.getBOOLEAN();
  }

  public boolean convertibleTo(Type t) {
    if (t == Type.getSUPERTYPE()) return true;
    if (!(t instanceof BOOLEANType)){
      return false;
    }

    // must be refactored later to make casts possible

    return true;
  }

}

class SUPERTYPE extends Type {

  public SUPERTYPE() {
    super("SUPERTYPE_NEVER_DISPLAYED");
  }

  public boolean hasLengthM() {
    return false;
  }

  public int getLengthM() {
    return -1;
  }

  public Type getTypeclass() {
    assert false;
    return Type.getBOOLEAN();
  }

  public Type getCastFunctionSuperTypeM() {
    return Type.getSUPERTYPE();
  }

  public boolean convertibleTo(Type t) {
    return t instanceof SUPERTYPE;
  }

}
