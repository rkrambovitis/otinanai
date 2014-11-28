package gr.phaistosnetworks.admin.otinanai;

import java.util.logging.*;
import java.util.*;
import redis.clients.jedis.*;

class RedisTracker implements KeyWordTracker {

	public RedisTracker(String key, int as, float at, int acs, Logger l) {
		mean = 0f;
      alarmSamples = as;
      alarmThreshold = at;
      alarmConsecutiveSamples = acs;
      alarmCount = 0;
		keyWord = new String(key);
      logger = l;
      jedis = new Jedis("localhost");
		sampleCount = 1;
      currentFloat = 0f;
      currentDataCount = -1;
      recordType = OtiNanai.UNSET;
		alarm = 0L;
      logger.finest("[RedisTracker]: new RedisTracker initialized for \"" +keyWord+"\"");
	}

	public String getKeyWord() {
		return keyWord;
   }

   public void delete() {
      jedis.del(keyWord + "thirtySec");
      jedis.del(keyWord + "fiveMin");
      jedis.del(keyWord + "thirtyMin");
      jedis.quit();
   }

   public void put() {
      if (recordType == OtiNanai.UNSET) {
         recordType = OtiNanai.FREQ;
         currentCount = 0;
      }
      if (recordType == OtiNanai.FREQ) {
         currentCount ++;
         logger.finest("[RedisTracker]: currentCount is now " +currentCount);
      } else {          
         logger.info("[RedisTracker]: Ignoring put of wrong type ("+keyWord+" is FREQ)");
      }
   }

   public void put(long value) {
      if (recordType == OtiNanai.UNSET) {
         recordType = OtiNanai.COUNTER;
         currentLong = 0l;
         currentPrev = 0l;
      }
      if (recordType == OtiNanai.COUNTER) {
         currentLong = value;
         if (currentDataCount == 0)
            currentDataCount++;
         logger.finest("[RedisTracker]: currentLong is now " +currentLong);
      } else {
         logger.info("[RedisTracker]: Ignoring put of wrong type ("+keyWord+" is COUNTER)");
      }  
   }

   public void put(float value) {
      if (recordType == OtiNanai.UNSET) {
         recordType = OtiNanai.GAUGE;
         currentFloat = 0f;
         currentDataCount = 0;
      }
      if (recordType == OtiNanai.SUM ) {
         currentFloat += value;
      } else if (recordType == OtiNanai.GAUGE) {
         currentFloat += value;
         currentDataCount ++;
         logger.finest("[RedisTracker]: currentFloat is now " +currentFloat);
      } else {
         logger.info("[RedisTracker]: Ignoring put of wrong type ("+keyWord+" is GAUGE)");
      }  
   }
   

   public void tick() {
      long ts = System.currentTimeMillis();
      logger.fine("[RedisTracker]: ticking " + keyWord );
      flush(ts);
   }

	private void flush(long ts) {
      if (recordType == OtiNanai.UNSET)
         return;
      if (recordType == OtiNanai.COUNTER && currentLong == 0l)
         return;
      if (recordType == OtiNanai.GAUGE && currentDataCount == 0)
         return;

      float perSec = 0f;
      float timeDiff = (float)(ts - lastTimeStamp);

      String step1Key = keyWord + "thirtySec";
      String step2Key = keyWord + "fiveMin";
      String step3Key = keyWord + "thirtyMin";

      lastTimeStamp = ts;
      if (recordType == OtiNanai.GAUGE) {
         logger.fine("[RedisTracker]: currentFloat = " +currentFloat);
         perSec = (currentFloat / currentDataCount);
         currentFloat = 0f;
         currentDataCount = 0;
      } else if (recordType == OtiNanai.SUM) {
         perSec = ((float)((1000f*currentFloat)/timeDiff));
         currentFloat = 0f;
      } else if (recordType == OtiNanai.COUNTER) {
         if (currentLong != currentPrev) {
            logger.fine("[RedisTracker]: currentLong = " + currentLong);
            if (currentPrev == 0l || currentPrev > currentLong) {
               logger.fine("Last count is 0 or decrementing. Setting and Skipping");
            } else {
               float valueDiff = (float)(currentLong - currentPrev);
               perSec = ((float)((valueDiff*1000f)/timeDiff));
            }
            currentPrev = currentLong;
            currentLong = 0l;
         }

      } else if (recordType == OtiNanai.FREQ ) {
         perSec = ((float)currentCount*1000f)/timeDiff;
         logger.finest("[RedisTracker]: perSec = " +perSec);
         currentCount = 0;
      }

      logger.fine("[RedisTracker]: "+keyWord+" timeDiff: " +timeDiff+ " perSec: "+perSec);
      String toPush = new String(ts+" "+String.format("%.3f", perSec));
//      jedis.lpush(step1Key, new String(ts+" "+String.format("%.2f", perSec)));

      try {
         if (jedis.llen(step1Key) > 1) {
            //ugly deduplication
            String dato1 = jedis.lindex(step1Key,0);
            String dato2 = jedis.lindex(step1Key,1);
            String dato0 = toPush.substring(toPush.indexOf(" ") +1);
            dato1 = dato1.substring(dato1.indexOf(" ") +1);
            dato2 = dato2.substring(dato2.indexOf(" ") +1);
            if (dato0.equals(dato1)) {
               if (dato1.equals(dato2)) {
                  jedis.lpop(step1Key);
               }
            }
         }
      } catch (Exception e) {
         logger.severe("[RedisTracker]: tick(): "+e);
         logger.severe("toPush: "+toPush);
         System.err.println("[RedisTracker]: tick(): "+e.getMessage());
      }
      logger.finest("[RedisTracker]: lpush "+step1Key+" "+toPush);
      jedis.lpush(step1Key, toPush);


      
      /*
       * Aggregate old 30sec samples and make 5min samples
       */

      float lastMerge;
      long lastts;
      long tsMerge;
      String lastDato = new String();
      String lastDatoString = new String();

      if (jedis.llen(step1Key) >= OtiNanai.STEP1_MAX_SAMPLES) {
         lastMerge = 0;
         lastts = 0l;
         tsMerge = 0l;

         for (int i=1; i<=OtiNanai.STEP1_SAMPLES_TO_MERGE ; i++) {
            lastDatoString=jedis.rpop(step1Key);
            lastts = Long.parseLong(lastDatoString.substring(0,lastDatoString.indexOf(" ")));
            lastDato=lastDatoString.substring(lastDatoString.indexOf(" ")+1);

            logger.fine("[RedisTracker]: Data: "+lastMerge+" += "+lastDato+" ts: "+tsMerge+" += "+lastts);
            lastMerge += Float.parseFloat(lastDato);
            tsMerge += lastts;
         }
         float finalSum = lastMerge/OtiNanai.STEP1_SAMPLES_TO_MERGE;
         long finalts = tsMerge/OtiNanai.STEP1_SAMPLES_TO_MERGE;

         logger.fine("[RedisTracker]: "+keyWord+": Aggregated dataSum:"+ lastMerge + " / "+OtiNanai.STEP1_SAMPLES_TO_MERGE+" = "+finalSum+". tsSum: "+tsMerge+" / "+OtiNanai.STEP1_SAMPLES_TO_MERGE+" = "+ finalts);

         toPush = new String(finalts+" "+String.format("%.3f", finalSum));
         jedis.lpush(step2Key, toPush);
      }


      /*
       * Aggregate old 5min samples and make 30min samples
       */

      if (jedis.llen(step2Key) >= OtiNanai.STEP2_MAX_SAMPLES) {
         lastMerge = 0;
         lastts = 0l;
         tsMerge = 0;

         for (int i=1; i<=OtiNanai.STEP2_SAMPLES_TO_MERGE ; i++) {
            lastDatoString = jedis.rpop(step2Key);
            lastts = Long.parseLong(lastDatoString.substring(0,lastDatoString.indexOf(" ")));
            lastDato = lastDatoString.substring(lastDatoString.indexOf(" ")+1);
            lastMerge += Float.parseFloat(lastDato);
            tsMerge += lastts;
         }
         float finalSum = lastMerge/OtiNanai.STEP2_SAMPLES_TO_MERGE;
         long finalts = tsMerge/OtiNanai.STEP2_SAMPLES_TO_MERGE;

         logger.fine("[RedisTracker]: "+keyWord+": Aggregated dataSum:"+ lastMerge + " / "+OtiNanai.STEP2_SAMPLES_TO_MERGE+" = "+finalSum+". tsSum: "+tsMerge+" / "+OtiNanai.STEP2_SAMPLES_TO_MERGE+" = "+ finalts);
         toPush = new String(finalts+" "+String.format("%.3f", finalSum));
         jedis.lpush(step3Key, toPush);
      }



      /*
       * Alarm detection
       */
      if ( sampleCount < alarmSamples)
         sampleCount++;
      if (mean == 0f && perSec != 0f) {
         logger.fine("[RedisTracker]: mean is 0, setting new value");
         mean = perSec;
         sampleCount = 1;
      } else if (perSec != 0f) {
         logger.fine("[RedisTracker]: Calculating new mean");
         float deviation = (perSec-mean)/mean;
         mean += (perSec-mean)/alarmSamples;
         logger.fine("[RedisTracker]: d: "+deviation+" m: "+mean);

         if ((sampleCount >= alarmSamples) && (deviation >= alarmThreshold)) {
            alarmCount++;
            if (alarmCount >= alarmConsecutiveSamples) {
               logger.info("[RedisTracker]: Error conditions met for " + keyWord + " mean: "+mean +" deviation: "+deviation+" consecutive: "+alarmCount);
               alarm=ts;
            } else {
               logger.info("[RedisTracker]: Error threshold breached " + keyWord + " mean: "+mean +" deviation: "+deviation+" consecutive: "+alarmCount);
            }
         } else {
            alarmCount = 0;
         }
      }

      step1Key = null;
      step2Key = null;
      step3Key = null;
	}

	public long getAlarm() {
		return alarm;
	}

   public LinkedList<String> getMemory() {
      LinkedList<String> returner = new LinkedList<String>();
      try {
         returner.addAll(jedis.lrange(keyWord+"thirtySec",0,-1));
         returner.addAll(jedis.lrange(keyWord+"fiveMin",0,-1));
         returner.addAll(jedis.lrange(keyWord+"thirtyMin",0,-1));
      } catch (Exception e) {
         logger.severe("[RedisTracker]: getMemory(): " + e);
         System.err.println("[RedisTracker]: getMemory(): " +e.getMessage());
      }
      return returner;
   }

   public long getCurrentCount() {
      return currentCount;
   }

   public short getType() {
      return recordType;
   }

   public void setType(short type) {
      if (recordType == OtiNanai.UNSET)
         recordType = type;
   }

	private long alarm;
	private String keyWord;
	private long currentCount;
	private float mean;
	private int sampleCount;
   private int currentDataCount;
   private float currentFloat;
   private long currentLong;
   private long currentPrev;
   private long lastTimeStamp;
   private Logger logger;
   private int alarmSamples;
   private float alarmThreshold;
   private short recordType;
   private int alarmConsecutiveSamples;
   private int alarmCount;
   private Jedis jedis;
}
