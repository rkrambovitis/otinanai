package gr.phaistosnetworks.admin.otinanai;

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
	 * @param	al 	Time diff for alarm to still be active
	 * @param	ps 	Number of samples to keep for preview graphs
	 * @param	l	the logger to log to
	 */
	public OtiNanaiListener(DatagramSocket ds, int as, float at, int acs, Logger l, short st, String bucketName, String rh, String redisKeyWordList, String redisSavedQueries) {
		logger = l;
		alarmSamples = as;
		alarmThreshold = at;
		alarmConsecutiveSamples = acs;
		storageType = st;
		deleteLock = false;
		rKeyList = redisKeyWordList;
		rSavedQueries = redisSavedQueries;
		kwtList = new LLString();
		redisHost = rh;
		trackerMap = new HashMap<String, KeyWordTracker>();

		if (st == OtiNanai.REDIS) {
			jedis = new Jedis(redisHost);
			jedis2 = new Jedis(redisHost);
			kwtList = new LLString();
			if (jedis.exists(rKeyList)) {
				for (String s : jedis.smembers(rKeyList)) {
					kwtList.add(s);
				}
			}
			for (String kw : kwtList) { 
				logger.info("[Listener]: Creating new Tracker: "+kw);
				trackerMap.put(kw, new RedisTracker(kw, as, at, acs, redisHost, jedis2, logger));
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
		if (newRecord.isGauge()) {
			recType = OtiNanai.GAUGE;
		} else if (newRecord.isCounter()) {
			recType = OtiNanai.COUNTER;
		} else if (newRecord.isSum()) {
			recType = OtiNanai.SUM;
		}

		ArrayList<String> theKeys = newRecord.getKeyWords();

		for (String kw : theKeys) {
			if (recType == OtiNanai.FREQ) {
				try { 
					Float.parseFloat(kw);
					continue;
				} catch (NumberFormatException e) {}

				if (kw.equals("")) {
					continue;
				}

			} 

			KeyWordTracker kwt = getKWT(kw);
			if (kwt == null) {
				logger.info("[Listener]: New Tracker created: kw: "+kw+" host: "+newRecord.getHostName());
				if (storageType == OtiNanai.REDIS)
					kwt = new RedisTracker(kw, alarmSamples, alarmThreshold, alarmConsecutiveSamples, redisHost, jedis2, logger);
				else
					kwt = new MemTracker(kw, alarmSamples, alarmThreshold, alarmConsecutiveSamples, logger);

				kwt.setType(recType);
			}

			if (newRecord.isGauge()) {
				kwt.putGauge(newRecord.getGauge());
			} else if (newRecord.isSum()) {
				kwt.putSum(newRecord.getSum());
			} else if (newRecord.isCounter()) {
				kwt.putCounter(newRecord.getCounter());
			} else {
				kwt.putFreq();
			}

			trackerMap.put(kw, kwt);
			if (!kwtList.contains(kw)) {
				kwtList.add(kw);
				if (storageType == OtiNanai.REDIS)
					jedis.sadd(rKeyList, kw);
			}
		}
	}

	public LLString getKWTList() {
		return kwtList;
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
			} catch (Exception e) {
				logger.severe("[Listener]: Unable to tick "+kw+" :\n"+e);
			}
		}
		deleteLock=false;
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
	private int alarmSamples;
	private float alarmThreshold;
	private int alarmConsecutiveSamples;
	private DatagramSocket dataSocket;
	private Logger logger;
	private short storageType;
	private String rKeyList;
	private String rSavedQueries;
	private LLString kwtList;
	private Jedis jedis;
        private Jedis jedis2;
	private String redisHost;
	private boolean deleteLock;
}
