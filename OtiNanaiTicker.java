import java.io.*;
import java.util.*;
import java.util.logging.*;

class OtiNanaiTicker implements Runnable {
	public OtiNanaiTicker(OtiNanaiListener o, Logger l) {
		onl = o;
      logger = l;
      logger.finest("[Ticker]: New OtiNanaiTicker Initialized");
	}

	public void run() {
		while (true) {
         try {
            Thread.sleep(FIVE_MIN);
            logger.info("[Ticker]: TICK ?");
            onl.tick();
            logger.info("[Ticker]: TOCK !");
         } catch (InterruptedException ie) {
            logger.severe("[Ticker]: "+ie.getMessage());
         }
		}
	}
	
	private OtiNanaiListener onl;
   private static final int FIVE_MIN = 3000;
   private Logger logger;
}

