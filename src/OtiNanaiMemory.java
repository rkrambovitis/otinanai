package gr.phaistosnetworks.admin.otinanai;

import java.util.logging.*;
import java.util.*;

import com.basho.riak.client.bucket.*;

class OtiNanaiMemory {
   public OtiNanaiMemory(String key, long al, int alarmSamples, float alarmThreshold, Logger l, short rT, int previewSamples, float theValue, short storageType, Bucket bucket) {
      keyWord = key;
      recType = rT;
      logger = l;
      alarm = 0L;
      if (storageType == OtiNanai.MEM) {
         kwt = new MemTracker(key, previewSamples, alarmSamples, alarmThreshold, logger);
      } else if (storageType == OtiNanai.RIAK) {
         kwt = new RiakTracker(key, previewSamples, alarmSamples, alarmThreshold, logger, bucket);
      }
      if (rT == OtiNanai.GAUGE) {
         put(theValue);
      } else if (rT == OtiNanai.FREQ) {
         put();
      }
   }

   public void put() {
      kwt.put();
   }

   public void put(float value) {
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

   public LinkedList<String> getPreview() {
      return kwt.getPreview();
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
