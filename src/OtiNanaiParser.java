package gr.phaistosnetworks.admin.otinanai;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.net.*;


class OtiNanaiParser implements Runnable {
  public OtiNanaiParser(OtiNanaiListener o, String td, InetAddress ip, Logger l) {
    logger = l;
    hip = ip;
    theDato = td;
    onl = o;
  }

  public void run() {
    logger.finest("[Parser]: Attempting to parse: \""+theDato+"\" from "+hip);

    SomeRecord newRecord = new SomeRecord(hip, theDato);
    if (newRecord.isEvent()) {
      onl.addEvent(newRecord);
      return;
    }

    ArrayList<String> theKeys = newRecord.getKeyWords();
    for (String kw : theKeys) {
      if (newRecord.isFreq()) {
        try { 
          Float.parseFloat(kw);
          continue;
        } catch (NumberFormatException e) {}

        if (kw.equals("")) {
          continue;
        }
      } 

      KeyWordTracker kwt = onl.getKWT(kw);
      if (kwt == null) {
        logger.info("[Parser]: New KeyWord: \""+kw+"\" from "+hip);
        kwt = onl.addKWT(kw);
      }

      if (newRecord.isGauge()) {
        kwt.putGauge(newRecord.getValue());
      } else if (newRecord.isSum()) {
        kwt.putSum(newRecord.getValue());
      } else if (newRecord.isCounter()) {
        kwt.putCounter(newRecord.getCounter());
      } else {
        kwt.putFreq();
      }
    }
  }

  private String theDato;
  private InetAddress hip;
  private Logger logger;
  private OtiNanaiListener onl;
}

