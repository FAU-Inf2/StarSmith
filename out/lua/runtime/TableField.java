package runtime;

public abstract class TableField {

  public static final class TableFieldMember extends TableField {

    public final String name;
    public final Type type;

    public TableFieldMember(final String name, final Type type) {
      this.name = name;
      this.type = type;
    }

  }

  public static final class TableFieldArray extends TableField {

    public final Type type;

    public TableFieldArray(final Type type) {
      this.type = type;
    }

  }

  public static final TableFieldMember tableFieldMember(final String name, final Type type) {
    return new TableFieldMember(name, type);
  }

  public static final TableFieldArray tableFieldArray(final Type type) {
    return new TableFieldArray(type);
  }

}
