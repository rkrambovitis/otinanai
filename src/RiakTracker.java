package gr.phaistosnetworks.admin.otinanai;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.*;

import java.util.logging.*;
import java.util.*;

class RiakTracker implements KeyWordTracker {
	public RiakTracker(String key, int as, float at, short rt, Logger l, Bucket bucket) {
		mean = 0f;
		thirtySecCount = 0;
		fiveMinCount = 0;
		thirtyMinCount = 0;
      alarmSamples = as;
      alarmThreshold = at;
		keyWord = new String(key);
      thirtySecKey = keyWord + "thirtySec";
      fiveMinKey = keyWord + "fiveMin";
      thirtyMinKey = keyWord + "thirtyMin";
      riakBucket = bucket;
      logger = l;
		sampleCount = 1;
      thirtySecDataCount = -1;
      thirtySecFloat = 0f;
      thirtySecLong = 0l;
      thirtySecPrev = 0l;
      recordType = rt;
		alarm = 0L;
      logger.finest("[RiakTracker]: new RiakTracker initialized for \"" +keyWord+"\"");
	}

	public String getKeyWord() {
		return keyWord;
	}

	public void put() {
      thirtySecCount ++;
      logger.finest("[RiakTracker]: thirtySecCount is now " +thirtySecCount);
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
      logger.finest("[RiakTracker]: thirtySecFloat is now " +thirtySecFloat);
   }

   public void tick(long ts) {
      logger.fine("[RiakTracker]: ticking " + keyWord );
      try {
         flush(ts);
      } catch (RiakRetryFailedException rrfe) {
         logger.severe("[RiakTracker]: tick: "+rrfe);
      }
   }

	private void flush(long ts) throws RiakRetryFailedException{
      float perSec = 0f;
      /*
       * thirtySecDataCount is set to -1 by default, which means that the tracker tracks the amount of events.
       * In the event it's a "metric", it will be 1 or more.
       * Inthe event it's a metric but has no new data, it's 0.
       */
      //LLString thirtySecMemory = riakBucket.fetch(thirtySecKey, LLString.class).execute();
      //LLString fiveMinMemory = riakBucket.fetch(fiveMinKey, LLString.class).execute();
      //LLString thirtyMinMemory = riakBucket.fetch(thirtyMinKey, LLString.class).execute();

      LLString thirtySecMemory = new LLString();
      LLString fiveMinMemory = new LLString();
      LLString thirtyMinMemory = new LLString();

      logger.fine("[RiakTracker]: fetching existing " + thirtySecKey);
      thirtySecMemory = riakBucket.fetch(thirtySecKey, LLString.class).execute();
      if (thirtySecMemory == null ) {
         logger.fine("[RiakTracker]: null. Creating new " + thirtySecKey);
         thirtySecMemory = new LLString();
      }
     
      if (thirtySecDataCount > 0 ) {
         logger.fine("[RiakTracker]: thirtySecFloat = " +thirtySecFloat);
         perSec = (thirtySecFloat / thirtySecDataCount);
         thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));

      } else if (thirtySecLong > 0) {
         if (thirtySecLong != thirtySecPrev) {
            logger.fine("[RiakTracker]: thirtySecLong = " + thirtySecLong);
            if (thirtySecPrev == 0l || thirtySecPrev > thirtySecLong) {
               logger.fine("Last count is 0 or decrementing. Setting and Skipping");
            } else {
               long timeDiff = ts - lastTimeStamp;
               perSec = ((float)(thirtySecLong - thirtySecPrev)*1000/timeDiff);
               thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
            }
            thirtySecPrev = thirtySecLong;
            lastTimeStamp = ts;
         }
      } else if (thirtySecDataCount < 0 ) {
         logger.fine("[RiakTracker]: thirtySecCount = " +thirtySecCount);
         perSec = ((float)thirtySecCount / 30);
         logger.fine("[RiakTracker]: perSec = " +perSec);
         thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
      }

      if (thirtySecMemory.size() > 2) {
         logger.fine("[RiakTracker]: thirtySecMemory.size() = "+thirtySecMemory.size());
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
      if (thirtySecMemory.size() >= OtiNanai.THIRTY_SEC_SAMPLES) {
         lastMerge = 0;

         logger.fine("[RiakTracker]: fetching existing " + fiveMinKey);
         fiveMinMemory = riakBucket.fetch(fiveMinKey, LLString.class).execute();
         if (fiveMinMemory == null) {
            fiveMinMemory = new LLString();
         }


         for (int i=1; i<=OtiNanai.THIRTY_S_TO_FIVE_M ; i++) {
            lastDatoString=thirtySecMemory.get(OtiNanai.THIRTY_SEC_SAMPLES - i);
            lastts=lastDatoString.substring(0,lastDatoString.indexOf(" "));
            lastDato=lastDatoString.substring(lastDatoString.indexOf(" ")+1);
            logger.fine("[RiakTracker]: Aggregating: "+lastMerge+" += "+lastDato);
            lastMerge += Float.parseFloat(lastDato);
            thirtySecMemory.remove(OtiNanai.THIRTY_SEC_SAMPLES -i);
         }
         float finalSum = lastMerge/OtiNanai.THIRTY_S_TO_FIVE_M;
         logger.fine("[RiakTracker]: Aggregated to : "+ lastMerge + "/"+OtiNanai.THIRTY_S_TO_FIVE_M+" = "+finalSum);
         fiveMinMemory.push(new String(lastts+" "+String.format("%.2f", finalSum)));
      }
      riakBucket.store(thirtySecKey, thirtySecMemory).execute();

      /*
       * Aggregate old 5min samples and make 30min samples
       */
      if (fiveMinMemory.size() >= OtiNanai.FIVE_MIN_SAMPLES) {
         lastMerge = 0;

         logger.fine("[RiakTracker]: fetching existing " + thirtyMinKey);
         thirtyMinMemory = riakBucket.fetch(thirtyMinKey, LLString.class).execute();
         if (thirtyMinMemory == null) {
            thirtyMinMemory = new LLString();
         }

         for (int i=1; i<=OtiNanai.FIVE_M_TO_THIRTY_M ; i++) {
            lastDatoString=fiveMinMemory.get(OtiNanai.FIVE_MIN_SAMPLES - i);
            lastts=lastDatoString.substring(0,lastDatoString.indexOf(" "));
            lastDato=lastDatoString.substring(lastDatoString.indexOf(" ")+1);
            lastMerge += Long.parseLong(lastDato);
            fiveMinMemory.remove(OtiNanai.FIVE_MIN_SAMPLES -i);
         }
         thirtyMinMemory.push(new String(lastts+" "+Math.round(lastMerge/OtiNanai.FIVE_M_TO_THIRTY_M)));
         riakBucket.store(fiveMinKey, fiveMinMemory).execute();
         riakBucket.store(thirtyMinKey, thirtyMinMemory).execute();
      }



      /*
       * Alarm detection
       */
      if (sampleCount < alarmSamples)
         sampleCount++;

      if (mean == 0f && perSec != 0f) {
         logger.fine("[RiakTracker]: mean is 0, setting new value");
         mean = perSec;
         sampleCount = 1;
      } else if (perSec != 0f) {
         logger.fine("[RiakTracker]: Calculating new mean");
         float deviation = (perSec-mean)/mean;
         mean += (perSec-mean)/alarmSamples;
         logger.fine("[RiakTracker]: d: "+deviation+" m: "+mean);

         if ((sampleCount >= alarmSamples) && (deviation >= alarmThreshold)) {
            logger.info("[RiakTracker]: Error conditions met for " + keyWord);
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

   public LLString getMemory() {
      LLString returner = new LLString();
      try {
         LLString thirtySecMemory = riakBucket.fetch(thirtySecKey, LLString.class).execute();
         returner.addAll(thirtySecMemory);
      } catch (Exception e) {
         logger.severe("[RiakTracker]: getMemory(1): "+e);
      }

      try {
         LLString fiveMinMemory = riakBucket.fetch(fiveMinKey, LLString.class).execute();
         returner.addAll(fiveMinMemory);
      } catch (Exception e) {
         logger.severe("[RiakTracker]: getMemory(2): "+e);
      }
      
      try {
         LLString thirtyMinMemory = riakBucket.fetch(thirtyMinKey, LLString.class).execute();
         returner.addAll(thirtyMinMemory);
      } catch (Exception e) {
         logger.severe("[RiakTracker]: getMemory(3): "+e);
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
   private String thirtySecKey;
   private String fiveMinKey;
   private String thirtyMinKey;
   private Logger logger;
   private int alarmSamples;
   private float alarmThreshold;
   private Bucket riakBucket;
   private short recordType;
}

