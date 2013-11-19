package gr.phaistosnetworks.admin.otinanai;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.*;
import com.basho.riak.client.cap.*;
import com.basho.riak.client.raw.*;
import com.basho.riak.client.raw.pbc.*;


import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.logging.*;

import redis.clients.jedis.*;

class OtiNanaiListener implements Runnable {

	/**
	 * Multithread constructor
	 * @param	ds	the DatagraSocket to be used
    * @param   al Time diff for alarm to still be active
    * @param   ps Number of samples to keep for preview graphs
	 * @param	l	the logger to log to
	 */
	public OtiNanaiListener(DatagramSocket ds, long al, int as, float at, int acs, Logger l, short st, String bucketName, String riakHost, int riakPort) {
		logger = l;
      alarmLife = al;
      alarmSamples = as;
      alarmThreshold = at;
      alarmConsecutiveSamples = acs;
      storageType = st;
      riakBucket = null;
      deleteLock = false;
      rKeyList = new String("existing_keywords_list");
      kwtList = new LLString();
      trackerMap = new HashMap<String, KeyWordTracker>();

      if (st == OtiNanai.RIAK) {
         try {
            logger.config("[Listener]: Setting up Riak");

            IRiakClient riakClient = RiakFactory.pbcClient(riakHost, riakPort);
            //riakBucket = riakClient.createBucket(bucketName).nVal(1).r(1).disableSearch().lastWriteWins(true).backend("eleveldb").execute();
            riakBucket = riakClient.createBucket(bucketName).nVal(1).r(1).disableSearch().lastWriteWins(true).execute();
            logger.config("[Listener]: Riak.getAllowSiblings = " + riakBucket.getAllowSiblings());
            logger.config("[Listener]: Riak.getBackend = " + riakBucket.getBackend());
            logger.config("[Listener]: Riak.getBasicQuorum = " + riakBucket.getBasicQuorum());
            logger.config("[Listener]: Riak.getBigVClock = " + riakBucket.getBigVClock());
            logger.config("[Listener]: Riak.getSmallVClock = " + riakBucket.getSmallVClock());
            logger.config("[Listener]: Riak.getOldVClock = " + riakBucket.getOldVClock());
            logger.config("[Listener]: Riak.getChashKeyFunction = " + riakBucket.getChashKeyFunction());
            logger.config("[Listener]: Riak.getLastWriteWins = " + riakBucket.getLastWriteWins());
            logger.config("[Listener]: Riak.getNVal = " + riakBucket.getNVal());
            logger.config("[Listener]: Riak.getDW = " + riakBucket.getDW().getIntValue());
            logger.config("[Listener]: Riak.getPR = " + riakBucket.getPR().getIntValue());
            logger.config("[Listener]: Riak.getPW = " + riakBucket.getPW().getIntValue());
            logger.config("[Listener]: Riak.getRW = " + riakBucket.getRW().getIntValue());
            logger.config("[Listener]: Riak.getR = " + riakBucket.getR().getIntValue());
            logger.config("[Listener]: Riak.getW = " + riakBucket.getW().getIntValue());

            kwtList = getKWTList();
            for (String kw : kwtList) { 
               logger.info("[Listener]: Creating new Tracker: "+kw);
               trackerMap.put(kw, new RiakTracker(kw, as, at, acs, logger, riakBucket));
            }
         } catch (RiakException re) {
            logger.severe("[Listener]: "+re);
            System.exit(1);
         }
         storeKWTList(kwtList);
      } else if (st == OtiNanai.REDIS) {
         jedis = new Jedis("localhost");
         kwtList = new LLString();
         if (jedis.exists(rKeyList)) {
            for (String s : jedis.smembers(rKeyList)) {
               kwtList.add(s);
            }
         }
         for (String kw : kwtList) { 
            logger.info("[Listener]: Creating new Tracker: "+kw);
            trackerMap.put(kw, new RedisTracker(kw, as, at, acs, logger));
         }
      }
		dataSocket = ds;
		logger.finest("[Listener]: New OtiNanaiListener Initialized");
	}

	/**
	 * necessary threaded run method.
	 * infinite loop.
	 * Tries to receive data, and then sends it to parseData()
	 */
	public void run() {
		while(true) {
			byte[] receiveData = new byte[1490];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				dataSocket.receive(receivePacket);
			} catch (IOException ioer) {
				System.out.println(ioer);
				logger.severe("[Listener]: "+ioer.getStackTrace());
				continue;
			}
			String sentence = new String(receivePacket.getData());
			InetAddress IPAddress = receivePacket.getAddress();
			logger.finest("[Listener]: Listener received message from "+IPAddress);
			parseData(IPAddress, sentence.replaceAll("\u0000.*", "").replaceAll("[\r\n]", ""));
		}
	}
	
	/**
	 * parses the dato into a SomeRecord object and associates it with keywords.
	 * For each keyword there is a list with unique dato id (nanoTime).
	 * Each id and SomeRecord pair are added to a hashmap.
	 * @param	hip	Host IP (who sent us the dato)
	 * @param	theDato	The dato.
	 */
	private void parseData(InetAddress hip, String theDato) {
		logger.finest("[Listener]: + Attempting to parse: \""+theDato+"\" from "+hip);

		SomeRecord newRecord = new SomeRecord(hip, theDato);
      short recType = OtiNanai.FREQ;
		ArrayList<String> theKeys = newRecord.getKeyWords();

		for (String kw : theKeys) {
         if (newRecord.isGauge()) {
            recType = OtiNanai.GAUGE;
         } else if (newRecord.isCounter()) {
            recType = OtiNanai.COUNTER;
         } else {
            try { 
               Float.parseFloat(kw);
               logger.finest("[Listener]: Number, ignored");
               continue;
            } catch (NumberFormatException e) {}
            if (kw.equals("")) {
               logger.finest("[Listener]: Blank Keyword, ignored");
               continue;
            }
			} 

         KeyWordTracker kwt = getKWT(kw);
         if (kwt == null) {
            logger.info("[Listener]: New Tracker created: kw: "+kw+" host: "+newRecord.getHostName());
            if (storageType == OtiNanai.RIAK)
               kwt = new RiakTracker(kw, alarmSamples, alarmThreshold, alarmConsecutiveSamples, logger, riakBucket);
            else if (storageType == OtiNanai.REDIS)
               kwt = new RedisTracker(kw, alarmSamples, alarmThreshold, alarmConsecutiveSamples, logger);
            else
               kwt = new MemTracker(kw, alarmSamples, alarmThreshold, alarmConsecutiveSamples, logger);
         }

         if (newRecord.isGauge()) {
            kwt.put(newRecord.getGauge());
         } else if (newRecord.isCounter()) {
            kwt.put(newRecord.getCounter());
         } else {
            kwt.put();
         }

         trackerMap.put(kw, kwt);
         if (!kwtList.contains(kw)) {
            kwtList.add(kw);
            if (storageType == OtiNanai.REDIS) {
               jedis.sadd(rKeyList, kw);
            } else {
               storeKWTList(kwtList);
            }
         }
		}
      
	}

   public LLString getKWTList() {
      logger.info("[Listener]: Trying to get kwtList");
      if (storageType == OtiNanai.MEM || storageType == OtiNanai.REDIS) {
         return kwtList;
      } else {
         try {
            kwtList = riakBucket.fetch(rKeyList, LLString.class).execute();
            if (kwtList == null) {
               kwtList = new LLString();
            }
         } catch (RiakRetryFailedException rrfe) {
            kwtList = new LLString();
            logger.severe("[Listener]: Unable to retrieve keyList from riak\n"+rrfe);
         }
      }
      return kwtList;
   }

   private boolean storeKWTList(LLString toStore) {
      logger.fine("[Listener]: Trying to store kwtList");
      if (storageType == OtiNanai.RIAK) {
         try {
            riakBucket.store(rKeyList, toStore).execute();
            logger.fine("[Listener]: kwtList stored on riak");
         } catch (Exception rrfe) {
            logger.severe("[Listener]: Unable to store KWTList\n"+rrfe);
            return false;
         }
      } else {
         logger.severe("[Listener]: Cannot Store KWTlist for this storage engine (type: "+storageType+" - Not Implemented)");
      }
      return true;
   }

   public KeyWordTracker getKWT(String key) {
      return trackerMap.get(key);
   }

   public void deleteKWT(String key) {
      logger.info("[Listener]: Deleting data from "+key);
      while (deleteLock) {
         try {
            Thread.sleep(2000l);
         } catch (InterruptedException ie) {
            logger.severe("[Listener]: waiting on ticker to delete: "+ie.getStackTrace());
            break;
         }
      }
      deleteLock = true;
      if (storageType == OtiNanai.MEM) {
         kwtList.remove(key);
         trackerMap.remove(key);
      } else if (storageType == OtiNanai.REDIS) {
         KeyWordTracker kwt = getKWT(key);
         kwtList.remove(key);
         jedis.srem(rKeyList, key);
         jedis.del(key);
         trackerMap.remove(key);
         kwt.delete();
      } else {
         KeyWordTracker kwt = getKWT(key);
         kwtList = getKWTList();
         kwtList.remove(key);
         storeKWTList(kwtList);
         trackerMap.remove(key);
         kwt.delete();
      }
      deleteLock = false;
   }

	/**
	 * Access Method
	 */
	public Logger getLogger() {
		return logger;
	}

   public void tick() {
      while (deleteLock) {
         try {
            Thread.sleep(2000l);
         } catch (InterruptedException ie) {
            logger.severe("[Listener]: waiting on deletion to tick: "+ie.getStackTrace());
            break;
         }
      }
      deleteLock=true;
      LLString tempKW = new LLString();
      tempKW.addAll(trackerMap.keySet());
      for (String kw : tempKW) {
         try {
            trackerMap.get(kw).tick();
         } catch (NullPointerException npe) {
            logger.severe("[Listener]: Unable to tick "+kw+" (deleted?)\n"+npe);
         }
      }
      deleteLock=false;
   }

   public long getAlarmLife() {
      return alarmLife;
   }

   public long getAlarm(String kw) {
      if (!kwtList.contains(kw)) {
         return 0L;
      } else {
         return getKWT(kw).getAlarm();
      }
   }

   private HashMap<String,KeyWordTracker> trackerMap;
	private int port;
   private long alarmLife;
   private int alarmSamples;
   private float alarmThreshold;
   private int alarmConsecutiveSamples;
	private DatagramSocket dataSocket;
	private Logger logger;
   private short storageType;
   private Bucket riakBucket;
   private String rKeyList;
   private LLString kwtList;
   private Jedis jedis;
   private boolean deleteLock;
}
