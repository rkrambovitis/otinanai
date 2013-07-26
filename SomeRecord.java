import java.net.InetAddress;
import java.util.*;
import java.lang.*;
import java.text.SimpleDateFormat;

/**
 * Data Storage Class
 * Keeps self data, keywords, timestamp, ip of host and has access methods
 */
class SomeRecord {

	/**
	 * Constructor.
	 * @param	ip	IPAddress
	 * @param	data	arbitrary data as a string
	 */
	public SomeRecord(InetAddress ip, String data) {
		timeStamp = System.currentTimeMillis();
		theDate = calcDate(timeStamp);
		timeNano = Long.toString(System.nanoTime());
		myip = ip;
		theRecord = data;
		keyWords = new ArrayList<String>();
		findKeyWords(data.replaceAll("[\r\n]",""));
	}

	/**
	 * Method to break down the string into keywords.
	 * Probably worth overloading to send minimum keyword length rather than hard code it
	 * @param	str	the data to be broken down
	 */
	private void findKeyWords(String str) {
      str = str.toLowerCase();
		String[] Tokens = str.split("[ \t]");
		//for (String tok : Tokens ) {
		metrics = new ArrayList<Integer>();
      int i=0;
      boolean indexAll = false;
      if (Tokens[i].equals("index")) {
         indexAll = true;
         i++;
      }
		for (; i<Tokens.length; i++ ) {
			String tok = Tokens[i];
			try {
				Float.parseFloat(tok);
				metrics.add(new Integer(i));
			} catch (NumberFormatException e) {
            /*
				String[] subTokens = tok.split("[.]");
				for (String subTok : subTokens ) {
					if (subTok.length() >= 2 ) {
						keyWords.add(subTok);
		//				System.out.println(subTok);
					}
				}
            */
            if ( tok.length() >= 3 && tok.length() < 48 ) {
               keyWords.add(tok);
               if (!indexAll)
                  break;
            }
			}
		}
		Tokens = str.split("\\s");
		masterKey = Tokens[0];
	}

	/**
	 * Changes milliseconds into date with format: MM/dd/YY HH:mm:ss
	 * @param	millisecs the millisecs to change
	 * @return	String containing the date.
	 */
	private String calcDate(long millisecs) {
		SimpleDateFormat date_format = new SimpleDateFormat("MM/dd/YY HH:mm:ss");
		Date resultdate = new Date(millisecs);
		return date_format.format(resultdate);
	}

	/**
	 * Access Method
	 * @return the date of the record in format: MM/dd/YY HH:mm:ss
	 */
	public String getDate() {
		return theDate;
	}

	/**
	 * Returns if the nth word is a metric (float)
	 * @param	n	the word number to test.
	 */
	public boolean isMetric(Integer n) {
		return metrics.contains(n);
	}

	/**
	 * Access Method
	 * @return timestamp (millisecond) of record.
	 */
	public long getTimeStamp() {
		return timeStamp;
	}

	/**
	 * Access Method
	 * @return nanosecond time of record. Used as unique id
	 */
	public String getTimeNano() {
		return timeNano;
	}

	/**
	 * Access Method
	 * @return record source ip
	 */
	public InetAddress getIP() {
		return myip;
	}

	/**
	 * Access Method
	 * @return host name lookup of the record source ip
	 */
	public String getHostName() {
		return myip.getHostName();
	}

	/**
	 * Access Method
	 * @return	the whole data
	 */
	public String getRecord() {
		return theRecord;
	}

	/**
	 * Access Method
	 * @return	the nth word from the data or null.
	 */
	public String getRecord(int m) {
		String toks[] = theRecord.split("\\s");
		if (m < toks.length || m < 0) 
			return toks[m];
		return null;	
	}

	/**
	 * A check if the word exists in the list of detected keywords
	 * @param test	the string to test
	 */
	public boolean hasKeyword(String test) {
		return keyWords.contains(test);
	}

	/**
	 * Access Method
	 * @return 	ArrayList containing the keyword for this
	 */
	public ArrayList<String> getKeyWords() {
		return keyWords;
	}

	/**
	 * A check if the word is contained in the dato.
	 * This will be slower than hasKeyword, but does not require a precise match
	 * @param test	the string to test
	 */
	public boolean containsWord(String test) {
		return theRecord.toLowerCase().contains(test.toLowerCase());
	}

	/**
	 * This will most likely not be used
	 * @return the first word of data (could be useful is all data is org.gnome.desktop.wm.raiseOnFocus 0)
	 */
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
