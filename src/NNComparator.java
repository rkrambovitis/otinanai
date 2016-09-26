package gr.phaistosnetworks.admin.otinanai;

import java.util.*;

class NNComparator implements Comparator<Object> {

  public NNComparator(Map<String, String[]> map) {
    this.map = map;
  }

  public int compare(Object o1, Object o2) {
    Float v2 = Float.parseFloat(map.get(o2)[13]);
    Float v1 = Float.parseFloat(map.get(o1)[13]);
    int op = v1.compareTo(v2);
    if (op == 0) 
      return 1;
    return (op);
  }

  private Map<String, String[]> map;
}
