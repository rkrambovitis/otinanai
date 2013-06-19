import java.io.*;
import java.util.concurrent.*;
import java.util.*;

class OtiNanaiCommander implements Runnable {
	public OtiNanaiCommander(OtiNanaiListener o) {
		onl = o;
	}

	public void run() {
		Console console = System.console();
		while (true) {
			String s = console.readLine();
			if (s.length() == 0) continue;
			//System.out.println("issuing processcomand("+s+");");
			processCommand(s);
		}
	}

	private void processCommand(String input) {
		CopyOnWriteArrayList<SomeRecord> storage = onl.getData();
		Vector<SomeRecord> matched = new Vector<SomeRecord>();
		String[] inputWords = input.split("\\s");
		for (String word : inputWords ) {
			for (SomeRecord aRecord : storage ) {
				if (aRecord.hasKeyword(word)) {
					matched.add(aRecord);
				}
			}
		}
		if (matched.size() == 0 ) {
			for (String word : inputWords ) {
				//System.out.println(word);
				for (SomeRecord aRecord : storage ) {
					if (aRecord.containsWord(word)) {
						matched.add(aRecord);
					}
				}
			}
		}
		for (SomeRecord sr : matched) {
				System.out.println(sr.getTimeStamp() + " " + sr.getHostName() + " " + sr.getRecord());
		}
	}
	
	private OtiNanaiListener onl;
}
