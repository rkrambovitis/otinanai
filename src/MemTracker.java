package gr.phaistosnetworks.admin.otinanai;

import java.util.logging.*;
import java.util.*;

class MemTracker implements KeyWordTracker {
	public MemTracker(String key, int ps, int as, float at, short rt, Logger l) {
		mean = 0f;
		thirtySecCount = 0;
		fiveMinCount = 0;
		thirtyMinCount = 0;
      previewSamples = ps;
      alarmSamples = as;
      alarmThreshold = at;
      thirtySecMemory = new LinkedList<String> ();
      previewMemory = new LinkedList<String> ();
      fiveMinMemory = new LinkedList<String> ();
      thirtyMinMemory = new LinkedList<String> ();
		keyWord = new String(key);
      logger = l;
		sampleCount = 1;
      thirtySecDataCount = -1;
      thirtySecFloat = 0f;
      thirtySecLong = 0l;
      thirtySecPrev = 0l;
      recordType = rt;
		alarm = 0L;
      logger.finest("[MemTracker]: new MemTracker initialized for \"" +keyWord+"\"");
	}

	public String getKeyWord() {
		return keyWord;
	}

	public void put() {
      thirtySecCount ++;
      logger.finest("[MemTracker]: thirtySecCount is now " +thirtySecCount);
   }

	public void put(long value) {
      thirtySecLong = value;
      logger.finest("[MemTracker]: thirtySecLong is now " +thirtySecCount);
   }

   public void put(float value) {
      thirtySecFloat += value;
      thirtySecDataCount ++;
      if (thirtySecDataCount == 0)
         thirtySecDataCount++;
      logger.finest("[MemTracker]: thirtySecFloat is now " +thirtySecFloat);
   }

   public void tick(long ts) {
      logger.fine("[MemTracker]: ticking " + keyWord );
      flush(ts);
   }

	private void flush(long ts) {
      float perSec = 0f;
      /*
       * thirtySecDataCount is set to -1 by default, which means that the tracker tracks the amount of events.
       * thirtySecLong is the "COUNT" counter. If more than 0, then we are a "COUNT" type.
       * In the event it's a "metric", it will be 1 or more.
       * Inthe event it's a metric but has no new data, it's 0.
       */
      if (thirtySecDataCount > 0 ) {
         logger.fine("[MemTracker]: thirtySecFloat = " +thirtySecFloat);
         perSec = (thirtySecFloat / thirtySecDataCount);
         thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
         previewMemory.push(new String(ts+" "+String.format("%.2f", perSec)));

      } else if (thirtySecLong > 0) {
         if (thirtySecLong != thirtySecPrev) {
            logger.fine("[MemTracker]: thirtySecLong = " + thirtySecLong);
            if (thirtySecPrev == 0l || thirtySecPrev > thirtySecLong) {
               logger.fine("Last count is 0 or decrementing. Setting and Skipping");
            } else {
               long timeDiff = ts - lastTimeStamp;
               perSec = ((float)(thirtySecLong - thirtySecPrev)*1000/timeDiff);
               thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
               previewMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
            }
            thirtySecPrev = thirtySecLong;
            lastTimeStamp = ts;
         }
      } else if (thirtySecDataCount < 0 ) {
         logger.fine("[MemTracker]: thirtySecCount = " +thirtySecCount);
         perSec = ((float)thirtySecCount / 30);
         logger.fine("[MemTracker]: perSec = " +perSec);
         thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
         previewMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
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
               previewMemory.remove(1);
            }
         }
      }

      logger.fine("[MemTracker]: size of previewSamples for "+keyWord+" : "+previewMemory.size()+" (limit "+previewSamples+")");
      if (previewMemory.size() > previewSamples) {
         logger.fine("[MemTracker]: previewSamples limit exceeded. Trimming last value");
         previewMemory.remove(previewSamples);
      }
      

      float lastMerge;
      String lastDatoString = new String();
      String lastts = new String();
      String lastDato = new String();

      /*
       * Aggregate old 30sec samples and make 5min samples
       */
      if (thirtySecMemory.size() >= OtiNanai.THIRTY_SEC_SAMPLES) {
         lastMerge = 0;
         for (int i=1; i<=OtiNanai.THIRTY_S_TO_FIVE_M ; i++) {
            lastDatoString=thirtySecMemory.get(OtiNanai.THIRTY_SEC_SAMPLES - i);
            lastts=lastDatoString.substring(0,lastDatoString.indexOf(" "));
            lastDato=lastDatoString.substring(lastDatoString.indexOf(" ")+1);
            logger.fine("[MemTracker]: Aggregating: "+lastMerge+" += "+lastDato);
            lastMerge += Float.parseFloat(lastDato);
            thirtySecMemory.remove(OtiNanai.THIRTY_SEC_SAMPLES -i);
         }
         float finalSum = lastMerge/OtiNanai.THIRTY_S_TO_FIVE_M;
         logger.fine("[MemTracker]: Aggregated to : "+ lastMerge + "/"+OtiNanai.THIRTY_S_TO_FIVE_M+" = "+finalSum);
         fiveMinMemory.push(new String(lastts+" "+String.format("%.2f", finalSum)));
      }

      /*
       * Aggregate old 5min samples and make 30min samples
       */
      if (fiveMinMemory.size() >= OtiNanai.FIVE_MIN_SAMPLES) {
         lastMerge = 0;
         for (int i=1; i<=OtiNanai.FIVE_M_TO_THIRTY_M ; i++) {
            lastDatoString=fiveMinMemory.get(OtiNanai.FIVE_MIN_SAMPLES - i);
            lastts=lastDatoString.substring(0,lastDatoString.indexOf(" "));
            lastDato=lastDatoString.substring(lastDatoString.indexOf(" ")+1);
            lastMerge += Long.parseLong(lastDato);
            fiveMinMemory.remove(OtiNanai.FIVE_MIN_SAMPLES -i);
         }
         thirtyMinMemory.push(new String(lastts+" "+Math.round(lastMerge/OtiNanai.FIVE_M_TO_THIRTY_M)));
      }



      /*
       * Alarm detection
       */
      //if ( (sampleCount < alarmSamples) && (thirtySecDataCount != 0) )
      if ( sampleCount < alarmSamples)
         sampleCount++;

      if (mean == 0f && perSec != 0f) {
         logger.fine("[MemTracker]: mean is 0, setting new value");
         mean = perSec;
         /*
         logger.fine("[MemTracker]: thirtySecDataCount: "+thirtySecDataCount);
         if (thirtySecDataCount > 0 ) {
            logger.fine("[MemTracker]: GAUGE");
            mean = thirtySecFloat;
            logger.fine("[MemTracker]: m: "+mean);
         } else if (thirtySecPrev > 0 ) {
            logger.gine("[MemTracker]: COUNTER");
         } else if (thirtySecDataCount < 0 ) {
            logger.fine("[MemTracker]: FREQ");
            mean = thirtySecCount;
            logger.fine("[MemTracker]: m: "+mean);
         }
         */
         sampleCount = 1;
      } else if (perSec != 0f) {
         logger.fine("[MemTracker]: Calculating new mean");
         float deviation = (perSec-mean)/mean;
         mean += (perSec-mean)/alarmSamples;
         logger.fine("[MemTracker]: d: "+deviation+" m: "+mean);

         /*
         logger.fine("[MemTracker]: thirtySecDataCount: "+thirtySecDataCount);
         float deviation = 0f;
         if (thirtySecDataCount > 0 ) {
            logger.fine("[MemTracker]: GAUGE");
            deviation = (thirtySecFloat - mean)/mean;
            mean += (thirtySecFloat - mean)/alarmSamples;
            logger.fine("[MemTracker]: d: "+deviation+" - m: "+mean);
         } else if (thirtySecDataCount < 0 ) {
            logger.fine("[MemTracker]: FREQ");
            deviation = (thirtySecCount - mean)/mean;
            mean += (thirtySecCount - mean)/alarmSamples;
            logger.fine("[MemTracker]: d: "+deviation+" - m: "+mean);
         }
         */

         if ((sampleCount >= alarmSamples) && (deviation >= alarmThreshold)) {
            logger.info("[MemTracker]: Error conditions met for " + keyWord);
            alarm=ts;
         }
      }

      thirtySecCount = 0;
      thirtySecFloat = 0f;
      if (thirtySecDataCount > 0)
         thirtySecDataCount = 0;
	}

	public long getAlarm() {
		return alarm;
	}

   public LinkedList<String> getPreview() {
      return previewMemory;
   }

   public LinkedList<String> getMemory() {
      LinkedList<String> returner = new LinkedList<String>();
      try {
         returner.addAll(thirtySecMemory);
         returner.addAll(fiveMinMemory);
         returner.addAll(thirtyMinMemory);
      } catch (NullPointerException e) {
         logger.severe("[MemTracker] :"+e.getStackTrace());
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
   private long thirtySecLong;
   private long thirtySecPrev;
   private long lastTimeStamp;
   private LinkedList<String> thirtySecMemory;
   private LinkedList<String> previewMemory;
   private LinkedList<String> fiveMinMemory;
   private LinkedList<String> thirtyMinMemory;
   private Logger logger;
   private int previewSamples;
   private int alarmSamples;
   private float alarmThreshold;
   private short recordType;
}
