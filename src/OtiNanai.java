package gr.phaistosnetworks.admin.otinanai;

import java.net.ServerSocket;
import java.net.DatagramSocket;
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
	public OtiNanai(int listenerPort, int listenerThreads, int webPort, int webThreads, long cacheTime, int cacheItems, long alarmLife, int alarmSamples, float alarmThreshold, String logFile, String logLevel, short storageEngine, String bucketName, String riakHost, int riakPort){
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
			logger.config("[Init]: alarmLife: "+alarmLife + "ms");
			logger.config("[Init]: alarmSamples: "+alarmSamples);
			logger.config("[Init]: alarmThreshold: "+alarmThreshold);
			logger.config("[Init]: logFile: "+logFile);
			logger.config("[Init]: logLevel: "+logLevel);
			logger.config("[Init]: storageEngine: "+storageEngine);
			logger.config("[Init]: bucketName: "+bucketName);
			logger.config("[Init]: riakHost: "+riakHost);
			logger.config("[Init]: riakPort: "+riakPort);

			DatagramSocket ds = new DatagramSocket(listenerPort);
			OtiNanaiListener onl = new OtiNanaiListener(ds, alarmLife, alarmSamples, alarmThreshold, logger, storageEngine, bucketName, riakHost, riakPort);
			new Thread(onl).start();

         // Ticker
         logger.config("[Init]: Setting up ticker");
         OtiNanaiTicker ont = new OtiNanaiTicker(onl, logger);
         new Thread(ont).start();

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
      short storageEngine = OtiNanai.MEM;
      Long cacheTime = 120000L;
      Long alarmLife = 86400000L;
      int alarmSamples = 20;
      float alarmThreshold = 3.0f;
      int cacheItems = 50; 
      String bucketName = new String("OtiNanai");
      String logFile = new String("/var/log/otinanai.log");
      String logLevel = new String("INFO");
      String riakHost = new String("localhost");
      int riakPort = 8087;
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
					case "-at":
						i++;
						alarmThreshold = Float.parseFloat(args[i]);
						System.out.println("alarmThreshold = " + alarmThreshold);
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
					case "-riak":
						System.out.println("storageEngine = Riak");
                  storageEngine = OtiNanai.RIAK;
						break;
               case "-bn":
                  i++;
                  System.out.println("Bucket Name = " + args[i]);
                  bucketName = args[i];
                  break;
               case "-rh":
                  i++;
                  System.out.println("riak host = " + args[i]);
                  riakHost = args[i];
                  break;
               case "-rp":
                  i++;
                  System.out.println("riak port = " + args[i]);
                  riakPort = Integer.parseInt(args[i]);
                  break;
					default:
						System.out.println("-wp <webPort> -lp <listenerPort> -wt <webThreads> -ct <cacheTime (s)> -ci <cacheItems> -al <alarmLife (s)> -as <alarmSamples> -at <alarmThreshold> -lf <logFile> -ll <logLevel> -riak -bn <bucketName> -rh <riakEndPoint> -rp <riakPort>");
                  System.exit(0);
						break;
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			System.exit(1);
		}
		OtiNanai non = new OtiNanai(udpPort, listenerThreads, webPort, webThreads, cacheTime, cacheItems, alarmLife, alarmSamples, alarmThreshold, logFile, logLevel, storageEngine, bucketName, riakHost, riakPort);
	}

	/**
	 * Simple Log Formatter
	 */
	private class MyLogFormatter extends java.util.logging.Formatter {
		public String format(LogRecord rec) {
			StringBuffer buf = new StringBuffer(1024);
			buf.append(calcDate(rec.getMillis()));
			buf.append(" ");
			buf.append("["+rec.getLevel()+"]");
			buf.append(" : ");
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

	public static final int THIRTY_SEC_SAMPLES = 300;
	public static final int THIRTY_S_TO_FIVE_M = 10;
	public static final int FIVE_MIN_SAMPLES = 2880;
	public static final int FIVE_M_TO_THIRTY_M = 6;

	public static final int MAXSAMPLES = 20;
	public static final short GAUGE = 0;
	public static final short COUNTER = 1;
	public static final short FREQ = 2;
	public static final short MEM = 1;
	public static final short RIAK = 2;

	public static int MAX_LOG_OUTPUT=20;
	public static short GRAPH_FULL=1;
	public static short GRAPH_PREVIEW=2;
   public static short GRAPH_MERGED=3;
   public static short GRAPH_MERGED_AXES=4;

	public static short MAXPERPAGE=15;

	public static final int TICKER_INTERVAL = 30000;

   public static final short HEADER = 1;
   public static final short ENDHEAD = 2;
   public static final short ENDBODY = 3;
   public static final short GOOGLE = 4;
   public static final short FLOT = 5;
   public static final short JS = 6;
   public static final short ENDJS = 7;

   public static final long PREVIEWTIME = 3600000l;

}
