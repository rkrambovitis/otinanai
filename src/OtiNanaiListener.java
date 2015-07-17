package gr.phaistosnetworks.admin.otinanai;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.logging.*;

import redis.clients.jedis.*;

class OtiNanaiListener implements Runnable {

	public OtiNanaiListener(DatagramSocket ds, int as, float atl, float ath, int acs, Logger l, String bucketName, String rh, String redisKeyWordList, String redisSavedQueries, String redisEventList, String redisUnitList, String redisMultipList) {
		logger = l;
		alarmSamples = as;
		lowAlarmThreshold = atl;
		highAlarmThreshold = ath;
		alarmConsecutiveSamples = acs;
		deleteLock = false;
		rKeyList = redisKeyWordList;
                rEventList = redisEventList;
                rUnitList = redisUnitList;
                rMultipList = redisMultipList;
                eventMap = new TreeMap<Long, String>();
		unitMap = new HashMap<String, String>();
		multipMap = new HashMap<String, Float>();
		rSavedQueries = redisSavedQueries;
		kwtList = new LLString();
		redisHost = rh;
		trackerMap = new HashMap<String, KeyWordTracker>();

                jedis = new Jedis(redisHost);
                jedis2 = new Jedis(redisHost);
                kwtList = new LLString();
		System.out.println("Loading keys from redis keylist: "+rKeyList);
                if (jedis.exists(rKeyList)) {
                        for (String s : jedis.smembers(rKeyList)) {
                                kwtList.add(s);
                        }
                }
		System.out.println("Loading existing keywords");
                for (String kw : kwtList) { 
                        logger.info("[Listener]: Creating new Tracker: "+kw);
                        trackerMap.put(kw, new RedisTracker(kw, as, atl, ath, acs, redisHost, jedis2, logger));
                }
		System.out.println("Loading events from eventlist: "+rEventList);
                if (jedis.exists(rEventList)) {
                        Long tts = 0l;
                        String tev = new String();
                        for (String s : jedis.smembers(rEventList)) {
                                tts = Long.parseLong(s.substring(0, s.indexOf(" ")));
                                tev = s.substring(s.indexOf(" ")+1);
                                eventMap.put(tts, tev);
                        }
                }
		System.out.println("Loading units from unitlist: "+rUnitList);
                if (jedis.exists(rUnitList)) {
                        String kw = new String();
                        String unit = new String();
                        for (String s : jedis.smembers(rUnitList)) {
                                kw = s.substring(0, s.indexOf(" "));
                                unit = s.substring(s.indexOf(" ")+1);
                                unitMap.put(kw, unit);
                        }
                }
		System.out.println("Loading multipliers from multiplierlist: "+rMultipList);
                if (jedis.exists(rMultipList)) {
                        String kw = new String();
                        float multip = 1f;
                        for (String s : jedis.smembers(rMultipList)) {
                                kw = s.substring(0, s.indexOf(" "));
                                try {
                                        multip = Float.parseFloat(s.substring(s.indexOf(" ")+1));
                                        multipMap.put(kw, multip);
                                } catch (NumberFormatException nfe) {
                                        System.err.println("Broken multiplier: "+s);
                                }
                        }
                }
		dataSocket = ds;
		logger.finest("[Listener]: New OtiNanaiListener Initialized");
		System.out.println("Listener Initialized");
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
		} else if (newRecord.isEvent()) {
                        eventMap.put(newRecord.getTimeStamp(), newRecord.getEvent());
                        jedis.sadd(rEventList, new String(newRecord.getTimeStamp()+" "+newRecord.getEvent()));
			logger.info("[Listener]: New event-> "+newRecord.getEvent());
			System.err.println("New Event: -> "+newRecord.getEvent());
                        return;
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
                                kwt = new RedisTracker(kw, alarmSamples, lowAlarmThreshold, highAlarmThreshold, alarmConsecutiveSamples, redisHost, jedis2, logger);
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
                KeyWordTracker kwt = getKWT(key);
                kwtList.remove(key);
                jedis.srem(rKeyList, key);
                jedis.del(key);
                trackerMap.remove(key);
                kwt.delete();
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

	public TreeMap<Long, String> getEvents() {
		TreeMap<Long, String> emc = new TreeMap<Long, String>();
		emc.putAll(eventMap);
		return emc;
	}

	public void setUnits(String kw, String units) {
		if (unitMap.containsKey(kw))
			jedis.srem(rUnitList, kw+" "+unitMap.get(kw));
		unitMap.put(kw, units);
                jedis.sadd(rUnitList, kw+" "+units);
	}

	public String getUnits(String kw) {
		if (unitMap.containsKey(kw)) 
			return "("+unitMap.get(kw)+")";
			
		return new String("");
	}

	public void setMultiplier(String kw, float multip) {
		if (multipMap.containsKey(kw))
			jedis.srem(rMultipList, kw+" "+multipMap.get(kw));
		multipMap.put(kw, multip);
                jedis.sadd(rMultipList, kw+" "+multip);
	}

	public float getMultiplier(String kw) {
		if (multipMap.containsKey(kw)) 
			return multipMap.get(kw);
		return 1f;
	}
        
        public void alarmEnabled(String kw, boolean onOrOff) {
                 if (kwtList.contains(kw))
                         getKWT(kw).alarmEnabled(onOrOff);
        }

	public boolean alarmEnabled(String kw) {
		if (kwtList.contains(kw))
			return getKWT(kw).alarmEnabled();
		return false;
	}

	private HashMap<String,KeyWordTracker> trackerMap;
	private int port;
	private int alarmSamples;
	private float lowAlarmThreshold;
	private float highAlarmThreshold;
	private int alarmConsecutiveSamples;
	private DatagramSocket dataSocket;
	private Logger logger;
	private String rKeyList;
        private String rEventList;
	private String rUnitList;
	private String rMultipList;
	private String rSavedQueries;
	private LLString kwtList;
	private Jedis jedis;
        private Jedis jedis2;
	private String redisHost;
        private TreeMap<Long, String> eventMap;
	private HashMap<String, String> unitMap;
	private HashMap<String, Float> multipMap;
	private boolean deleteLock;
}
