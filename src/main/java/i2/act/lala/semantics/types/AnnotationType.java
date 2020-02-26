package i2.act.lala.semantics.types;

import i2.act.errors.RPGException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnnotationType implements Type {

  private final List<Type> argumentTypes;

  public AnnotationType(final Type... argumentTypes) {
    this.argumentTypes = new ArrayList<Type>(argumentTypes.length);

    for (int index = 0; index < argumentTypes.length; ++index) {
      final Type argumentType = argumentTypes[index];

      // optional/list types are only allowed as last argument
      if (index < argumentTypes.length - 1) {
        if (argumentType instanceof OptionalType) {
          throw new RPGException("optional types are only allowed at the end");
        } else if (argumentType instanceof ListType) {
          throw new RPGException("list types are only allowed at the end");
        }
      }

      this.argumentTypes.add(argumentType);
    }
  }

  public final List<Type> getArgumentTypes() {
    return Collections.unmodifiableList(this.argumentTypes);
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof AnnotationType)) {
      return false;
    }

    final AnnotationType otherAnnotationType = (AnnotationType) other;

    if (this.argumentTypes.size() != otherAnnotationType.argumentTypes.size()) {
      return false;
    }

    final int numberOfArgumentTypes = this.argumentTypes.size();
    for (int index = 0; index < numberOfArgumentTypes; ++index) {
      if (!this.argumentTypes.get(index).equals(otherAnnotationType.argumentTypes.get(index))) {
        return false;
      }
    }

    return true;
  }

  public final Type getArgumentType(final int index) {
    if (index < 0) {
      throw new RPGException("invalid index: " + index);
    }

    if (this.argumentTypes.isEmpty()) {
      return null;
    }

    // only the last argument type can be a list/optional type -> other cases are easy
    if (index < this.argumentTypes.size() - 1) {
      return this.argumentTypes.get(index);
    }

    assert (index >= this.argumentTypes.size() - 1);

    Type lastArgumentType = this.argumentTypes.get(this.argumentTypes.size() - 1);

    // 'unpack' optional type
    if (lastArgumentType instanceof OptionalType) {
      lastArgumentType = ((OptionalType) lastArgumentType).getType();
    }

    if (lastArgumentType instanceof ListType) {
      // list type -> arbitrarily many elements allowed
      return ((ListType) lastArgumentType).getElementType();
    } else {
      // not a list type -> only one element allowed
      if (index == this.argumentTypes.size() - 1) {
        return lastArgumentType;
      } else {
        return null;
      }
    }
  }

  public final boolean canBeAssignedTo(final AnnotationType other) {
    final int numberOfArgumentTypes = this.argumentTypes.size();
    for (int index = 0; index < numberOfArgumentTypes; ++index) {
      final Type thisType = this.argumentTypes.get(index);
      final Type otherType = other.getArgumentType(index);

      if (otherType == null) {
        return false;
      }

      if (!thisType.equals(otherType)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public final String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("@(");

    boolean first = true;
    for (final Type argumentType : this.argumentTypes) {
      if (first) {
        first = false;
      } else {
        builder.append(", ");
      }
      builder.append(argumentType);
    }

    builder.append(")");

    return builder.toString();
  }

}
