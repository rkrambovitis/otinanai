import java.util.logging.*;
import java.util.*;

class KeyWordTracker {
	public KeyWordTracker(String key, Logger l) {
		fiveMinCount = 0;
		fiveMinMean = 0;
		thirtyMinCount = 0;
		thirtyMinMean = 0;
      fiveMinMemory = new LinkedList<String> ();
		keyWord = new String(key);
		fiveMinFlush = System.currentTimeMillis();
		thirtyMinFlush = fiveMinFlush;
      logger = l;
		sampleCount = 1;
		deviation = 0;
		alarm = false;
      logger.finest("[KeyWordTracker]: new KeyWordTracker initialized for \"" +keyWord+"\"");
	}

	public String getKeyWord() {
		return keyWord;
	}

	public void put(long ts) {
		fiveMinCount ++;
      logger.finest("[KeyWordTracker]: fiveMinCount is now " +fiveMinCount);
	}

   public void tick(long ts) {
      logger.fine("[KeyWordTracker]: ticking " + keyWord );
      fiveMinFlush=ts;
      flush(ts);
   }

	private void flush(long ts) {
      logger.finest("[KeyWordTracker]: Adding " +fiveMinCount+ " to thirtyMinCount");
		thirtyMinCount += fiveMinCount;
      logger.finest("[KeyWordTracker]: Adding from front of stack");
      fiveMinMemory.push(new String(ts+" "+fiveMinCount));

      if (fiveMinMemory.size() >= FIVE_MINS_DAY) {
         logger.finest("[KeyWordTracker]: Removing from end of stack");
         fiveMinMemory.removeLast();
      }

		if (fiveMinMean == 0 ) {
         logger.finest("[KeyWordTracker]: fiveMinMean is 0, setting new value");
			fiveMinMean = fiveMinCount;
		} else {
         logger.finest("[KeyWordTracker]: Calculating new fiveMinMean");
			deviation = (fiveMinCount - fiveMinMean)/fiveMinMean;
			fiveMinMean += (fiveMinCount - fiveMinMean)/sampleCount;
		}
		sampleCount++;
		fiveMinCount = 0;
		if ((sampleCount > MINSAMPLES) && (deviation >= ERROR_DEVIATION)) {
         logger.info("[KeyWordTracker]: Error conditions met for " + keyWord);
			alarm=true;
      }
      if (sampleCount >= FIVE_MINS_DAY ) {
			sampleCount = 1;
		}
	}

	public boolean getAlarm() {
		return alarm;
	}

   public LinkedList<String> getFiveMinMemory() {
      return fiveMinMemory;
   }

   public long getFiveMinCount() {
      return fiveMinCount;
   }
   public long getThirtyMinCount() {
      return thirtyMinCount;
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
   private LinkedList<String> fiveMinMemory;
   private LinkedList<Long> thirtyMinMemory;
   private Logger logger;

	private static final int COUNT = 0;
	private static final int METRIC = 1;
	private static final int MINSAMPLES = 2;
	private static final int ERROR_DEVIATION = 2;
	private static final int FIVE_MIN = 30000;
	private static final int FIVE_MINS_DAY = 288;
}
