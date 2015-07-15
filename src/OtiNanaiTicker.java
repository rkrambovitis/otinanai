package gr.phaistosnetworks.admin.otinanai;


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
		try {
		Thread.sleep(30000);
		onl.tick();
		Thread.sleep(36000000);
		} catch (Exception e) {
			System.err.println("ticker failed");
		}
	}

	private OtiNanaiListener onl;
	private Logger logger;
}

