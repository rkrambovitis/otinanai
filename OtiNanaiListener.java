import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;                                                                                                                         


class OtiNanaiListener implements Runnable {
	public OtiNanaiListener(int lp) {
		storage = new CopyOnWriteArrayList<SomeRecord>();
		keyMaps = new HashMap<String,ArrayList<String>>();
		storageMap = new HashMap<String,SomeRecord>();
		keyTrackerMap = new HashMap<String, KeyWordTracker>();
		port = lp;
	}

	public void run() {
		try {
			DatagramSocket serverSocket = new DatagramSocket(port);
			while(true) {
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				try {
					serverSocket.receive(receivePacket);
				} catch (IOException ioer) {
					System.out.println(ioer);
					continue;
				}
				String sentence = new String(receivePacket.getData());
				InetAddress IPAddress = receivePacket.getAddress();
				parseData(IPAddress, sentence);
			}
		} catch (SocketException socker) {
			System.out.println(socker);
			System.exit(0);
		}
	}
	
	private void parseData(InetAddress hip, String theDato) {
		SomeRecord newRecord = new SomeRecord(hip, theDato);
		ArrayList<String> theKeys = newRecord.getKeyWords();
		for (String kw : theKeys) {
			//System.out.println(kw);
			if (kw.equals("")) {
				continue;
			} else if (keyMaps.containsKey(kw)) {
				keyMaps.get(kw).add(newRecord.getTimeNano());
				keyTrackerMap.get(kw).put(newRecord.getTimeStamp());
			} else {
				ArrayList<String> alBundy = new ArrayList<String>();
				alBundy.add(newRecord.getTimeNano());
				keyMaps.put(kw, alBundy);
				keyTrackerMap.put(kw, new KeyWordTracker(kw));
			}
		}
		storageMap.put(newRecord.getTimeNano(), newRecord);
		storage.add(newRecord);
	}

	public CopyOnWriteArrayList<SomeRecord> getData() {
		return storage;
	}

	public HashMap<String,SomeRecord> getDataMap() {
		return storageMap;
	}

	public HashMap<String,ArrayList<String>> getKeyMaps() {
		return keyMaps;
	}

	public HashMap<String,KeyWordTracker> getKeyTrackerMap() {
		return keyTrackerMap;
	}

	private CopyOnWriteArrayList<SomeRecord> storage;
	private HashMap<String,SomeRecord> storageMap;
	private HashMap<String,ArrayList<String>> keyMaps;
	private HashMap<String,KeyWordTracker> keyTrackerMap; 
	private int port;
}
