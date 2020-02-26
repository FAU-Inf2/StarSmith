package runtime;

import java.util.ArrayList;
import java.util.List;

public final class SymbTab {

  public final List<String> names;

  private SymbTab() {
    this.names = new ArrayList<String>();
  }

  private SymbTab(final List<String> names) {
    this.names = names;
  }

  // ---------------------------------------------------

  public static final SymbTab empty() {
    return new SymbTab();
  }

  public static final SymbTab put(final SymbTab before, final String name) {
    final SymbTab after = new SymbTab(new ArrayList<String>(before.names));
    after.names.add(name);
    return after;
  }

  public static final boolean contains(final SymbTab table, final String name) {
    return table.names.contains(name);
  }

  public static final List<String> defs(final SymbTab table) {
    return table.names;
  }

}
