import java.util.logging.*;
import java.util.*;

class KeyWordTracker {
	public KeyWordTracker(String key, Logger l) {
		mean = 0;
		thirtySecCount = 0;
		fiveMinCount = 0;
		thirtyMinCount = 0;
      thirtySecMemory = new LinkedList<String> ();
      fiveMinMemory = new LinkedList<String> ();
      thirtyMinMemory = new LinkedList<String> ();
		keyWord = new String(key);
      logger = l;
		sampleCount = 1;
		alarm = 0L;
      logger.finest("[KeyWordTracker]: new KeyWordTracker initialized for \"" +keyWord+"\"");
	}

	public String getKeyWord() {
		return keyWord;
	}

	public void put() {
		thirtySecCount ++;
      logger.finest("[KeyWordTracker]: thirtySecCount is now " +thirtySecCount);
	}

   public void tick(long ts) {
      logger.fine("[KeyWordTracker]: ticking " + keyWord );
      flush(ts);
   }

	private void flush(long ts) {
      logger.fine("[KeyWordTracker]: thirtySecCount = " +thirtySecCount);
      float perSec = ((float)thirtySecCount / 30);
      logger.fine("[KeyWordTracker]: perSec = " +perSec);
      thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));

      float lastMerge;
      String lastDatoString = new String();
      String lastts = new String();
      String lastDato = new String();

      if (thirtySecMemory.size() >= THIRTY_SEC_SAMPLES) {
         lastMerge = 0;
         for (int i=1; i<=THIRTY_S_TO_FIVE_M ; i++) {
            lastDatoString=thirtySecMemory.get(THIRTY_SEC_SAMPLES - i);
            lastts=lastDatoString.substring(0,lastDatoString.indexOf(" "));
            lastDato=lastDatoString.substring(lastDatoString.indexOf(" ")+1);
            logger.fine("[KeyWordTracker]: Aggregating: "+lastMerge+" += "+lastDato);
            lastMerge += Float.parseFloat(lastDato);
            thirtySecMemory.remove(THIRTY_SEC_SAMPLES -i);
         }
         float finalSum = lastMerge/THIRTY_S_TO_FIVE_M;
         logger.fine("[KeyWordTracker]: Aggregated to : "+ lastMerge + "/"+THIRTY_S_TO_FIVE_M+" = "+finalSum);
         fiveMinMemory.push(new String(lastts+" "+String.format("%.2f", finalSum)));
      }

      if (fiveMinMemory.size() >= FIVE_MIN_SAMPLES) {
         lastMerge = 0;
         for (int i=1; i<=FIVE_M_TO_THIRTY_M ; i++) {
            lastDatoString=fiveMinMemory.get(FIVE_MIN_SAMPLES - i);
            lastts=lastDatoString.substring(0,lastDatoString.indexOf(" "));
            lastDato=lastDatoString.substring(lastDatoString.indexOf(" ")+1);
            lastMerge += Long.parseLong(lastDato);
            fiveMinMemory.remove(FIVE_MIN_SAMPLES -i);
         }
         thirtyMinMemory.push(new String(lastts+" "+Math.round(lastMerge/FIVE_M_TO_THIRTY_M)));
      }

      

      if (sampleCount < MEANSAMPLES)
         sampleCount++;

		if (mean == 0 ) {
         logger.fine("[KeyWordTracker]: mean is 0, setting new value");
			mean = thirtySecCount;
         sampleCount = 1;
		} else {
         logger.fine("[KeyWordTracker]: Calculating new mean");
			float deviation = (thirtySecCount - mean)/mean;
			mean += (thirtySecCount - mean)/MEANSAMPLES;

         if ((sampleCount >= MEANSAMPLES) && (deviation >= ERROR_DEVIATION)) {
            logger.info("[KeyWordTracker]: Error conditions met for " + keyWord);
            alarm=ts;
         }
      }

      thirtySecCount = 0;
	}

	public long getAlarm() {
		return alarm;
	}

   public LinkedList<String> getMemory() {
      LinkedList<String> returner = new LinkedList<String>();
      try {
         returner.addAll(thirtySecMemory);
      } catch (NullPointerException e1) {
         logger.severe(e1.getMessage());
      }
      try {
         returner.addAll(fiveMinMemory);
      } catch (NullPointerException e2) {
         logger.severe(e2.getMessage());
      }
      try {
         returner.addAll(thirtyMinMemory);
      } catch (NullPointerException e3) {
         logger.severe(e3.getMessage());
      }
      return returner;
   }

   public long getThirtySecCount() {
      return thirtySecCount;
   }

   public long getFiveMinCount() {
      return fiveMinCount;
   }
   public long getThirtyMinCount() {
      return thirtyMinCount;
   }

	private long alarm;
	private String keyWord;
	private long thirtySecCount;
	private long fiveMinCount;
	private long thirtyMinCount;
	private float mean;
	private int sampleCount;
   private LinkedList<String> thirtySecMemory;
   private LinkedList<String> fiveMinMemory;
   private LinkedList<String> thirtyMinMemory;
   private Logger logger;

	private static final int COUNT = 0;
	private static final int METRIC = 1;
	private static final int MEANSAMPLES = 10;
	private static final int ERROR_DEVIATION = 2;

   private static final int THIRTY_SEC_SAMPLES = 300;
   private static final int THIRTY_S_TO_FIVE_M = 10;
	private static final int FIVE_MIN_SAMPLES = 2880;
   private static final int FIVE_M_TO_THIRTY_M = 6;
}
