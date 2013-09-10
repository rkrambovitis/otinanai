import java.util.logging.*;
import java.util.*;

class KeyWordTracker {
	public KeyWordTracker(String key, Logger l) {
		mean = 0f;
		thirtySecCount = 0;
		fiveMinCount = 0;
		thirtyMinCount = 0;
      thirtySecMemory = new LinkedList<String> ();
      fiveMinMemory = new LinkedList<String> ();
      thirtyMinMemory = new LinkedList<String> ();
		keyWord = new String(key);
      logger = l;
		sampleCount = 1;
      thirtySecDataCount = -1;
      thirtySecFloat = 0f;
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

   public void put(float value) {
      thirtySecFloat += value;
      thirtySecDataCount ++;
      if (thirtySecDataCount == 0)
         thirtySecDataCount++;
      logger.finest("[KeyWordTracker]: thirtySecFloat is now " +thirtySecFloat);
   }

   public void tick(long ts) {
      logger.fine("[KeyWordTracker]: ticking " + keyWord );
      flush(ts);
   }

	private void flush(long ts) {
      float perSec;
      /*
       * thirtySecDataCount is set to -1 by default, which means that the tracker tracks the amount of events.
       * In the event it's a "metric", it will be 1 or more.
       * Inthe event it's a metric but has no new data, it's 0.
       */
      if (thirtySecDataCount > 0 ) {
         logger.fine("[KeyWordTracker]: thirtySecFloat = " +thirtySecFloat);
         perSec = (thirtySecFloat / thirtySecDataCount);
         thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));

      } else if (thirtySecDataCount < 0 ) {
         logger.fine("[KeyWordTracker]: thirtySecCount = " +thirtySecCount);
         perSec = ((float)thirtySecCount / 30);
         logger.fine("[KeyWordTracker]: perSec = " +perSec);
         thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
      }

      if (thirtySecMemory.size() > 2) {
         //ugly deduplication
         String dato0 = thirtySecMemory.get(0);
         String dato1 = thirtySecMemory.get(1);
         String dato2 = thirtySecMemory.get(2);
         dato0 = dato0.substring(dato0.indexOf(" ") +1);
         dato1 = dato1.substring(dato1.indexOf(" ") +1);
         dato2 = dato2.substring(dato2.indexOf(" ") +1);
         if (dato0.equals(dato1)) {
            if (dato1.equals(dato2)) {
               thirtySecMemory.remove(1);
            }
         }
      }
      

      float lastMerge;
      String lastDatoString = new String();
      String lastts = new String();
      String lastDato = new String();

      /*
       * Aggregate old 30sec samples and make 5min samples
       */
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

      /*
       * Aggregate old 5min samples and make 30min samples
       */
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



      /*
       * Alarm detection
       */
      if ( (sampleCount < MEANSAMPLES) && (thirtySecDataCount != 0) )
         sampleCount++;

      if (mean == 0f ) {
         logger.fine("[KeyWordTracker]: mean is 0, setting new value");
         logger.fine("[KeyWordTracker]: thirtySecDataCount: "+thirtySecDataCount);
         if (thirtySecDataCount > 0 ) {
            logger.fine("[KeyWordTracker]: GAUGE");
            mean = thirtySecFloat;
            logger.fine("[KeyWordTracker]: m: "+mean);
         } else if (thirtySecDataCount < 0 ) {
            logger.fine("[KeyWordTracker]: FREQ");
            mean = thirtySecCount;
            logger.fine("[KeyWordTracker]: m: "+mean);
         }
         sampleCount = 1;
      } else {
         logger.fine("[KeyWordTracker]: Calculating new mean");
         logger.fine("[KeyWordTracker]: thirtySecDataCount: "+thirtySecDataCount);
         float deviation = 0f;
         if (thirtySecDataCount > 0 ) {
            logger.fine("[KeyWordTracker]: GAUGE");
            deviation = (thirtySecFloat - mean)/mean;
            mean += (thirtySecFloat - mean)/MEANSAMPLES;
            logger.fine("[KeyWordTracker]: d: "+deviation+" - m: "+mean);
         } else if (thirtySecDataCount < 0 ) {
            logger.fine("[KeyWordTracker]: FREQ");
            deviation = (thirtySecCount - mean)/mean;
            mean += (thirtySecCount - mean)/MEANSAMPLES;
            logger.fine("[KeyWordTracker]: d: "+deviation+" - m: "+mean);
         }

         if ((sampleCount >= MEANSAMPLES) && (deviation >= ERROR_DEVIATION)) {
            logger.info("[KeyWordTracker]: Error conditions met for " + keyWord);
            alarm=ts;
         }
      }

      thirtySecCount = 0;
      thirtySecFloat = 0f;
      thirtySecDataCount = 0;
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
   private int thirtySecDataCount;
   private float thirtySecFloat;
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
