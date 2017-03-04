package gr.phaistosnetworks.admin.otinanai;

import java.util.logging.*;
import java.util.*;
import redis.clients.jedis.*;

class RedisTracker implements KeyWordTracker {

  public RedisTracker(String key, int as, float atl, float ath, int acs, JedisPool jp, Logger l) {
    mean = 0f;
    variance = 0d;
    stdev = 0d;
    alarmSamples = as;
    alarmEnabled = true;
    lowAlarmThreshold = atl;
    highAlarmThreshold = ath;
    alarmConsecutiveSamples = acs;
    lowAlarmCount = 0;
    highAlarmCount = 0;
    zeroesCount = 0;
    keyWord = new String(key);
    logger = l;
    sampleCount = 1;
    currentFloat = 0f;
    currentDataCount = 0;
    lastTimeStamp = 0l;
    curTS = 0l;
    zeroPct = 0f;
    recordType = OtiNanai.UNSET;
    step1Key = keyWord + "thirtySec";
    step2Key = keyWord + "fiveMin";
    step3Key = keyWord + "thirtyMin";
    jediTemple = jp;
    alarmDisabled = keyWord+"alarmDisabled";
    alarmKey = keyWord + "alarmTS";

    try ( Jedis jedis = jediTemple.getResource() ) {
      if (jedis.exists(alarmDisabled)) {
        alarmEnabled = false;
      }

      String alarmSaved = jedis.get(alarmKey);
      if (alarmSaved == null) {
        alarm = 0L;
      } else {
        alarm = Long.parseLong(alarmSaved);
      }
      logger.finest("[RedisTracker]: new RedisTracker initialized for \"" +keyWord+"\"");
    } catch (Exception e) {
      logger.severe("[RedisTracker]: "+e.getCause());
    }
  }

  public String getKeyWord() {
    return keyWord;
  }

  public void delete() {
    try ( Jedis jedis = jediTemple.getResource() ) {
      jedis.del(step1Key);
      jedis.del(step2Key);
      jedis.del(step3Key);
      jedis.del(alarmKey);
      jedis.del(alarmDisabled);
    } catch (Exception e) {
      logger.severe("[RedisTracker]: "+e.getCause());
    }
  }

  public void putFreq() {
    if (recordType == OtiNanai.UNSET) {
      recordType = OtiNanai.FREQ;
      currentCount = 0;
    }
    currentCount ++;
  }

  public void putCounter(long value) {
    if (recordType == OtiNanai.UNSET) {
      recordType = OtiNanai.COUNTER;
      currentLong = 0l;
      currentPrev = 0l;
    }
    currentLong = value;
    curTS = System.currentTimeMillis();
    logger.finest("[RedisTracker]: currentLong is now " +currentLong);
  }

  public void putGauge(float value) {
    if (recordType == OtiNanai.UNSET) {
      recordType = OtiNanai.GAUGE;
      currentFloat = 0f;
      currentDataCount = 0;
    }
    currentFloat += value;
    currentDataCount ++;
    curTS = System.currentTimeMillis();
    logger.finest("[RedisTracker]: currentFloat is now " +currentFloat);
  }

  public void putSum(float value) {
    if (recordType == OtiNanai.UNSET) {
      recordType = OtiNanai.SUM;
      currentFloat = 0f;
    }
    currentFloat += value;
  }

  public void putHist(float value) {
    if (recordType == OtiNanai.UNSET) {
      recordType = OtiNanai.HISTOGRAM;
      histValues = new ArrayList<Float>();
    }
    currentMin = value;
    currentMax = value;
    histValues.add(value);
  }


  public void tick() {
    logger.fine("[RedisTracker]: ticking " + keyWord );
    if (recordType == OtiNanai.COUNTER || recordType == OtiNanai.GAUGE)
      flush(curTS);
    flush(System.currentTimeMillis());
  }

  private void flush(long ts) {
    if (recordType == OtiNanai.UNSET)
      return;
    if (recordType == OtiNanai.COUNTER && currentLong == 0l)
      return;
    if (recordType == OtiNanai.GAUGE && currentDataCount == 0)
      return;
    if (recordType == OtiNanai.SUM && currentFloat == 0f)
      return;

    if (lastTimeStamp == 0l && recordType != OtiNanai.GAUGE) {
      lastTimeStamp = ts;
      currentFloat = 0f;
      currentDataCount = 0;
      currentCount = 0;
      currentPrev = currentLong;
      currentLong = 0l;
      return;
    }

    float perSec = 0f;
    float timeDiff = (float)(ts - lastTimeStamp);
    lastTimeStamp = ts;
    try ( Jedis jedis = jediTemple.getResource() ) {

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
      logger.finest("[RedisTracker]: lpush "+step1Key+" "+toPush);
      jedis.lpush(step1Key, toPush);



      /*
       * Aggregation
       */
      float lastMerge = 0f;
      long tsMerge = 0l;
      long lastts = 0l;
      String lastDato = new String();
      String lastDatoString = new String();
      List<String> newestData;

      /*
       * Aggregate last X samples and expire the 1 latest sample when limit exceeded
       * This will allow us to choose to show the aggregated data stream only
       * when we exceed the limit of the shortest.
       * Otherwise, graphs have too many data points, and the change from non aggregated
       * to aggregated makes it look like crap.
       */
      if ((jedis.llen(step1Key) % OtiNanai.STEP1_SAMPLES_TO_MERGE) == 0) {
        newestData = jedis.lrange(step1Key, 0, OtiNanai.STEP1_SAMPLES_TO_MERGE-1);
        for (String dato : newestData) {
          lastts = Long.parseLong(dato.substring(0,dato.indexOf(" ")));
          lastDato = dato.substring(dato.indexOf(" ")+1).replaceAll(",",".");
          logger.finest("[RedisTracker]: Data: "+lastMerge+" += "+lastDato+" ts: "+tsMerge+" += "+lastts);
          lastMerge += Float.parseFloat(lastDato);
          tsMerge += lastts;
          if (jedis.llen(step1Key) >= OtiNanai.STEP1_MAX_SAMPLES)
            jedis.rpop(step1Key);
        }

        float finalSum = lastMerge/OtiNanai.STEP1_SAMPLES_TO_MERGE;
        long finalts = tsMerge/OtiNanai.STEP1_SAMPLES_TO_MERGE;

        logger.fine("[RedisTracker]: "+keyWord+": First Aggregated dataSum:"+ lastMerge + " / "+OtiNanai.STEP1_SAMPLES_TO_MERGE+" = "+finalSum+". tsSum: "+tsMerge+" / "+OtiNanai.STEP1_SAMPLES_TO_MERGE+" = "+ finalts);

        toPush = new String(finalts+" "+String.format("%.3f", finalSum));
        jedis.lpush(step2Key, toPush);

        /*
         * Same as above, but for step2, i.e. second aggregation,
         * or further aggregation of already aggregated values,
         * so aggregating aggregation aggro aggreviate aggrevous
         * oh aggravation, how aggravating you are.
         */

        if ((jedis.llen(step2Key) % OtiNanai.STEP2_SAMPLES_TO_MERGE) == 0) {
          lastMerge = 0;
          tsMerge = 0;
          newestData = jedis.lrange(step2Key, 0, OtiNanai.STEP2_SAMPLES_TO_MERGE-1);
          for (String dato : newestData) {
            lastts = Long.parseLong(dato.substring(0,dato.indexOf(" ")));
            lastDato = dato.substring(dato.indexOf(" ")+1).replaceAll(",",".");
            logger.finest("[RedisTracker]: Data: "+lastMerge+" += "+lastDato+" ts: "+tsMerge+" += "+lastts);
            lastMerge += Float.parseFloat(lastDato);
            tsMerge += lastts;
            if (jedis.llen(step2Key) >= OtiNanai.STEP2_MAX_SAMPLES)
              jedis.rpop(step2Key);
          }

          finalSum = lastMerge/OtiNanai.STEP2_SAMPLES_TO_MERGE;
          finalts = tsMerge/OtiNanai.STEP2_SAMPLES_TO_MERGE;

          logger.fine("[RedisTracker]: "+keyWord+": Second Aggregated dataSum:"+ lastMerge + " / "+OtiNanai.STEP2_SAMPLES_TO_MERGE+" = "+finalSum+". tsSum: "+tsMerge+" / "+OtiNanai.STEP2_SAMPLES_TO_MERGE+" = "+ finalts);

          toPush = new String(finalts+" "+String.format("%.3f", finalSum));
          jedis.lpush(step3Key, toPush);

        }
      }


      /*
       * Alarm detection
       */

      if (alarmEnabled) {
        if (mean == 0f && perSec != 0f) {
          logger.fine("[RedisTracker]: mean is 0, setting new value");
          mean = perSec;
          sampleCount = 1;
          return;
        }

        mean += (perSec-mean)/alarmSamples;
        variance += ((perSec-mean)*(perSec-mean))/alarmSamples;
        stdev = Math.sqrt(variance);

        if ( sampleCount < alarmSamples ) {
          sampleCount++;
          if (perSec == 0f) {
            zeroesCount ++;
            zeroPct = 100.0f * ((float)zeroesCount / (float)sampleCount);
          }
        } else {
          if (perSec < (mean - 3*stdev )) {
            //if ((Math.abs(perSec) < (Math.abs(mean) / lowAlarmThreshold)) && (perSec != 0f || zeroPct < 2.0f)) {
            lowAlarmCount++;
            highAlarmCount = 0;
          } else if (perSec > (mean + 3*stdev)) {
            //} else if (Math.abs(perSec) > (Math.abs(mean) * highAlarmThreshold)) {
            highAlarmCount++;
          lowAlarmCount = 0;
          } else {
            highAlarmCount = 0;
            lowAlarmCount = 0;
          }

          if (lowAlarmCount >= alarmConsecutiveSamples || highAlarmCount >= alarmConsecutiveSamples ) {
            if ( alarm == 0 || (ts - alarm > OtiNanai.ALARMLIFE) ) {
              logger.info("[RedisTracker]: Error conditions met for " + keyWord + " mean: "+mean +" value: "+perSec+" zeroPct: "+zeroPct+" zeroesCount: "+zeroesCount+" sampleCount: "+sampleCount+" highCount: "+highAlarmCount+" lowCount: "+lowAlarmCount);
              OtiNanaiNotifier onn = new OtiNanaiNotifier((highAlarmCount >= alarmConsecutiveSamples ? "High" : "Low" )
                  + " " + keyWord
                  + " " + OtiNanai.WEBURL+"/"+keyWord
                  + " " + String.format("%.2f", perSec)
                  + " " + String.format("%.3f", mean));
              onn.send();
              alarm=ts;
              jedis.set(alarmKey, Long.toString(ts));
            }
          }
          }
        }
      } catch (Exception e) {
        logger.severe("[RedisTracker]: "+e.getCause());
      }
    }

    public long getAlarm() {
      if (!alarmEnabled)
        return 0L;
      return alarm;
    }

    public ArrayList<String> getMemory(Long startTime, Long offset) {
      ArrayList<String> returner = new ArrayList<String>();
      try ( Jedis jedis = jediTemple.getResource() ) {
        long startTimeAgo = (System.currentTimeMillis() - startTime + offset);
        //System.out.println("kw: "+keyWord +"startTime: "+startTime+" offset: "+offset+" startTimeAgo:" +startTimeAgo+" step1: "+OtiNanai.STEP1_MILLISECONDS+" step2: "+OtiNanai.STEP2_MILLISECONDS);
        if (startTimeAgo <= OtiNanai.STEP1_MILLISECONDS || jedis.llen(step2Key) < 2 ) {
          returner.addAll(jedis.lrange(step1Key,0,-1));
        } else if (startTimeAgo <= OtiNanai.STEP2_MILLISECONDS || jedis.llen(step3Key) < 2 ) {
          returner.addAll(jedis.lrange(step2Key,0,-1));
        } else {
          returner.addAll(jedis.lrange(step3Key,0, -1));
        }
      } catch (Exception e) {
        logger.severe("[RedisTracker]: "+e.getCause());
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

    public boolean alarmEnabled() {
      return alarmEnabled;
    }

    public void alarmEnabled(boolean onOrOff) {
      try ( Jedis jedis = jediTemple.getResource() ) {
        alarmEnabled = onOrOff;
        if (alarmEnabled && (onOrOff == false)) {
          jedis.set(alarmDisabled, "true");
        } else if (!alarmEnabled && (onOrOff == true)) {
          jedis.del(alarmDisabled);
        }
      } catch (Exception e) {
        logger.severe("[RedisTracker]: "+e.getCause());
      }
    }

    private String keyWord;
    private long currentCount;
    private float mean;
    private double variance;
    private double stdev;
    private int sampleCount;
    private int currentDataCount;
    private float currentFloat;
    private float currentMin;
    private float currentMax;
    private ArrayList<Float> histValues;
    private float zeroPct;
    private long currentLong;
    private long currentPrev;
    private long lastTimeStamp;
    private long curTS;
    private Logger logger;
    private long alarm;
    private boolean alarmEnabled;
    private int alarmSamples;
    private float lowAlarmThreshold;
    private float highAlarmThreshold;
    private int alarmConsecutiveSamples;
    private int highAlarmCount;
    private int lowAlarmCount;
    private short recordType;
    private int zeroesCount;
    private JedisPool jediTemple;
    private String redisHost;
    private String step1Key;
    private String step2Key;
    private String step3Key;
    private String alarmKey;
    private String alarmDisabled;
  }
