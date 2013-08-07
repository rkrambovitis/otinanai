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
	public OtiNanaiListener(int lp, long al, Logger l) throws SocketException {
		logger = l;
      alarmLife = al;
		keyMaps = new HashMap<String,ArrayList<String>>();
		storageMap = new HashMap<String,SomeRecord>();
		memoryMap = new HashMap<String, OtiNanaiMemory>();
      keyWords = new ArrayList<String>();
		port = lp;
		dataSocket = new DatagramSocket(lp);
		logger.finest("[Listener]: New OtiNanaiListener Initialized");
	}

	/**
	 * Multithread constructor
	 * @param	ds	the DatagraSocket to be used
	 * @param	l	the logger to log to
	 */
	public OtiNanaiListener(DatagramSocket ds, long al, Logger l) {
		logger = l;
      alarmLife = al;
		keyMaps = new HashMap<String,ArrayList<String>>();
		storageMap = new HashMap<String,SomeRecord>();
		memoryMap = new HashMap<String, OtiNanaiMemory>();
      keyWords = new ArrayList<String>();
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
				logger.severe("[Listener]: "+ioer.getMessage());
				continue;
			}
			String sentence = new String(receivePacket.getData());
			InetAddress IPAddress = receivePacket.getAddress();
			logger.fine("[Listener]: Listener received message from "+IPAddress);
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
		ArrayList<String> theKeys = newRecord.getKeyWords();
		for (String kw : theKeys) {
         //kw = kw.replaceAll("[+=@#$%^&*;:()-_\\/|'\"]", "");
         kw = kw.replaceAll("[-#'$+=!@$%^&*()|'\\/\":,?<>{};]", "");
         try { 
            Float.parseFloat(kw);
            logger.finest("[Listener]: Number, ignored");
            continue;
         } catch (NumberFormatException e) {}
			if (kw.equals("")) {
				logger.finest("[Listener]: Blank Keyword, ignored");
				continue;
			} else if (keyMaps.containsKey(kw)) {
				logger.finest("[Listener]: Existing keyword detected. Adding to list : " + kw);
				keyMaps.get(kw).add(newRecord.getTimeNano());
				memoryMap.get(kw).put(newRecord.getHostName());
            if (keyMaps.get(kw).size() >= MAXSAMPLES) {
               String uid = keyMaps.get(kw).get(0);
               keyMaps.get(kw).remove(uid);
               storageMap.remove(uid);
            }
			} else {
				logger.info("[Listener]: Keyword not detected. Creating new list : " + kw);
				ArrayList<String> alBundy = new ArrayList<String>();
				alBundy.add(newRecord.getTimeNano());
				keyMaps.put(kw, alBundy);
            keyWords.add(kw);
				memoryMap.put(kw, new OtiNanaiMemory(kw, alarmLife, logger));
			}
		}
		logger.finest("[Listener]: Storing to storageMap");
		storageMap.put(newRecord.getTimeNano(), newRecord);
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
	public HashMap<String,OtiNanaiMemory> getMemoryMap() {
		return memoryMap;
	}
	
	/**
	 * Access Method
	 */
	public Logger getLogger() {
		return logger;
	}

   public void tick() {
      long now=System.currentTimeMillis();
      for (String kw : keyWords) {
         memoryMap.get(kw).tick(now);
      }
   }

   private ArrayList<String> keyWords;
	private HashMap<String,SomeRecord> storageMap;
	private HashMap<String,ArrayList<String>> keyMaps;
	private HashMap<String,OtiNanaiMemory> memoryMap; 
	private int port;
   private long alarmLife;
	private DatagramSocket dataSocket;
	private Logger logger;
   private static final int MAXSAMPLES = 20;
}
