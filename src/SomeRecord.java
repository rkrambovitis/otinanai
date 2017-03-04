package gr.phaistosnetworks.admin.otinanai;

import java.net.InetAddress;
import java.util.*;
import java.lang.*;
import java.text.SimpleDateFormat;
import java.net.URLEncoder;


/**
 * Data Storage Class
 * Keeps self data, keywords, timestamp, ip of host and has access methods
 */
class SomeRecord {

  /**
   * Constructor.
   * @param  ip  IPAddress
   * @param  data  arbitrary data as a string
   */
  public SomeRecord(InetAddress ip, String data) {
    timeStamp = System.currentTimeMillis();
    theDate = calcDate(timeStamp);
    timeNano = Long.toString(System.nanoTime());
    myip = ip;
    theRecord = data;
    keyWords = new ArrayList<String>();
    IAmGauge = false;
    IAmCounter = false;
    IAmEvent = false;
    theValue = 0F;
    theCounter = 0L;
    process(data.replaceAll("[\r\n]",""), 3, 128);
  }

  private void process(String str, int min, int max) {
    try {
      theEvent = URLEncoder.encode(str.replaceAll("eventmarker ", "").replaceAll(",",",&#8203;"), "UTF-8");
    } catch (Exception e) {
      theEvent = str.replaceAll("eventmarker ", "").replaceAll("[#'$+=!@$%^&*()|'\\/\":,?<>{};\\[\\]]", "");
    }
    str = str.replaceAll("[#'$+=!@$%^&*()|'\\/\":,?<>{};\\[\\]]", "");
    str = str.toLowerCase();
    storeMetric(str, min, max);
    if (!IAmGauge && !IAmCounter && !IAmSum) {
      findKeyWords(str, min, max);
    }
  }


  /**
   * Method to break down the string into keywords.
   * @param   min   the minimum length of a word to be considered as a keyword
   * @param   max   the maximum ...
   * @param  str  the data to be broken down
   */
  private void storeMetric(String str, int min, int max) {
    String[] tokens = str.split("[ \t]");
    if (tokens.length == 2 && isKeyWord(tokens[0], min, max)) {
      //System.out.println("Record: Gauge");
      Float w2 = toFloat(tokens[1]);
      if (w2 != null) {
        theValue = w2;
        IAmGauge = true;
        keyWords.add(tokens[0]);
      }
    } else if (tokens.length == 3 && isKeyWord(tokens[0], min, max) && tokens[1].equals("counter")) {
      //System.out.println("Record: Counter");
      Long w3 = toLong(tokens[2]);
      if (w3 != null) {
        theCounter = w3;
        IAmCounter = true;
        keyWords.add(tokens[0]);
      }
    } else if (tokens.length == 3 && isKeyWord(tokens[0], min, max) && tokens[2].equals("counter")) {
      Long w2 = toLong(tokens[1]);
      if (w2 != null) {
        theCounter = w2;
        IAmCounter = true;
        keyWords.add(tokens[0]);
      }
    } else if (tokens.length == 3 && isKeyWord(tokens[0], min, max) && tokens[1].equals("sum")) {
      Float w3 = toFloat(tokens[2]);
      if (w3 != null) {
        theValue = w3;
        IAmSum = true;
        keyWords.add(tokens[0]);
      }
    } else if (tokens.length == 3 && isKeyWord(tokens[0], min, max) && tokens[2].equals("sum")) {
      Float w2 = toFloat(tokens[1]);
      if (w2 != null) {
        theValue = w2;
        IAmSum = true;
        keyWords.add(tokens[0]);
      }
    } else if (tokens.length == 3 && isKeyWord(tokens[0], min, max) && tokens[1].equals("hist")) {
      Float w3 = toFloat(tokens[2]);
      if (w3 != null) {
        theValue = w3;
        IAmHistogram = true;
        keyWords.add(tokens[0]);
      }
    } else if (tokens.length == 3 && isKeyWord(tokens[0], min, max) && tokens[2].equals("hist")) {
      Float w2 = toFloat(tokens[1]);
      if (w2 != null) {
        theValue = w2;
        IAmHistogram = true;
        keyWords.add(tokens[0]);
      }
    } else if (tokens[tokens.length-1].equals("eventmarker") || tokens[0].equals("eventmarker")) {
      Long w2 = toLong(tokens[1]);
      if (w2 != null && w2 > 1000000000) {
        str=str.replaceAll(tokens[1]+" ", "");
        timeStamp = (1000l*w2);
      }
      IAmEvent = true;
    } 
  }

  /**
   * Method to break down the string into keywords.
   * @param   min   the minimum length of a word to be considered as a keyword
   * @param   max   the maximum ...
   * @param  str  the data to be broken down
   */
  private void findKeyWords(String str, int min, int max) {
    String[] tokens = str.split("[ \t]");
    int i=0;
    boolean indexAll = false;
    String genKW = new String();
    if (tokens[i].equals("index_all_words")) {
      indexAll = true;
      genKW=getHostName();
      i++;
    }
    for (; i<tokens.length; i++ ) {
      String tok = tokens[i];
      if (toFloat(tok) == null && isKeyWord(tok, min, max)) {
        if (!indexAll) {
          keyWords.add(tok);
          break;
        }
        else {
          if (tok.matches(".*\\d+.*"))
            genKW = genKW+".var";
          else
            genKW=genKW+"."+tok;
        }
      }
    }
    if (indexAll) 
      keyWords.add(genKW);
    //tokens = str.split("\\s");
    //masterKey = tokens[0];
  }

  /**
   * Returns true if a word is a metric
   * @param   str   the word
   */
  private Float toFloat(String str) {
    try {
      return Float.parseFloat(str);
    } catch (NumberFormatException nfe) {
      return null;
    }
  }

  /**
   * Returns true if a word is a long
   * @param   str   the word
   */
  private Long toLong(String str) {
    try {
      return Long.parseLong(str);
    } catch (NumberFormatException nfe) {
      return null;
    }
  }

  /**
   * Returns true if a word has the right length to be a keyword
   * @param   str   the keyword
   * @param   min   min word length
   * @param   max   max word length
   */
  private boolean isKeyWord(String str, int min, int max) {
    if ( str.length() >= min && str.length() < max )
      return true;
    return false;
  }

  /**
   * Changes milliseconds into date with format: MM/dd/YY HH:mm:ss
   * @param  millisecs the millisecs to change
   * @return  String containing the date.
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
   * @return  the whole data
   */
  public String getRecord() {
    return theRecord;
  }

  /**
   * Access Method
   * @return  the nth word from the data or null.
   */
  public String getRecord(int m) {
    String toks[] = theRecord.split("\\s");
    if (m < toks.length || m < 0) 
      return toks[m];
    return null;  
  }

  /**
   * A check if the word exists in the list of detected keywords
   * @param test  the string to test
   */
  public boolean hasKeyword(String test) {
    return keyWords.contains(test);
  }

  /**
   * Access Method
   * @return   ArrayList containing the keyword for this
   */
  public ArrayList<String> getKeyWords() {
    return keyWords;
  }

  /**
   * A check if the word is contained in the dato.
   * This will be slower than hasKeyword, but does not require a precise match
   * @param test  the string to test
   */
  public boolean containsWord(String test) {
    return theRecord.toLowerCase().contains(test.toLowerCase());
  }

  /**
   * This will most likely not be used
   * @return the first word of data (could be useful is all data is org.gnome.desktop.wm.raiseOnFocus 0)
   */

  public boolean isGauge() {
    return IAmGauge;
  }

  public boolean isCounter() {
    return IAmCounter;
  }

  public boolean isSum() {
    return IAmSum;
  }

  public boolean isHistogram() {
    return IAmHistogram;
  }

  public boolean isEvent() {
    return IAmEvent;
  }

  public boolean isFreq() {
    return (!IAmGauge && !IAmCounter && !IAmSum && !IAmEvent);
  }

  public Long getCounter() {
    return theCounter;
  }

  public Float getValue() {
    return theValue;
  }

  public String getEvent() {
    if (IAmEvent)
      return theEvent;
    return null;
  }

  private long timeStamp;
  private String timeNano;
  private InetAddress myip;
  private String theRecord;
  private Float theValue;
  private boolean IAmGauge;
  private boolean IAmSum;
  private boolean IAmHistogram;
  private boolean IAmEvent;
  private Long theCounter;
  private boolean IAmCounter;
  private String theDate;
  private String theEvent;
  private ArrayList<String> keyWords;
  //private String masterKey;
}
