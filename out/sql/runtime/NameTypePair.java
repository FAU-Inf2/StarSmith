package runtime;

public class NameTypePair {

  public static NameTypePair getNew(String n, Type t) {
    return new NameTypePair(n, t);
  }

  public static Type getType(NameTypePair ntp) {
    return ntp.type;
  }

  public static String getName(NameTypePair ntp) {
    return ntp.name;
  }

  private String name;
  private Type type;

  private NameTypePair(String n, Type t) {
    this.name = n;
    this.type = t;
  }

}
