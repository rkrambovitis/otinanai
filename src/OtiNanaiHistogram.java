package gr.phaistosnetworks.admin.otinanai;

import java.util.ArrayList;

class OtiNanaiHistogram {
  public static String get(float min, float max, ArrayList<Float> values) {
    int ranges = (int)Math.round(3.33 * Math.log10(values.size())) + 1;
    float rangeStep = (max - min)/ranges;
    int[] rangeCounts = new int[ranges];
    float[] rangeBoundaries = new float[ranges];
    int i;

    for (i = 0; i < ranges ; i++) {
      rangeCounts[i] = 0;
      rangeBoundaries[i] = min + (i + 1) * rangeStep;
    }

    for (float f : values) {
      for (i = 0 ; i < ranges ; i++) {
        if (f <= rangeBoundaries[i]) {
          rangeCounts[i] = rangeCounts[i] + 1;
          break;
        }
      }
    }

    String op = new String("" + min + "-" + rangeBoundaries[0] + ":" + rangeCounts[0]);
    for (i = 1 ; i < ranges ; i++) {
      op += (", " + rangeBoundaries[i - 1] + "-" + rangeBoundaries[i] + ":" + rangeCounts[i]);
    }

    return op;
  }
}
