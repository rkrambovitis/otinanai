import java.util.logging.*;

class KeyWordTracker {
	public KeyWordTracker(String key) {
		fiveMinCount = 0;
		fiveMinMean = 0;
		thirtyMinCount = 0;
		thirtyMinMean = 0;
		keyWord = new String(key);
		fiveMinFlush = System.currentTimeMillis();
		thirtyMinFlush = fiveMinFlush;
		sampleCount = 0;
		deviation = 0;
		alarm = false;
	}

	public String getKeyWord() {
		return keyWord;
	}

	public void put(long ts) {
		fiveMinCount ++;
		if ((ts - fiveMinFlush) > 30000 ) {
			fiveMinFlush=ts;
			flush();
		}
	}

	private void flush() {
		thirtyMinCount += fiveMinCount;
		if (fiveMinMean == 0 ) {
			fiveMinMean = fiveMinCount;
		} else {
			deviation = (fiveMinCount - fiveMinMean)/fiveMinMean;
			fiveMinMean += (fiveMinCount - fiveMinMean)/sampleCount;
		}
		sampleCount++;
		fiveMinCount = 0;
		if ((sampleCount > MINSAMPLES) && (deviation >= ERROR_DEVIATION)) {
			alarm=true;
		} else {
			alarm = false;
		}
		if (sampleCount >= 288 ) {
			sampleCount = 0;
		}
	}

	public boolean getAlarm() {
		return alarm;
	}

	private boolean alarm;
	private long fiveMinFlush;
	private long thirtyMinFlush;
	private String keyWord;
	private long fiveMinCount;
	private long thirtyMinCount;
	private float fiveMinMean;
	private long thirtyMinMean;
	private int sampleCount;
	private float deviation;

	private static final int COUNT = 0;
	private static final int METRIC = 1;
	private static final int MINSAMPLES = 2;
	private static final int ERROR_DEVIATION = 2;
	private static final int FIVE_MIN = 300000;
}
