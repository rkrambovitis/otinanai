import java.io.*;
import java.util.concurrent.*;

class OtiNanaiCommander implements Runnable {
	public OtiNanaiCommander(OtiNanaiListener o) {
		onl = o;
	}

	public void run() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String s;
		try {
			while ((s = in.readLine()) != null && s.length() != 0) {
			//System.out.println(s);
			    processCommand(s);
			}
		} catch (IOException ioe) {
			System.err.println(ioe);
		}
	}

	private void processCommand(String input) {
		System.out.println(input);
		switch (input) {
			case "hello": 
				System.out.println("And what a fine morning it is");
				break;
			case "get":
				getData();
				break;
			case "date":
				System.out.println(System.currentTimeMillis());
				break;
			default: 
				usage();
				break;
		}
	}
	
	private void getData() {
		CopyOnWriteArrayList<SomeRecord> storage = onl.getData();
		SomeRecord aRecord;
		for (int i=0; i<storage.size(); i++) {
			aRecord = storage.get(i);
			System.out.println(aRecord.getIP() + " " + aRecord.getHostName() + " " + aRecord.getTimeStamp() + " " + aRecord.getRecord());
		}
	}

	private void usage() {
		System.out.println("try: <hello|get|date>");
	}

	private OtiNanaiListener onl;
}
