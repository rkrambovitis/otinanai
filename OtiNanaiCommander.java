import java.io.*;
import java.util.*;

class OtiNanaiCommander implements Runnable {
	public OtiNanaiCommander(OtiNanaiListener o) {
		onp = new OtiNanaiProcessor(o);
	}

	public void run() {
		Console console = System.console();
		while (true) {
			String s = console.readLine();
			if (s.length() == 0) continue;
			//onp.setONL(onl);
			print(onp.processCommand(s));
		}
	}

	private void print(Vector<SomeRecord> matched) {
		for (SomeRecord sr : matched) {
			//System.out.println(tsToDate(sr.getTimeStamp()) + " " + sr.getHostName() + " " + sr.getRecord());
			System.out.println(sr.getTimeNano() + " " + sr.getTimeStamp() + " " + sr.getHostName() + " " + sr.getRecord());
		}
	}

	private Date tsToDate(long ts) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(ts);
		return cal.getTime();
	}
	
	private OtiNanaiProcessor onp;
}
