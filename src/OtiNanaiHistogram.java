package gr.phaistosnetworks.admin.otinanai;

import java.util.List;

class OtiNanaiHistogram {
  public static OtiNanaiProtos.Histogram get(float min, float max, List<Float> values, long ts) {
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

    OtiNanaiProtos.Histogram.Builder histogram = OtiNanaiProtos.Histogram.newBuilder();
    histogram.setTimestamp((int)(ts/1000))
        .setMinValue(min)
        .setRangeStep(rangeStep);

    for (i = 0; i < ranges ; i++) {
      histogram.addRangeCount(rangeCounts[i]);
    }
    return histogram.build();
  }
}
