package gr.phaistosnetworks.admin.otinanai;

import java.util.logging.*;
import java.util.*;

import com.basho.riak.client.bucket.*;

class OtiNanaiMemory {
   public OtiNanaiMemory(String key, long al, int alarmSamples, float alarmThreshold, Logger l, short rT, float theGauge, long theCounter, short storageType, Bucket bucket) {
      keyWord = key;
      recType = rT;
      logger = l;
      alarm = 0L;
      if (storageType == OtiNanai.MEM) {
         kwt = new MemTracker(key, alarmSamples, alarmThreshold, rT, logger);
      } else if (storageType == OtiNanai.RIAK) {
         kwt = new RiakTracker(key, alarmSamples, alarmThreshold, rT, logger, bucket);
      }
      if (rT == OtiNanai.GAUGE) {
         logger.fine("[Memory]: GAUGE detected");
         put(theGauge);
      }else if (rT == OtiNanai.COUNTER) {
         logger.fine("[Memory]: COUNTER detected");
         put(theCounter);
      } else if (rT == OtiNanai.FREQ) {
         logger.fine("[Memory]: FREQ detected");
         put();
      }
   }

   public void put() {
      kwt.put();
   }

   public void delete() {
      kwt.delete();
   }

   public void put(float value) {
      kwt.put(value);
   }

   public void put(long value) {
      kwt.put(value);
   }

   public void tick(long ts) {
      kwt.tick(ts);
   }

   public String getKeyWord() {
      return keyWord;
   }

   public boolean getAlarm(long ts) {
      if ((alarm != 0L) && ((ts - alarm) < alarmLife))
         return true;

      alarm = kwt.getAlarm();

      if ((alarm != 0L) && ((ts - alarm) < alarmLife))
         return true;
      return false;
   }

   public LinkedList<String> getMemory() {
      return kwt.getMemory();
   }

   private String defKey;
   private long alarm;
   private long alarmLife;
   private String keyWord;
   private Logger logger;
   private KeyWordTracker kwt;
   private short recType;
}
