package gr.phaistosnetworks.admin.otinanai;

import java.util.ArrayList;

class OtiNanaiHistogram {
  public static String get(float min, float max, ArrayList<Float> values) {
    int ranges = (int)Math.round(3.33 * Math.log10(values.size())) + 1;
    System.out.println("min: " + min + " Max: " + max + " total: " + values.size() + " Ranges: " + ranges);
    return "Hello World";
  }
}
