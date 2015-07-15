package gr.phaistosnetworks.admin.otinanai;

import java.net.ServerSocket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;

/**
 * Otinanai means Whatever, anything, who cares?
 * You pipe it data, it indexes the data, aggregates it, detects anomalies and generates graphs
 * well... it will anyway, someday
 */
class OtiNanai {

	/**
	 * Standard Constructor.
	 * @param	listenerPort	The udp port to pipe data to.
	 * @param	listenerThreads	Number of listener threads (not implemented yet)
	 * @param	webPort	The web interface port
	 * @param	webThreads	The number of web listener threads
	 */
	public OtiNanai(int listenerPort, int listenerThreads, int webPort, int webThreads, long cacheTime, int cacheItems, int alarmSamples, float lowAlarmThreshold, float highAlarmThreshold, int alarmConsecutiveSamples, String logFile, String logLevel, String bucketName, String redisHost, String redisKeyWordList, String redisSavedQueries, String redisEventList, String redisUnitList, String redisMultipList){
		setupLogger(logFile, logLevel);
		try {
			// Listener
			logger.config("[Init]: Setting up new DatagramSocket Listener");
			logger.config("[Init]: listenerPort "+listenerPort);
			logger.config("[Init]: listenerThreads "+listenerThreads);
			logger.config("[Init]: webPort "+webPort);
			logger.config("[Init]: webThreads "+webThreads);
			logger.config("[Init]: cacheTime "+cacheTime + "ms");
			logger.config("[Init]: cacheItems "+cacheItems);
			logger.config("[Init]: alarmSamples: "+alarmSamples);
			logger.config("[Init]: lowAlarmThreshold: "+lowAlarmThreshold);
			logger.config("[Init]: highAlarmThreshold: "+highAlarmThreshold);
			logger.config("[Init]: alarmLife: "+ALARMLIFE + "ms");
			logger.config("[Init]: alarmConsecutiveSamples: "+alarmConsecutiveSamples);
			logger.config("[Init]: logFile: "+logFile);
			logger.config("[Init]: logLevel: "+logLevel);
			logger.config("[Init]: bucketName: "+bucketName);
			logger.config("[Init]: redisHost: "+redisHost);
			logger.config("[Init]: redisKeyWordList: "+redisKeyWordList);
			logger.config("[Init]: redisSavedQueries: "+redisSavedQueries);
			logger.config("[Init]: redisEventList: "+redisEventList);
			logger.config("[Init]: redisUnitList: "+redisUnitList);
			logger.config("[Init]: redisMultipList: "+redisMultipList);
			logger.config("[Init]: notifyScript: "+NOTIFYSCRIPT);
			logger.config("[Init]: Web url: "+WEBURL);

			DatagramSocket ds = new DatagramSocket(listenerPort);
			OtiNanaiListener onl = new OtiNanaiListener(ds, alarmSamples, lowAlarmThreshold, highAlarmThreshold, alarmConsecutiveSamples, logger, bucketName, redisHost, redisKeyWordList, redisSavedQueries, redisEventList, redisUnitList, redisMultipList);
			new Thread(onl).start();

			// Ticker
			logger.config("[Init]: Setting up ticker");
                        if (TICKER_INTERVAL > 0) {
                                OtiNanaiTicker ont = new OtiNanaiTicker(onl, logger);
                                new Thread(ont).start();
                        } else
                                System.err.println("[Init]: Ticker Disabled");

			// Cacher
			logger.config("[Init]: Setting up cacher (life: " +cacheTime+ " items: "+cacheItems+")");
			OtiNanaiCache onc = new OtiNanaiCache(cacheTime, cacheItems, logger);

			// Web Interface
			logger.config("[Init]: Setting up new Web Listener on port "+webPort);
			ServerSocket ss = new ServerSocket(webPort);
			OtiNanaiWeb onw = new OtiNanaiWeb(onl, onc, ss, logger);
			for (int i=1; i<=webThreads; i++) {
				logger.config("[Init]: Starting web thread: "+i+"/"+webThreads);
				new Thread(onw).start();
			}

		} catch (java.lang.Exception e) {
			System.err.println(e);
			logger.severe("[Init]: "+e.getStackTrace());
			System.exit(1);
		}
		//OtiNanaiCommander onc = new OtiNanaiCommander(onl);
		//new Thread(onc).start();
	}

	/**
	 * This method does excactly what is says on the tin.
	 * Sets up a new logger to be used by OtiNanai.
	 * @param	fileName	The log filename (full path)
	 * @param	logLevel	The log level
	 */
	private void setupLogger(String fileName, String logLevel) {
		try {
			// This one is for log messages
			FileHandler fh = new FileHandler(fileName, 52428800, 2, true);
			logger = Logger.getLogger("OtiNanai");
			String lcll = logLevel.toLowerCase();
			if (lcll.equals("severe")) {
				logger.setLevel(Level.SEVERE);
			} else if  (lcll.equals("warning")) {
				logger.setLevel(Level.WARNING);
			} else if  (lcll.equals("info")) {
				logger.setLevel(Level.INFO);
			} else if  (lcll.equals("config")) {
				logger.setLevel(Level.CONFIG);
			} else if  (lcll.equals("fine")) {
				logger.setLevel(Level.FINE);
			} else if  (lcll.equals("finer")) {
				logger.setLevel(Level.FINER);
			} else if  (lcll.equals("finest")) {
				logger.setLevel(Level.FINEST);
			} else if  (lcll.equals("all")) {
				logger.setLevel(Level.ALL);
			} else {
				logger.setLevel(Level.WARNING);
			}

			fh.setFormatter(new MyLogFormatter());
			logger.setUseParentHandlers(false);
			logger.addHandler(fh);

		} catch (IOException e) {
			System.err.println("Cannot create log file");
			System.exit(1);
		}
	}

	/**
	 * Main
	 * Deals with command line arguments and creates a new OtiNanai instance
	 */
	public static void main(String args[]) {
		String arg;
		int webPort = 9876;
		int webThreads = 5;
		int udpPort = 9876;
		int tcpPort = 1010;
		int listenerThreads = 5;
		Long cacheTime = 120000L;
		Long alarmLife = 86400000L;
		int alarmSamples = 20;
		float lowAlarmThreshold = 15.0f;
		float highAlarmThreshold = 6.0f;
                float parsedFloat = 0f;
		int cacheItems = 50; 
		int alarmConsecutiveSamples = 3;
		int step1Hours = 28;
		boolean step1HoursOverride = true;
		int step2Hours = 340;
		boolean step2HoursOverride = true;
		String bucketName = new String("OtiNanai");
		String logFile = new String("/var/log/otinanai.log");
		String logLevel = new String("INFO");
		String redisHost = new String("localhost");
		String redisKeyWordList = new String("existing_keywords_list"); 
		String redisSavedQueries = new String("saved_queries_list");
                String redisEventList = new String("OtiNanai_Event_List");
                String redisUnitList = new String("OtiNanai_Unit_List");
                String redisMultipList = new String("OtiNanai_Multiplier_List");
		String notifyScript = new String("/tmp/otinanai_notifier");
		String webUrl = new String();
		try {
			webUrl = "http://"+InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException uhe) {
			System.out.println("Unable to determine local hostname, using 127.0.0.1 instead");
			System.err.println(uhe);
			webUrl = "http://127.0.0.1";
		}
		String sane = new String();
		boolean customUrl = false;
		try {
			for (int i=0; i<args.length; i++) {
				arg = args[i];
				System.out.println("arg " +i+ ": " +arg);
				switch (arg) {
					case "-wp":
						i++;
						webPort = Integer.parseInt(args[i]);
						System.out.println("Web port = " + webPort);
						break;	
					case "-lp":
						i++;
						udpPort = Integer.parseInt(args[i]);
						tcpPort = Integer.parseInt(args[i]);
						System.out.println("Listener port = " + udpPort);
						break;
					case "-wt":
						i++;
						webThreads = Integer.parseInt(args[i]);
						System.out.println("Web Threads = " + webThreads);
						break;
					case "-ct":
						i++;
						cacheTime = 1000*(Long.parseLong(args[i]));
						System.out.println("cacheTime = " + cacheTime);
						break;
					case "-ci":
						i++;
						cacheItems = Integer.parseInt(args[i]);
						System.out.println("cacheItems = " + cacheItems);
						break;
					case "-al":
						i++;
						alarmLife = 1000*(Long.parseLong(args[i]));
						System.out.println("alarmLife = " + alarmLife);
						break;
					case "-as":
						i++;
						alarmSamples = Integer.parseInt(args[i]);
						System.out.println("alarmSamples = " + alarmSamples);
						break;
					case "-atl":
						i++;
						parsedFloat = Float.parseFloat(args[i]);
                                                if (parsedFloat <= 0f) 
                                                        System.out.println("Invalid Low Alarm Threshold given.");
                                                else 
                                                        lowAlarmThreshold = parsedFloat;
						System.out.println("lowAlarmThreshold = " + lowAlarmThreshold);
						break;
					case "-ath":
						i++;
						parsedFloat = Float.parseFloat(args[i]);
                                                if (parsedFloat <= 0f)
                                                        System.out.println("Invalid Alarm Threshold given.");
                                                else
                                                        highAlarmThreshold = parsedFloat;
						System.out.println("highAlarmThreshold = " + highAlarmThreshold);
						break;
					case "-acs":
						i++;
						alarmConsecutiveSamples = Integer.parseInt(args[i]);
						System.out.println("alarmConsecutiveSamples = " + alarmConsecutiveSamples);
						break;
					case "-lf":
						i++;
						logFile = args[i];
						System.out.println("logFile = " + logFile);
						break;
					case "-ll":
						i++;
						logLevel = args[i];
						System.out.println("logLevel = " + logLevel);
						break;
					case "-bn":
						i++;
						System.out.println("Bucket Name = " + args[i]);
						bucketName = args[i];
						break;
					case "-rh":
						i++;
						System.out.println("redis host = " + args[i]);
						redisHost = args[i];
						break;
					case "-rdkwlist":
						i++;
						sane = args[i].replaceAll("[-#'$+=!@$%^&*()|'\\/\":,?<>{};]","_"); 
						System.out.println("redisKeyWordList = " + sane);
						redisKeyWordList = sane;
						break;
					case "-rdsvq":
						i++;
						sane = args[i].replaceAll("[-#'$+=!@$%^&*()|'\\/\":,?<>{};]","_"); 
						System.out.println("redisSavedQueries = " + sane);
						redisSavedQueries = sane;
						break;
					case "-rdevtlist":
						i++;
						sane = args[i].replaceAll("[-#'$+=!@$%^&*()|'\\/\":,?<>{};]","_"); 
						System.out.println("redisEventList = " + sane);
						redisEventList = sane;
						break;
					case "-rdunitlist":
						i++;
						sane = args[i].replaceAll("[-#'$+=!@$%^&*()|'\\/\":,?<>{};]","_"); 
						System.out.println("redisUnitList = " + sane);
						redisUnitList = sane;
						break;
					case "-rdmultiplist":
						i++;
						sane = args[i].replaceAll("[-#'$+=!@$%^&*()|'\\/\":,?<>{};]","_"); 
						System.out.println("redisMultipList = " + sane);
						redisMultipList = sane;
						break;
					case "-s1samples":
						i++;
						System.out.println("-s1samples is Deprecated. Please use -s1hours instead");
						System.out.println("step1Samples = " + args[i]);
						STEP1_MAX_SAMPLES = Integer.parseInt(args[i]);
						step1HoursOverride = false;
						break;
					case "-s1hours":
						i++;
						System.out.println("step1hours = " + args[i]);
						step1Hours = Integer.parseInt(args[i]);
						step1HoursOverride = true;
						break;
					case "-s1agg":
						i++;
						System.out.println("step1SamplesToMerge = " + args[i]);
						STEP1_SAMPLES_TO_MERGE = Integer.parseInt(args[i]);
						break;
					case "-s2samples":
						i++;
						System.out.println("-s2samples is Deprecated. Please use -s2hours instead");
						System.out.println("step2Samples = " + args[i]);
						STEP2_MAX_SAMPLES = Integer.parseInt(args[i]);
						step2HoursOverride = false;
						break;
					case "-s2hours":
						i++;
						System.out.println("step2hours = " + args[i]);
						step2Hours = Integer.parseInt(args[i]);
						step2HoursOverride = true;
						break;
					case "-s2agg":
						i++;
						System.out.println("step2SamplesToMerge = " + args[i]);
						STEP2_SAMPLES_TO_MERGE = Integer.parseInt(args[i]);
						break;
					case "-tick":
						i++;
						System.out.println("TickerInterval (s) = " + args[i]);
						TICKER_INTERVAL = 1000*Integer.parseInt(args[i]);
						break;
					case "-gpp":
						i++;
						System.out.println("GraphsPerPage = " + args[i]);
						MAXPERPAGE = Short.parseShort(args[i]);
						break;
					case "-notify":
						i++;
						System.out.println("notifyScript = " + args[i]);
						notifyScript = args[i];
						break;
					case "-url":
						i++;
						System.out.println("webUrl = " + args[i]);
						webUrl = args[i];
						customUrl = true;
						break;
					default:
						System.out.println(
								"-help	: This output\n"
								+"-wp <webPort>          : Web Interface Port (default: 9876)\n"
								+"-lp <listenerPort>    : UDP listener Port (default: 9876)\n"
								+"-url <webUrl>         : Web Url (for links in notifications) (default: host:port)\n"
								+"-wt <webThreads>      : No Idea, probably unused\n"
								+"-ct <cacheTime>       : How long (seconds) to cache generated page (default: 120)\n"
								+"-ci <cacheItems>      : How many pages to store in cache (default: 50)\n"
								+"-al <alarmLife>       : How long (seconds) an alarm state remains (default: 86400)\n"
								+"-as <alarmSamples>    : Minimum samples before considering for alarm (default: 20)\n"
								+"-atl <lowAlarmThreshold>  : Low alarm threshold multiplier (how many times below mean value is alarm) (default: 10)\n"
								+"-ath <highAlarmThreshold>  : High alarm threshold multiplier (how many times above mean value is alarm) (default: 6)\n"
								+"-acs <alarmConsecutiveSamples>    : How many consecutive samples above threshold trigger alarm state (default: 3)\n"
								+"-notify <notifyScript>            : Script to use for alarms (default: /tmp/otinanai_notifier)\n"
								+"-gpp <graphsPerPage>  : Max graphs per page (default: 30)\n"
								+"-tick <tickInterval>  : Every how often (seconds) does the ticker run (add new samples, aggregate old) (default: 60)\n"
								+"-s1hours <step1Hours>      	    : Time for aggregation. Overrides old s1samples setting.(default: 28)\n"
								+"-s1agg <step1SamplesToAggregate>  : Samples to aggregate when sample count exceeded (default: 10)\n"
								+"-s2hours <step2Hours>      	    : Time for further aggregation. Overrides old s2samples setting.(default: 340)\n"
								+"-s2agg <step2SamplesToAggregate>  : Samples to further aggregate when count exceeded (default: 6)\n"
								+"-lf <logFile>         : \n"
								+"-ll <logLevel>        : finest, fine, info, config, warning, severe (default: config)\n"
								+"-rh <redisEndPoint>   : Redis endpoint (default: localhost)\n"
								+"-rdkwlist <redisKeyWordListName>  : Name of keyword list, useful for more than one instance running on the same redis. (default: existing_keywords_list)\n"
								+"-rdsvq <redisSavedQueriesList>    : Name of saved queries list for redis. (default: saved_queries_list)\n"
								+"-rdevtlist <redisEventList>    : Name of event list for redis. (default: OtiNanai_Event_List)\n"
								+"-rdunitlist <redisUnitList>    : Name of event list for redis. (default: OtiNanai_Unit_List)\n"
								+"-rdmultiplist <redisMultiplierList>    : Name of event list for redis. (default: OtiNanai_Multiplier_List)\n"
								);
						System.exit(0);
						break;
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			System.exit(1);
		}

		if (!customUrl) {
			webUrl = webUrl + ":" + webPort;
		}

		WEBURL = webUrl;
		NOTIFYSCRIPT = notifyScript;
		ALARMLIFE = alarmLife;

		if (step1HoursOverride) {
			STEP1_MAX_SAMPLES = (int)((step1Hours * 3600000) / TICKER_INTERVAL);
		}
		if (step2HoursOverride) {
			STEP2_MAX_SAMPLES = (int)((step2Hours * 3600000) / TICKER_INTERVAL);
		}
		STEP1_MILLISECONDS = STEP1_MAX_SAMPLES * TICKER_INTERVAL;
		STEP2_MILLISECONDS = STEP2_MAX_SAMPLES * TICKER_INTERVAL;

		OtiNanai non = new OtiNanai(udpPort, listenerThreads, webPort, webThreads, cacheTime, cacheItems, alarmSamples, lowAlarmThreshold, highAlarmThreshold, alarmConsecutiveSamples, logFile, logLevel, bucketName, redisHost, redisKeyWordList, redisSavedQueries, redisEventList, redisUnitList, redisMultipList);
	}

	/**
	 * Simple Log Formatter
	 */
	private class MyLogFormatter extends java.util.logging.Formatter {
		public String format(LogRecord rec) {
			StringBuffer buf = new StringBuffer(1024);
			buf.append(calcDate(rec.getMillis()));
			buf.append(" ["+rec.getLevel()+"]:");
			buf.append(formatMessage(rec));
			buf.append('\n');
			return buf.toString();
		}

		private String calcDate(long millisecs) {
			SimpleDateFormat date_format = new SimpleDateFormat("MMM dd HH:mm:ss");
			Date resultdate = new Date(millisecs);
			return date_format.format(resultdate);
		}

		public String getHead(Handler h) {
			return "OtiNanai Logger Initiated : " + (new Date()) + "\n";
		}
		public String getTail(Handler h) {
			return "OtiNanai Logger Exiting : " + (new Date()) + "\n";
		}
	}

	private Logger logger;

	//public static final int STEP1_MAX_SAMPLES = 20;
	public static int STEP1_MAX_SAMPLES = 1440;
	public static int STEP1_SAMPLES_TO_MERGE = 10;
	public static long STEP1_MILLISECONDS = 100800000l;
	public static int STEP2_MAX_SAMPLES = 2880;
	public static int STEP2_SAMPLES_TO_MERGE = 6;
	public static long STEP2_MILLISECONDS = 864000000l;
	public static int TICKER_INTERVAL = 60000;
	public static long PREVIEWTIME = 21600000l;
	public static short MAXPERPAGE=30;

	//public static final int MAXSAMPLES = 20;
	//public static int MAX_LOG_OUTPUT=20;

	public static String WEBURL;
	public static String NOTIFYSCRIPT;
	public static long ALARMLIFE;

	public static final short UNSET = 0;
	public static final short GAUGE = 1;
	public static final short COUNTER = 2;
	public static final short FREQ = 3;
	public static final short SUM = 4;

	public static final short GRAPH_FULL=1;
	public static final short GRAPH_PREVIEW=2;
	public static final short GRAPH_MERGED=3;
	public static final short GRAPH_GAUGE=4;
	public static final short GRAPH_STACKED=5;

	public static final short HEADER = 1;
	public static final short ENDHEAD = 2;
	public static final short ENDBODY = 3;
	public static final short FLOT = 4;
	public static final short FLOT_MERGED = 5;
	public static final short FLOT_PREVIEW = 6;
	public static final short FLOT_STACKED = 7;
	public static final short JS = 8;
	public static final short ENDJS = 9;
	public static final short GAGE = 10;
	public static final short REFRESH = 11;

	public static final short MAX_KW_LENGTH = 24;

	public static final short NADA = 0;
	public static final short KILO = 1;
	public static final short MEGA = 2;
	public static final short GIGA = 3;
	public static final short PETA = 4;
        public static final int MAXMERGECOUNT = 3;
}
