package gr.phaistosnetworks.admin.otinanai;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.*;

import java.util.logging.*;
import java.util.*;

class RiakTracker implements KeyWordTracker {
	public RiakTracker(String key, int ps, int as, float at, Logger l, Bucket bucket) {
		mean = 0f;
		thirtySecCount = 0;
		fiveMinCount = 0;
		thirtyMinCount = 0;
      previewSamples = ps;
      alarmSamples = as;
      alarmThreshold = at;
		keyWord = new String(key);
      thirtySecKey = keyWord + "thirtySec";
      fiveMinKey = keyWord + "fiveMin";
      thirtyMinKey = keyWord + "thirtyMin";
      previewKey = keyWord + "preview";
      riakBucket = bucket;
      logger = l;
		sampleCount = 1;
      thirtySecDataCount = -1;
      thirtySecFloat = 0f;
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

   public void put(float value) {
      thirtySecFloat += value;
      thirtySecDataCount ++;
      if (thirtySecDataCount == 0)
         thirtySecDataCount++;
      logger.finest("[RiakTracker]: thirtySecFloat is now " +thirtySecFloat);
   }

   public void put(long value) {
      logger.severe("[RiakTracker]: Counters Not Implemented yet in Riak");
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

      float perSec;
      /*
       * thirtySecDataCount is set to -1 by default, which means that the tracker tracks the amount of events.
       * In the event it's a "metric", it will be 1 or more.
       * Inthe event it's a metric but has no new data, it's 0.
       */
      //LLString previewMemory = riakBucket.fetch(previewKey, LLString.class).execute();
      //LLString thirtySecMemory = riakBucket.fetch(thirtySecKey, LLString.class).execute();
      //LLString fiveMinMemory = riakBucket.fetch(fiveMinKey, LLString.class).execute();
      //LLString thirtyMinMemory = riakBucket.fetch(thirtyMinKey, LLString.class).execute();

      LLString previewMemory = new LLString();
      LLString thirtySecMemory = new LLString();
      LLString fiveMinMemory = new LLString();
      LLString thirtyMinMemory = new LLString();

      logger.fine("[RiakTracker]: fetching existing " + previewKey);
      previewMemory = riakBucket.fetch(previewKey, LLString.class).execute();
      if (previewMemory == null) {
         logger.fine("[RiakTracker]: null. Creating new " + previewKey);
         previewMemory = new LLString();
      }

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
         previewMemory.push(new String(ts+" "+String.format("%.2f", perSec)));

      } else if (thirtySecDataCount < 0 ) {
         logger.fine("[RiakTracker]: thirtySecCount = " +thirtySecCount);
         perSec = ((float)thirtySecCount / 30);
         logger.fine("[RiakTracker]: perSec = " +perSec);
         thirtySecMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
         previewMemory.push(new String(ts+" "+String.format("%.2f", perSec)));
      }

      if (thirtySecMemory.size() > 2) {
         logger.fine("[RiakTracker]: thirtySecMemory.size() = "+thirtySecMemory.size());
         logger.fine("[RiakTracker]: previewMemory.size() = "+previewMemory.size());
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
               if (previewMemory.size() > 2) {
                  previewMemory.remove(1);
               }
            }
         }
      }


      logger.fine("[RiakTracker]: size of previewSamples for "+keyWord+" : "+previewMemory.size()+" (limit "+previewSamples+")");
      if (previewMemory.size() > previewSamples) {
         logger.fine("[RiakTracker]: previewSamples limit exceeded. Trimming last value");
         previewMemory.remove(previewSamples);
      }

      riakBucket.store(previewKey, previewMemory).execute();

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
      if ( (sampleCount < alarmSamples) && (thirtySecDataCount != 0) )
         sampleCount++;

      if (mean == 0f ) {
         logger.fine("[RiakTracker]: mean is 0, setting new value");
         logger.fine("[RiakTracker]: thirtySecDataCount: "+thirtySecDataCount);
         if (thirtySecDataCount > 0 ) {
            logger.fine("[RiakTracker]: GAUGE");
            mean = thirtySecFloat;
            logger.fine("[RiakTracker]: m: "+mean);
         } else if (thirtySecDataCount < 0 ) {
            logger.fine("[RiakTracker]: FREQ");
            mean = thirtySecCount;
            logger.fine("[RiakTracker]: m: "+mean);
         }
         sampleCount = 1;
      } else {
         logger.fine("[RiakTracker]: Calculating new mean");
         logger.fine("[RiakTracker]: thirtySecDataCount: "+thirtySecDataCount);
         float deviation = 0f;
         if (thirtySecDataCount > 0 ) {
            logger.fine("[RiakTracker]: GAUGE");
            deviation = (thirtySecFloat - mean)/mean;
            mean += (thirtySecFloat - mean)/alarmSamples;
            logger.fine("[RiakTracker]: d: "+deviation+" - m: "+mean);
         } else if (thirtySecDataCount < 0 ) {
            logger.fine("[RiakTracker]: FREQ");
            deviation = (thirtySecCount - mean)/mean;
            mean += (thirtySecCount - mean)/alarmSamples;
            logger.fine("[RiakTracker]: d: "+deviation+" - m: "+mean);
         }

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

   //public LinkedList<String> getPreview() {
   public LLString getPreview() {
      try {
         return riakBucket.fetch(previewKey, LLString.class).execute();
      } catch (RiakRetryFailedException rrfe) {
         logger.severe("[RiakTracker]: getPreview: "+rrfe);
      }
      return null;
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
   private String thirtySecKey;
   private String fiveMinKey;
   private String thirtyMinKey;
   private String previewKey;
   private Logger logger;
   private int previewSamples;
   private int alarmSamples;
   private float alarmThreshold;
   private Bucket riakBucket;
}

