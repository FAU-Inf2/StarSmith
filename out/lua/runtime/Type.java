package runtime;

import runtime.TableField.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class Type {

  @Override
  public abstract boolean equals(final Object other);

  @Override
  public abstract String toString();


  // ------------------------------------------------------


  public static final class PrimitiveType extends Type {

    public final String name;

    public PrimitiveType(final String name) {
      this.name = name;
    }


    @Override
    public final boolean equals(final Object other) {
      return other == this;
    }

    @Override
    public final String toString() {
      return this.name;
    }

  }

  private static final Type ANY_INSTANCE = new PrimitiveType("<Any>");

  private static final PrimitiveType NIL_INSTANCE = new PrimitiveType("Nil");
  private static final PrimitiveType BOOLEAN_INSTANCE = new PrimitiveType("Boolean");
  private static final PrimitiveType NUMBER_INSTANCE = new PrimitiveType("Number");
  private static final PrimitiveType STRING_INSTANCE = new PrimitiveType("String");

  public static final Type Any() {
    return ANY_INSTANCE;
  }

  public static final PrimitiveType Nil() {
    return NIL_INSTANCE;
  }

  public static final PrimitiveType Boolean() {
    return BOOLEAN_INSTANCE;
  }

  public static final PrimitiveType Number() {
    return NUMBER_INSTANCE;
  }

  public static final PrimitiveType String() {
    return STRING_INSTANCE;
  }

  public static final boolean isPrimitiveType(final Type type) {
    return type instanceof PrimitiveType;
  }


  // ------------------------------------------------------


  public static final class TupleType extends Type {

    public final Tuple tuple;

    public TupleType(final Tuple tuple) {
      this.tuple = tuple;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof TupleType)) {
        return false;
      }

      final TupleType otherTupleType = (TupleType) other;

      if (Tuple.size(this.tuple) != Tuple.size(otherTupleType.tuple)) {
        return false;
      }

      final int tupleSize = Tuple.size(this.tuple);
      for (int idx = 0; idx < tupleSize; ++idx) {
        if (!this.tuple.elements.get(idx).equals(otherTupleType.tuple.elements.get(idx))) {
          return false;
        }
      }

      return true;
    }

    @Override
    public final String toString() {
      return "TupleType" + this.tuple.toString();
    }

  }

  public static final TupleType fromTuple(final Tuple tuple) {
    return new TupleType(tuple);
  }

  public static final Tuple toTuple(final Type type) {
    if (type instanceof TupleType) {
      return ((TupleType) type).tuple;
    } else {
      return Tuple.from(type);
    }
  }


  // ------------------------------------------------------


  public static final class TableType extends Type {

    public final Map<String, Type> members;
    public final Tuple arrayElements;

    public final Type expectedNamedMember;
    public final Type expectedArrayElement;

    public TableType(final Map<String, Type> members, final Tuple arrayElements,
        final Type expectedNamedMember, final Type expectedArrayElement) {
      this.members = members;
      this.arrayElements = arrayElements;
      this.expectedNamedMember = expectedNamedMember;
      this.expectedArrayElement = expectedArrayElement;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof TableType)) {
        return false;
      }

      final TableType otherTableType = (TableType) other;
      return this.members.equals(otherTableType.members)
          && this.arrayElements.equals(otherTableType.arrayElements)
          && Objects.equals(this.expectedNamedMember, otherTableType.expectedNamedMember)
          && Objects.equals(this.expectedArrayElement, otherTableType.expectedArrayElement);
    }

    @Override
    public final String toString() {
      final StringBuilder builder = new StringBuilder();

      builder.append("TableType(members: (");

      boolean first = true;
      for (final Map.Entry<String, Type> member : this.members.entrySet()) {
        if (first) {
          first = false;
        } else {
          builder.append(", ");
        }

        final String name = member.getKey();
        final Type type = member.getValue();

        builder.append(String.format("%s => %s", name, type));
      }

      builder.append("), arrayElements: ");
      builder.append(this.arrayElements.toString());
      builder.append(", expectedNamedMember: ");
      builder.append(this.expectedNamedMember);
      builder.append(", expectedArrayElement: ");
      builder.append(this.expectedArrayElement);
      builder.append(")");

      return builder.toString();
    }

    public final boolean canBeAssignedTo(final TableType targetType) {
      if (this == targetType) {
        return true;
      }

      // (1) check expectedNamedMember
      assert (this.expectedNamedMember == null);
      if (targetType.expectedNamedMember != null) {
        boolean foundMatchingMember = false;
        for (final Type type : this.members.values()) {
          if (assignable(type, targetType.expectedNamedMember)) {
            foundMatchingMember = true;
            break;
          }
        }

        if (!foundMatchingMember) {
          return false;
        }
      }

      // (2) check expectedArrayElement
      assert (this.expectedArrayElement == null);
      if (targetType.expectedArrayElement != null) {
        boolean foundMatchingElement = false;
        for (final Object element : this.arrayElements.elements) {
          final Type arrayElement = (Type) element;
          if (assignable(arrayElement, targetType.expectedArrayElement)) {
            foundMatchingElement = true;
            break;
          }
        }

        if (!foundMatchingElement) {
          return false;
        }
      }

      // (3) check named members
      for (final Map.Entry<String, Type> targetMember : targetType.members.entrySet()) {
        final String targetMemberName = targetMember.getKey();
        final Type targetMemberType = targetMember.getValue();

        if (!this.members.containsKey(targetMemberName)) {
          return false;
        }

        final Type sourceMemberType = this.members.get(targetMemberName);

        if (!assignable(sourceMemberType, targetMemberType)) {
          return false;
        }
      }

      // (4) check array elements
      if (this.arrayElements.size() < targetType.arrayElements.size()) {
        return false;
      }

      final int targetSize = targetType.arrayElements.size();
      for (int index = 0; index < targetSize; ++index) {
        final Type sourceArrayType = (Type) this.arrayElements.elements.get(index);
        final Type targetArrayType = (Type) targetType.arrayElements.elements.get(index);

        if (!assignable(sourceArrayType, targetArrayType)) {
          return false;
        }
      }

      return true;
    }

  }

  private static final TableType ANY_TABLE =
      new TableType(new LinkedHashMap<>(), Tuple.empty(), null, null);
  private static final TableType EMPTY_TABLE =
      new TableType(new LinkedHashMap<>(), Tuple.empty(), null, null);

  public static final TableType anyTableType() {
    return ANY_TABLE;
  }

  public static final TableType anyTableTypeWithMember(final Type memberType) {
    return new TableType(new LinkedHashMap<>(), Tuple.empty(), memberType, null);
  }

  public static final TableType anyTableTypeWithArrayElement(final Type arrayElementType) {
    return new TableType(new LinkedHashMap<>(), Tuple.empty(), null, arrayElementType);
  }

  public static final TableType emptyTableType() {
    return EMPTY_TABLE;
  }

  public static final boolean isTableType(final Type type) {
    return type instanceof TableType;
  }

  public static final int tableSize(final Type type) {
    return numberOfMembers(type) + numberOfArrayElements(type);
  }

  public static final boolean hasExpectedNamedMember(final Type type) {
    final TableType tableType = (TableType) type;
    return tableType.expectedNamedMember != null;
  }

  public static final boolean hasExpectedArrayElement(final Type type) {
    final TableType tableType = (TableType) type;
    return tableType.expectedArrayElement != null;
  }

  public static final int numberOfMembers(final Type type) {
    final TableType tableType = (TableType) type;

    final int concreteSize = tableType.members.size();

    if (hasExpectedNamedMember(type)) {
      return concreteSize + 1;
    } else {
      return concreteSize;
    }
  }

  public static final int numberOfArrayElements(final Type type) {
    final TableType tableType = (TableType) type;

    final int concreteSize = tableType.arrayElements.size();

    if (hasExpectedArrayElement(type)) {
      return concreteSize + 1;
    } else {
      return concreteSize;
    }
  }

  // removes the first array member from the given table type (if any)
  public static final TableType removeMember(final Type type) {
    final TableType tableType = (TableType) type;

    if (tableType.arrayElements.size() == 0) {
      return tableType;
    } else {
      return new TableType(tableType.members, Tuple.tail(tableType.arrayElements), null, null);
    }
  }

  // removes the named member from the given table type (if any)
  public static final TableType removeMember(final Type type, final String name) {
    final TableType tableType = (TableType) type;

    if (!tableType.members.containsKey(name)) {
      return tableType;
    } else {
      final Map<String, Type> newMembers = new LinkedHashMap<>(tableType.members);
      newMembers.remove(name);

      return new TableType(newMembers, tableType.arrayElements, null, null);
    }
  }

  public static final TableType removeExpectedNamedMember(final Type type) {
    final TableType tableType = (TableType) type;

    if (tableType.expectedNamedMember == null) {
      return tableType;
    } else {
      return new TableType(tableType.members, tableType.arrayElements,
          null, tableType.expectedArrayElement);
    }
  }

  public static final TableType removeExpectedArrayElement(final Type type) {
    final TableType tableType = (TableType) type;

    if (tableType.expectedArrayElement == null) {
      return tableType;
    } else {
      return new TableType(tableType.members, tableType.arrayElements,
          tableType.expectedNamedMember, null);
    }
  }

  public static final Type getFirstArrayElement(final Type type) {
    return (Type) Tuple.head(((TableType) type).arrayElements);
  }

  public static final boolean hasMember(final Type type, final String name) {
    return ((TableType) type).members.containsKey(name);
  }

  public static final Type typeOf(final Type type, final String name) {
    return ((TableType) type).members.get(name);
  }

  public static final Type typeOf(final Type type, final int index) {
    return (Type) ((TableType) type).arrayElements.elements.get(index - 1);
  }

  public static final Type expectedNamedMember(final Type type) {
    return ((TableType) type).expectedNamedMember;
  }

  public static final Type expectedArrayElement(final Type type) {
    return ((TableType) type).expectedArrayElement;
  }

  public static final TableType fromFieldTuple(final Tuple fieldTuple) {
    final Map<String, Type> members = new LinkedHashMap<>();
    final Tuple arrayElements = new Tuple();

    for (final Object field : fieldTuple.elements) {
      if (field instanceof TableFieldMember) {
        final TableFieldMember fieldMember = (TableFieldMember) field;
        members.put(fieldMember.name, fieldMember.type);
      } else {
        assert (field instanceof TableFieldArray);
        final TableFieldArray fieldArray = (TableFieldArray) field;
        arrayElements.elements.add(fieldArray.type);
      }
    }

    return new TableType(members, arrayElements, null, null);
  }

  public static final List<Symbol> getTableMembers(final Type _tableType, final Type expectedType) {
    //System.err.println("--[ getTableMembers() ]--");

    final List<Symbol> tableMembers = new ArrayList<>();

    final TableType tableType = (TableType) _tableType;
    for (final Map.Entry<String, Type> member : tableType.members.entrySet()) {
      final String name = member.getKey();
      final Type type = member.getValue();

      assert (!isFunctionType(type));

      if (expectedType == null || assignable(type, expectedType)) {
        tableMembers.add(new Symbol(name, type));
      }
    }

    //System.err.println(tableMembers);
    //System.err.println("-------------------------");

    //assert (!tableMembers.isEmpty());

    return tableMembers;
  }

  public static final List<Integer> getArrayElements(final Type _tableType, final Type expectedType) {
    final List<Integer> arrayElements = new ArrayList<>();

    final TableType tableType = (TableType) _tableType;

    int index = 1;
    for (final Object element : tableType.arrayElements.elements) {
      final Type elementType = (Type) element;

      assert (!isFunctionType(elementType));

      if (expectedType == null || assignable(elementType, expectedType)) {
        arrayElements.add(index);
      }

      ++index;
    }

    return arrayElements;
  }


  // ------------------------------------------------------


  public static final class FunctionType extends Type {

    public final TupleType parameterTypes;
    public final Type returnType;

    public FunctionType(final TupleType parameterTypes, final Type returnType) {
      this.parameterTypes = parameterTypes;
      this.returnType = returnType;
    }

    @Override
    public final boolean equals(final Object other) {
      if (!(other instanceof FunctionType)) {
        return false;
      }

      final FunctionType otherFunctionType = (FunctionType) other;
      return this.parameterTypes.equals(otherFunctionType.parameterTypes)
          && this.returnType.equals(otherFunctionType.returnType);
    }

    @Override
    public final String toString() {
      final StringBuilder builder = new StringBuilder();

      builder.append("FunctionType(");
      builder.append(this.parameterTypes);
      builder.append(" -> ");
      builder.append(this.returnType);
      builder.append(")");

      return builder.toString();
    }

  }

  public static final boolean isFunctionType(final Type type) {
    return (type instanceof FunctionType);
  }

  public static final FunctionType functionType(final TupleType parameterTypes,
      final Type returnType) {
    return new FunctionType(parameterTypes, returnType);
  }

  public static final FunctionType anyFunctionType() {
    return new FunctionType(null, null);
  }

  public static final FunctionType anyFunctionType(final Type returnType) {
    return new FunctionType(null, returnType);
  }

  public static final TupleType getParameterTypes(final Type functionType) {
    return ((FunctionType) functionType).parameterTypes;
  }

  public static final Type getReturnType(final Type functionType) {
    return ((FunctionType) functionType).returnType;
  }


  // ------------------------------------------------------


  public static final boolean is(final Type typeOne, final Type typeTwo) {
    return typeOne.equals(typeTwo);
  }

  public static final boolean assignable(final Type sourceType, final Type targetType) {
    return assignable(sourceType, targetType, false);
  }

  public static final boolean assignable(final Type sourceType, final Type targetType,
      final boolean strictTableTypes) {
    if (isFunctionType(sourceType)) {
      return false;
    }

    if (ANY_INSTANCE.equals(targetType)) {
      return true;
    }

    if (NIL_INSTANCE.equals(targetType)) {
      return true;
    }

    if (BOOLEAN_INSTANCE.equals(targetType)) {
      // everything can be used as a boolean value...
      return true;
    }

    if (isFunctionType(targetType) && getParameterTypes(targetType) == null) {
      // target type is "anyFunctionType"
      return isFunctionType(sourceType)
          && (getReturnType(targetType) == null
              || assignable(getReturnType(sourceType), getReturnType(targetType)));
    }

    if (isTableType(targetType) && isTableType(sourceType)) {
      if (strictTableTypes) {
        return sourceType.equals(targetType);
      } else {
        return ((TableType) sourceType).canBeAssignedTo((TableType) targetType);
      }
    }

    return sourceType.equals(targetType);
  }

}
