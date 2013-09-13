package gr.phaistosnetworks.admin.otinanai;


import java.util.logging.*;
import java.util.*;

class OtiNanaiMemory {
   public OtiNanaiMemory(String key, long al, Logger l, short rT, int ps, float theValue) {
      keyWord = key;
      recType = rT;
      logger = l;
      alarmLife = al;
      previewSamples = ps;
      alarm = 0L;
      kwt = new KeyWordTracker(key, previewSamples, logger);
      if (rT == GAUGE) {
         put(theValue);
      } else if (rT == FREQ) {
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

   public Set<String> getAllHosts() {
      return null;
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
   private int previewSamples;
   private KeyWordTracker kwt;
   private short recType;
   private static final short GAUGE = 0;
   private static final short COUNTER = 1;
   private static final short FREQ = 2;

}
