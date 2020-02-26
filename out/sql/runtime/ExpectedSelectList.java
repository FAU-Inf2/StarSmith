package runtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.stream.Collectors;

public class ExpectedSelectList extends ROL<NameTypePair> {

  public static ExpectedSelectList getEmpty() {
    return new ExpectedSelectList();
  }

  public ExpectedSelectList getNew() {
    return getEmpty();
  }

  public static ExpectedSelectList getNew(String name, Type t) {
    ExpectedSelectList esl = getEmpty();
    esl.list.add(NameTypePair.getNew(name, t));
    return esl;
  }

  public boolean equalsAccordingToList(NameTypePair nt1, NameTypePair t2) {
    assert false; // should never be called!
    return true;
  }

  public static List<String> getNames(ExpectedSelectList esl) {
    return esl.list.stream().map(ntp -> NameTypePair.getName(ntp)).collect(Collectors.toList());
  }

}
