import java.net.InetAddress;
import java.util.*;

class SomeRecord {

	public SomeRecord(InetAddress ip, String data) {
		timeStamp = System.currentTimeMillis();
		myip = ip;
		theRecord = data;
		keyWords = new ArrayList<String>();
		findKeyWords(data);
	}

	private void findKeyWords(String str) {
		String[] Tokens = str.split("[ .\t\n]");
		for (String tok : Tokens ) {
			if (tok.length() >= 4 ) {
				//System.out.println(tok);
				keyWords.add(tok);
			}
		}
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public InetAddress getIP() {
		return myip;
	}

	public String getHostName() {
		return myip.getHostName();
	}

	public String getRecord() {
		return theRecord;
	}

	public boolean hasKeyword(String test) {
		return keyWords.contains(test);
	}

	public boolean containsWord(String test) {
		return theRecord.toLowerCase().contains(test.toLowerCase());
	}

	private long timeStamp;
	private InetAddress myip;
	private String theRecord;
	private ArrayList<String> keyWords;
}
