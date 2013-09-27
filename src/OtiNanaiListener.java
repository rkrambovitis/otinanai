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


class OtiNanaiListener implements Runnable {

	/**
	 * Multithread constructor
	 * @param	ds	the DatagraSocket to be used
    * @param   al Time diff for alarm to still be active
    * @param   ps Number of samples to keep for preview graphs
	 * @param	l	the logger to log to
	 */
	public OtiNanaiListener(DatagramSocket ds, long al, int as, float at, Logger l, short st, String bucketName, String riakHost, int riakPort) {
		logger = l;
      alarmLife = al;
      alarmSamples = as;
      alarmThreshold = at;
      storageType = st;
      riakBucket = null;
      keyWordRiakString = new String("KeyWords_keyword");

      if (st == OtiNanai.RIAK) {
         try {
            logger.config("[Listener]: Setting up Riak");

            IRiakClient riakClient = RiakFactory.pbcClient(riakHost, riakPort); //or RiakFactory.httpClient();                   
            riakBucket = riakClient.createBucket(bucketName).nVal(1).r(1).disableSearch().lastWriteWins(true).backend("eleveldb").execute();
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
         } catch (RiakException re) {
            logger.severe("[Listener]: "+re);
            System.exit(1);
         }
      }
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
         if (memoryMap.containsKey(kw)) {
				logger.finest("[Listener]: Existing keyword detected. Adding to list : " + kw);
            if (newRecord.isGauge()) {
               memoryMap.get(kw).put(newRecord.getGauge());
            } else if (newRecord.isCounter()) {
               memoryMap.get(kw).put(newRecord.getCounter());
            } else {
               memoryMap.get(kw).put();
            }
			} else {
				logger.info("[Listener]: Keyword not detected. Creating new list : " + kw);
				memoryMap.put(kw, new OtiNanaiMemory(kw, alarmLife, alarmSamples, alarmThreshold, logger, recType,  newRecord.getGauge(), newRecord.getCounter(), storageType, riakBucket));
			}
		}
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
      tempKW.addAll(memoryMap.keySet());
      for (String kw : tempKW) {
         memoryMap.get(kw).tick(now);
      }
   }

	private HashMap<String,OtiNanaiMemory> memoryMap; 
	private int port;
   private long alarmLife;
   private int alarmSamples;
   private float alarmThreshold;
	private DatagramSocket dataSocket;
	private Logger logger;
   private short storageType;
   private Bucket riakBucket;
   private String keyWordRiakString;
}
