import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.logging.*;


class OtiNanaiListener implements Runnable {

	/**
	 * Primary constructor, for singular listener.
	 * @param	lp	the port to listen on
	 * @param	l	the logger to log to
	 */
	public OtiNanaiListener(int lp, Logger l) throws SocketException {
		logger = l;
		storage = new CopyOnWriteArrayList<SomeRecord>();
		keyMaps = new HashMap<String,ArrayList<String>>();
		storageMap = new HashMap<String,SomeRecord>();
		keyTrackerMap = new HashMap<String, KeyWordTracker>();
		port = lp;
		dataSocket = new DatagramSocket(lp);
		logger.finest("New OtiNanaiListener Initialized");
	}

	/**
	 * Multithread constructor
	 * @param	ds	the DatagraSocket to be used
	 * @param	l	the logger to log to
	 */
	public OtiNanaiListener(DatagramSocket ds, Logger l) {
		logger = l;
		storage = new CopyOnWriteArrayList<SomeRecord>();
		keyMaps = new HashMap<String,ArrayList<String>>();
		storageMap = new HashMap<String,SomeRecord>();
		keyTrackerMap = new HashMap<String, KeyWordTracker>();
		dataSocket = ds;
		logger.finest("New OtiNanaiListener Initialized");
	}

	/**
	 * necessary threaded run method.
	 * infinite loop.
	 * Tries to receive data, and then sends it to parseData()
	 */
	public void run() {
		while(true) {
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				dataSocket.receive(receivePacket);
			} catch (IOException ioer) {
				System.out.println(ioer);
				logger.severe(ioer.getMessage());
				continue;
			}
			String sentence = new String(receivePacket.getData());
			InetAddress IPAddress = receivePacket.getAddress();
			logger.fine("Listener received message from "+IPAddress);
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
		logger.finest("+ Attempting to parse: \""+theDato+"\" from "+hip);
		SomeRecord newRecord = new SomeRecord(hip, theDato);
		ArrayList<String> theKeys = newRecord.getKeyWords();
		for (String kw : theKeys) {
			//System.out.println(kw);
			if (kw.equals("")) {
				logger.finest("Blank Keyword, ignored");
				continue;
			} else if (keyMaps.containsKey(kw)) {
				logger.fine("Existing keyword detected. Adding to list : " + kw);
				keyMaps.get(kw).add(newRecord.getTimeNano());
				keyTrackerMap.get(kw).put(newRecord.getTimeStamp());
			} else {
				logger.fine("Keyword not detected. Creating new list : " + kw);
				ArrayList<String> alBundy = new ArrayList<String>();
				alBundy.add(newRecord.getTimeNano());
				keyMaps.put(kw, alBundy);
				keyTrackerMap.put(kw, new KeyWordTracker(kw));
			}
		}
		logger.fine("Storing to storage and storageMap");
		storageMap.put(newRecord.getTimeNano(), newRecord);
		storage.add(newRecord);
	}

	/**
	 * Access Method
	 */
	public CopyOnWriteArrayList<SomeRecord> getData() {
		return storage;
	}

	/**
	 * Access Method
	 */
	public HashMap<String,SomeRecord> getDataMap() {
		return storageMap;
	}

	/**
	 * Access Method
	 */
	public HashMap<String,ArrayList<String>> getKeyMaps() {
		return keyMaps;
	}

	/**
	 * Access Method
	 */
	public HashMap<String,KeyWordTracker> getKeyTrackerMap() {
		return keyTrackerMap;
	}
	
	/**
	 * Access Method
	 */
	public Logger getLogger() {
		return logger;
	}

	private CopyOnWriteArrayList<SomeRecord> storage;
	private HashMap<String,SomeRecord> storageMap;
	private HashMap<String,ArrayList<String>> keyMaps;
	private HashMap<String,KeyWordTracker> keyTrackerMap; 
	private int port;
	private DatagramSocket dataSocket;
	private Logger logger;
}
