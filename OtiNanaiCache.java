import java.io.*;
import java.util.*;
import java.util.logging.*;

class OtiNanaiCache (int exp) {
	public OtiNanaiCache () {
      expiry = exp;
      theCache = new HashMap<String,String>();
      logger = l;
      logger.finest("[Ticker]: New OtiNanaiTicker Initialized");
	}

   public void 
   public void cache(String keyword, String data) {
      theCache.put(keyword, data);
   }

   public String getCached(String keyword) {
      if (theCache.containsKey(keyword)) {
         return theCache.get(keyWord);
      } 
      return null;
   }

   private HashMap<String,String> theCache;
   private int expiry;
   private Logger logger;
}

