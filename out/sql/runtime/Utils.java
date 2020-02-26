package runtime;

public class Utils {

  public static String printReplicatedStringToLength(String s, int len) {
    return new String(new char[(len / s.length()) + 1]).replace("\0", s).substring(0, len);
  }

}
