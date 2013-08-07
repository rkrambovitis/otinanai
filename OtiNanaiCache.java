import java.io.*;
import java.util.*;
import java.util.logging.*;

class OtiNanaiCache {
	public OtiNanaiCache (Long exp, int max, Logger l) {
      logger = l;
      expiry = exp;
      limit = max;
      counter = 0;
      if ( limit < 50 ) {
         logger.info("[Cacher]: Limit cannot be less than 50, so nerr");
         limit = 50;
      }
      theCache = new HashMap<String,String>(limit*2);
      cacheTime = new HashMap<String, Long>(limit*2);
      logger.finest("[Cacher]: New OtiNanaiCache Initialized (time: "+exp+" items: "+limit+")");
	}

   private void tidy() {
      Long now = System.currentTimeMillis();
      for (String key: cacheTime.keySet()) {
         if ((now - cacheTime.get(key)) > expiry) {
            cacheTime.remove(key);
            theCache.remove(key);
            logger.fine("[Cacher]: Clearing item from cache \""+key+"\"");
         }
      }
      logger.info("[Cacher]: items left in cache: "+cacheTime.size());
      if (theCache.size() >= limit ) {
         logger.warning("[Cacher]: Nuking cache ("+theCache.size()+" items)");
         theCache = new HashMap<String,String>(limit);
         cacheTime = new HashMap<String, Long>(limit);
      }
      counter = 0;
   }

   public void cache(String keyword, String data) {
      logger.fine("[Cacher]: Adding keyword "+keyword);
      counter++;
      if (counter >= limit) {
         if (theCache.size() >= limit ) {
            tidy();
         }
      }
      theCache.put(keyword, data);
      cacheTime.put(keyword, System.currentTimeMillis());
   }

   public String getCached(String keyWord) {
      if (theCache.containsKey(keyWord)) {
         if ((System.currentTimeMillis() - cacheTime.get(keyWord)) > expiry) {
            logger.fine("[Cacher]: keyWord expired ing cache: "+keyWord);
            return null;
         }
         logger.fine("[Cacher]: keyWord found in cache: "+keyWord);
         return theCache.get(keyWord);
      } 
      logger.fine("[Cacher]: keyWord not found in cache: "+keyWord);
      return null;
   }

   private HashMap<String, Long> cacheTime;
   private HashMap<String,String> theCache;
   private Long expiry;
   private int limit;
   private int counter;
   private Logger logger;
}

