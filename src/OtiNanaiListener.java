package gr.phaistosnetworks.admin.otinanai;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.*;


import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.logging.*;


class OtiNanaiListener implements Runnable {

	/**
	 * Multithread constructor
	 * @param	ds	the DatagraSocket to be used
    * @param   al Time diff for alarm to still be active
    * @param   ps Number of samples to keep for preview graphs
	 * @param	l	the logger to log to
	 */
	public OtiNanaiListener(DatagramSocket ds, long al, int as, float at, int ps, Logger l, short st) {
		logger = l;
      alarmLife = al;
      alarmSamples = as;
      alarmThreshold = at;
      previewSamples = ps;
      storageType = st;
      riakBucket = null;
      keyWords = new LLString();
      keyWordRiakString = new String("KeyWords_keyword");

      if (st == OtiNanai.RIAK) {
         try {
            logger.config("[Listener]: Setting up Riak");

            IRiakClient riakClient = RiakFactory.pbcClient(); //or RiakFactory.httpClient();                   
            riakBucket = riakClient.createBucket("OtiNanai").execute();
/*
            logger.fine("[Listener]: fetching existing keyWords List");
            keyWords = riakBucket.fetch(keyWordRiakString, LLString.class).execute();
            if (keyWords == null) {
               logger.fine("[Listener]: null. Creating new keyWords List");
               keyWords = new LLString();
            }
            */
         } catch (RiakException re) {
            logger.severe("[Listener]: "+re);
            System.exit(1);
         }
      }
		keyMaps = new HashMap<String,ArrayList<String>>(200);
		storageMap = new HashMap<String,SomeRecord>(5000);
		memoryMap = new HashMap<String, OtiNanaiMemory>(200);

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
         
         if (newRecord.isMetric()) {
            kw = kw + "_";
            recType = OtiNanai.GAUGE;
         }
         
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
            if (newRecord.isMetric()) {
               memoryMap.get(kw).put(newRecord.getMetric());
            } else {
               memoryMap.get(kw).put();
            }
            if (keyMaps.get(kw).size() >= OtiNanai.MAXSAMPLES) {
               String uid = keyMaps.get(kw).get(0);
               keyMaps.get(kw).remove(uid);
               storageMap.remove(uid);
            }
			} else {
				logger.info("[Listener]: Keyword not detected. Creating new list : " + kw);
				ArrayList<String> nanoList = new ArrayList<String>();
				nanoList.add(newRecord.getTimeNano());
				keyMaps.put(kw, nanoList);
            keyWords.add(kw);
				memoryMap.put(kw, new OtiNanaiMemory(kw, alarmLife, alarmSamples, alarmThreshold, logger, recType, previewSamples, newRecord.getMetric(), storageType, riakBucket));
            try {
               riakBucket.store(keyWordRiakString, keyWords).execute();
            } catch (RiakRetryFailedException rrfe) {
               logger.severe("[Lisener]: Failed to store keyWords List to Riak");
            }
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
      LLString tempKW = new LLString();
      tempKW.addAll(keyWords);
      for (String kw : tempKW) {
         memoryMap.get(kw).tick(now);
      }
   }

   private LLString keyWords;
	private HashMap<String,SomeRecord> storageMap;
	private HashMap<String,ArrayList<String>> keyMaps;
	private HashMap<String,OtiNanaiMemory> memoryMap; 
	private int port;
   private int previewSamples;
   private long alarmLife;
   private int alarmSamples;
   private float alarmThreshold;
	private DatagramSocket dataSocket;
	private Logger logger;
   private short storageType;
   private Bucket riakBucket;
   private String keyWordRiakString;
}
