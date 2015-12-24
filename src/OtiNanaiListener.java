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
		trackerMap = new ConcurrentHashMap<String, KeyWordTracker>();
		threadPool = Executors.newCachedThreadPool();

		redisHost = rh;
		jediTemple = new JedisPool(new JedisPoolConfig(), redisHost);

		try ( Jedis jedis = jediTemple.getResource() ) {
			kwtList = new LLString();
			logger.info("[Listener]: Loading keys from redis keylist: "+rKeyList);
			if (jedis.exists(rKeyList)) {
				for (String s : jedis.smembers(rKeyList)) {
					kwtList.add(s);
				}
			}
			logger.info("[Listener]: Loading existing keywords");
			for (String kw : kwtList) {
				logger.fine("[Listener]: Creating new Tracker: "+kw);
				trackerMap.put(kw, new RedisTracker(kw, as, atl, ath, acs, jediTemple, logger));
			}
			logger.info("[Listener]: Loading events from eventlist: "+rEventList);
			if (jedis.exists(rEventList)) {
				Long tts = 0l;
				String tev = new String();
				for (String s : jedis.smembers(rEventList)) {
					tts = Long.parseLong(s.substring(0, s.indexOf(" ")));
					tev = s.substring(s.indexOf(" ")+1);
					eventMap.put(tts, tev);
				}
			}
			logger.info("[Listener]: Loading units from unitlist: "+rUnitList);
			if (jedis.exists(rUnitList)) {
				String kw = new String();
				String unit = new String();
				for (String s : jedis.smembers(rUnitList)) {
					kw = s.substring(0, s.indexOf(" "));
					unit = s.substring(s.indexOf(" ")+1);
					unitMap.put(kw, unit);
				}
			}
			logger.info("[Listener]: Loading multipliers from multiplierlist: "+rMultipList);
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
			rStarList = new String("List_Of_Starred_Inputs");
			logger.info("[Listener]: Loading starred graphs from starlist: "+rStarList);
			starList = new LLString();
			if (jedis.exists(rStarList)) {
				for (String s : jedis.smembers(rStarList)) {
					starList.add(s);
				}
			}

			rStoreList = new String("List_Of_Stored_Inputs");
			logger.info("[Listener]: Loading stored queries from storelist: "+rStoreList);
			storeList = new LLString();
			if (jedis.exists(rStoreList)) {
				for (String s : jedis.smembers(rStoreList)) {
					storeList.add(s);
				}
			}

			rDashList = new String("List_Of_Dashboards");
			logger.info("[Listener]: Loading dashboard from: "+rDashList);
			dashList = new LLString();
			dashMap = new HashMap<String, LLString>();
			LLString dashData;
			if (jedis.exists(rDashList)) {
				for (String s : jedis.smembers(rDashList)) {
					String dashName = s.replaceAll("_Dashboard", "");
					logger.info("[Listener]: Loading dashboard "+dashName+" from "+s);
					dashData = new LLString();
					dashList.add(dashName);
					for (String t : jedis.smembers(s)) {
						logger.info("[Listener]: + "+t);
						dashData.add(t);
					}
					dashMap.put(dashName, dashData);
				}
			} else {
				logger.info("[Listener]: No dashboard list found");
			}
			dataSocket = ds;
			logger.finest("[Listener]: New OtiNanaiListener Initialized");
			System.out.println("Listener Initialized");
		} catch (Exception e) {
			logger.severe("[Listener]: "+e.getCause());
		}
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
				logger.severe("[Listener]: "+ioer.getCause());
				continue;
			}
			String sentence = new String(receivePacket.getData());
			InetAddress IPAddress = receivePacket.getAddress();
                        //System.out.println("[Listener]: "+sentence);
			logger.finest("[Listener]: "+ sentence +" from "+IPAddress);
			threadPool.submit(new OtiNanaiParser(this, sentence.replaceAll("\u0000.*", "").replaceAll("[\r\n]", ""), IPAddress, logger));
			//parseData(IPAddress, sentence.replaceAll("\u0000.*", "").replaceAll("[\r\n]", ""));
		}
	}

	public void addEvent(SomeRecord newRecord) {
		if (newRecord.isEvent()) {
                        eventMap.put(newRecord.getTimeStamp(), newRecord.getEvent());
			try ( Jedis jedis = jediTemple.getResource()) {
				jedis.sadd(rEventList, new String(newRecord.getTimeStamp()+" "+newRecord.getEvent()));
				logger.fine("[Listener]: New event-> "+newRecord.getEvent());
			} catch (Exception e) {
				logger.severe("[Listener]: "+e.getCause());
			}
		}
	}

	public LLString getKWTList() {
		return kwtList;
	}

	public KeyWordTracker getKWT(String key) {
		return trackerMap.get(key);
	}

	public String getType(String key) {
		if (trackerMap.containsKey(key)) {
			short t = trackerMap.get(key).getType();
			switch (t) {
				case OtiNanai.GAUGE: return new String("gauge");
				case OtiNanai.COUNTER: return new String("count");
				case OtiNanai.FREQ: return new String("freq");
				case OtiNanai.SUM: return new String("sum");
			}
		}
		return new String("unset");
	}

	public KeyWordTracker addKWT(String key) {
		if (!kwtList.contains(key)) {
			KeyWordTracker kwt = new RedisTracker(key, alarmSamples, lowAlarmThreshold, highAlarmThreshold, alarmConsecutiveSamples, jediTemple, logger);
			trackerMap.put(key, kwt);
			kwtList.add(key);
			try ( Jedis jedis = jediTemple.getResource()) {
				jedis.sadd(rKeyList, key);
			} catch (Exception e) {
				logger.severe("[Listener]: "+e.getCause());
			}
			return kwt;
		}
		return getKWT(key);
	}

	public void deleteKWT(String key) {
		logger.info("[Listener]: Deleting data from "+key);
		while (deleteLock) {
			try {
				Thread.sleep(2000l);
			} catch (InterruptedException ie) {
				logger.severe("[Listener]: waiting on ticker to delete: "+ie.getCause());
				break;
			}
		}
		deleteLock = true;
                KeyWordTracker kwt = getKWT(key);
                kwtList.remove(key);
		try ( Jedis jedis = jediTemple.getResource()) {
			jedis.srem(rKeyList, key);
			jedis.del(key);
		} catch (Exception e) {
			logger.severe("[Listener]: "+e.getCause());
		}
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
				logger.severe("[Listener]: waiting on deletion to tick: "+ie.getCause());
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
		try ( Jedis jedis = jediTemple.getResource() ) {
			if (unitMap.containsKey(kw))
				jedis.srem(rUnitList, kw+" "+unitMap.get(kw));
			unitMap.put(kw, units);
			jedis.sadd(rUnitList, kw+" "+units);
		} catch (Exception e) {
			logger.severe("[Listener]: "+e.getCause());
		}
	}

	public String getUnits(String kw) {
		if (unitMap.containsKey(kw)) 
			return "("+unitMap.get(kw)+")";
			
		return new String("");
	}

	public void setMultiplier(String kw, float multip) {
		try ( Jedis jedis = jediTemple.getResource() ) {
			if (multipMap.containsKey(kw))
				jedis.srem(rMultipList, kw+" "+multipMap.get(kw));
			multipMap.put(kw, multip);
			jedis.sadd(rMultipList, kw+" "+multip);
                } catch (Exception e) {
                        logger.severe("[Listener]: "+e.getCause());
                }
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

	public boolean toggleStar(String input) {
		try ( Jedis jedis = jediTemple.getResource() ) {
			if (starList.contains(input)) {
				starList.remove(input);
				jedis.srem(rStarList, input);
				return false;
			} else {
				starList.add(input);
				jedis.sadd(rStarList, input);
				return true;
			}
                } catch (Exception e) {
                        logger.severe("[Listener]: "+e.getCause());
                }
		return false;
	}

	public boolean storeQuery(String input) {
		try ( Jedis jedis = jediTemple.getResource() ) {
			if (!storeList.contains(input)) {
				storeList.add(input);
				jedis.sadd(rStoreList, input);
				return true;
			}
                } catch (Exception e) {
                        logger.severe("[Listener]: "+e.getCause());
                }
		return false;
	}

        public LLString getStoreList() {
                return storeList;
        }

	public boolean isStarred(String input) {
		return starList.contains(input);
	}

	public LLString getStarList() {
		return starList;
	}

	public LLString getDashboard(String dashboardName) {
		if (dashMap.containsKey(dashboardName)) {
			return dashMap.get(dashboardName);
		} else {
			return new LLString();
		}
	}

	public LLString getDashBoardList() {
		return dashList;
	}

	public boolean dashContainsKey(String dashboardName, String key, boolean stacked) {
                if (stacked)
                        key = key + " --stacked ";
                logger.info("[Listener]: Checking "+dashboardName+" for \""+key+"\"");
                try {
                        return (dashMap.get(dashboardName).contains(key));
                } catch (Exception e) {
                        return false;
                }
	}

	public boolean toggleDashboard(String kws, String dashboardName) {
		String rDashboardKey = dashboardName+"_Dashboard";
                try ( Jedis jedis = jediTemple.getResource() ) {
			if (!dashList.contains(dashboardName)) {
				dashMap.put(dashboardName, new LLString());
				dashList.add(dashboardName);
				jedis.sadd(rDashList, rDashboardKey);
			}

			if (dashMap.get(dashboardName).contains(kws)) {
				jedis.srem(rDashboardKey, kws);
				dashMap.get(dashboardName).remove(kws);
				if (dashMap.get(dashboardName).isEmpty()) {
					dashList.remove(dashboardName);
					jedis.srem(rDashList, rDashboardKey);
				}
				return false;
			} else {
				jedis.sadd(rDashboardKey, kws);
				dashMap.get(dashboardName).add(kws);
				return true;
			}
		} catch (Exception e) {
                        logger.severe("[Listener]: "+e.getCause());
                }
                return false;
	}

	public boolean updateDashboard(String kws, String dashboardName) {
		String rDashboardKey = dashboardName+"_Dashboard";
		if (kws.toLowerCase().contains("donotsavetodashboard"))
			return false;
                try ( Jedis jedis = jediTemple.getResource() ) {
			if (!dashList.contains(dashboardName)) {
				dashList.add(dashboardName);
                                jedis.sadd(rDashList, rDashboardKey);
			}

			jedis.del(rDashboardKey);
			LLString updatedList = new LLString();
			String[] fromJS = kws.split(",");
			for (String input : fromJS) {
				jedis.sadd(rDashboardKey, input);
				updatedList.add(input);
			}
			dashMap.put(dashboardName, updatedList);
			return true;
		} catch (Exception e) {
                        logger.severe("[Listener]: "+e.getCause());
                }
                return false;
	}

	private ExecutorService threadPool;
	private ConcurrentHashMap<String,KeyWordTracker> trackerMap;
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
	private String rStarList;
	private String rStoreList;
	private String rDashList;
	private LLString starList;
	private LLString storeList;
	private LLString kwtList;
	private String redisHost;
	private JedisPool jediTemple;
        private TreeMap<Long, String> eventMap;
	private HashMap<String, String> unitMap;
	private HashMap<String, Float> multipMap;
	private HashMap<String, LLString> dashMap;
	private LLString dashList;
	private boolean deleteLock;
}
