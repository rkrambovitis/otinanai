import java.io.*;
import java.net.*;
import java.util.*;

class OtiNanaiWeb implements Runnable {
	public OtiNanaiWeb(OtiNanaiListener o) {
		onl = o;
		onp = new OtiNanaiProcessor(o);
		dataMap = onl.getDataMap();
	}
    
	public void run() {
		String requestMessageLine;
		try {
			ServerSocket listenSocket = new ServerSocket(6789);
			while (true) {
				Socket connectionSocket = listenSocket.accept();
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				requestMessageLine = inFromClient.readLine().replaceAll("[;\\/]", "").replaceAll("GET|HTTP1.1", "");
				System.err.println(requestMessageLine);
				DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				ArrayList<String> results = new ArrayList<String>();
				if (requestMessageLine.equalsIgnoreCase(" favicon.ico ")) {
					outToClient.writeBytes("HTTP/1.1 404 Not Found\r\n");
					connectionSocket.close();
				} else {
					String[] request = requestMessageLine.split("[ .,]");
					for (String word : request) {
						if (word.equals(""))
								continue;
						results = onp.processCommand(results, word.replaceAll("\\s", ""));
					}
					String text = toString(results);

					outToClient.writeBytes("HTTP/1.1 200 OK\r\n");
					outToClient.writeBytes("Content-Type: text/html\r\n");

					int numOfBytes = text.length();

					outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
					outToClient.writeBytes("\r\n");
				 
					outToClient.write(text.getBytes(), 0, numOfBytes);
					connectionSocket.close();
				}
			}
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}

	private String toString(ArrayList<String> keyList) {
		String output = new String("<html><body><pre>");
		SomeRecord sr;
		for (String key : keyList) {
			sr = dataMap.get(key);
			output = output + sr.getTimeStamp() + " " + sr.getHostName() + " " + sr.getRecord();
		}
		output = output + "</pre></body></html>";
		return output;
	}

	private OtiNanaiProcessor onp;
	private OtiNanaiListener onl;
	private HashMap<String,SomeRecord> dataMap;
}
