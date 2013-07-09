import java.net.InetAddress;
import java.util.*;
import java.lang.*;
import java.text.SimpleDateFormat;

class SomeRecord {

	public SomeRecord(InetAddress ip, String data) {
		timeStamp = System.currentTimeMillis();
		theDate = calcDate(timeStamp);
		timeNano = Long.toString(System.nanoTime());
		myip = ip;
		theRecord = data;
		keyWords = new ArrayList<String>();
		findKeyWords(data.replaceAll("[\r\n]",""));
	}

	private void findKeyWords(String str) {
		String[] Tokens = str.split("[ \t]");
		//for (String tok : Tokens ) {
		metrics = new ArrayList<Integer>();
		for (int i=0; i<Tokens.length; i++ ) {
			String tok = Tokens[i];
			try {
				Float.parseFloat(tok);
				metrics.add(new Integer(i));
			} catch (NumberFormatException e) {
				String[] subTokens = tok.split("[.:]");
				for (String subTok : subTokens ) {
					if (subTok.length() >= 2 ) {
						keyWords.add(subTok);
		//				System.out.println(subTok);
					}
				}
			}
		}
		Tokens = str.split("\\s");
		masterKey = Tokens[0];
	}

	private String calcDate(long millisecs) {
		SimpleDateFormat date_format = new SimpleDateFormat("MM/dd/YY HH:mm:ss");
		Date resultdate = new Date(millisecs);
		return date_format.format(resultdate);
	}

	public String getDate() {
		return theDate;
	}

	public boolean isMetric(Integer c) {
		return metrics.contains(c);
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public String getTimeNano() {
		return timeNano;
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

	public String getRecord(int m) {
		String toks[] = theRecord.split("\\s");
		if (m < toks.length || m < 0) 
			return toks[m];
		return null;	
	}

	public boolean hasKeyword(String test) {
		return keyWords.contains(test);
	}

	public ArrayList<String> getKeyWords() {
		return keyWords;
	}

	public boolean containsWord(String test) {
		return theRecord.toLowerCase().contains(test.toLowerCase());
	}

	public String getKey() {
		return masterKey;
	}

	private ArrayList<Integer> metrics;
	private long timeStamp;
	private String timeNano;
	private InetAddress myip;
	private String theRecord;
	private String theDate;
	private ArrayList<String> keyWords;
	private String masterKey;
}
