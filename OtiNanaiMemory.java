import java.util.logging.*;
import java.util.*;

class OtiNanaiMemory {
   public OtiNanaiMemory(String key, Logger l) {
      keyWord = key;
      logger = l;
      alarm = false;
      keyTrackerMap = new HashMap<String,KeyWordTracker> (20);
      KeyWordTracker defaultKWT = new KeyWordTracker(key, logger);
      defKey = new String("All_Hosts_Combined");
      keyTrackerMap.put(defKey, defaultKWT);
   }

   public void put(String host) {
      if (keyTrackerMap.containsKey(host)) {
         logger.finest("[Memory]: "+host+" -> "+keyWord+" tracker detected.");
         keyTrackerMap.get(host).put();
         keyTrackerMap.get(defKey).put();
      } else {
         logger.info("[Memory]: Creating new "+host+" -> "+keyWord+" tracker.");
         KeyWordTracker nkt = new KeyWordTracker(keyWord, logger);
         nkt.put();
         keyTrackerMap.get(defKey).put();
         keyTrackerMap.put(host, nkt);
      }
   }

   public void tick(long ts) {
      Set<String> allKeys = keyTrackerMap.keySet();
      for (String host : allKeys) {
         logger.fine("[Memory]: Flushing "+host);
         keyTrackerMap.get(host).tick(ts);
      }
   }

   public String getKeyWord() {
      return keyWord;
   }

   public boolean getAlarm() {
      if (alarm)
         return alarm;

      Set<String> allKeys = keyTrackerMap.keySet();
      for (String host : allKeys) {
         logger.fine("[Memory]: checking for alarms: "+host);
         if (keyTrackerMap.get(host).getAlarm())
            alarm = true;
      }
      return alarm;
   }

   public Set<String> getAllHosts() {
      return keyTrackerMap.keySet();
   }

   public void resetAlarm() {
      alarm = false;
   }

   public LinkedList<String> getMemory(String host) {
      if (keyTrackerMap.containsKey(host)) {
         return keyTrackerMap.get(host).getMemory();
      } else {
         return null;
      }
   }

   private String defKey;
   private boolean alarm;
   private String keyWord;
   private Logger logger;
   private HashMap<String,KeyWordTracker> keyTrackerMap;
}
