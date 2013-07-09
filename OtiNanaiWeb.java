import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;


class OtiNanaiWeb implements Runnable {
	public OtiNanaiWeb(OtiNanaiListener o, int lp) throws IOException {
		onl = o;
		onp = new OtiNanaiProcessor(o);
		dataMap = onl.getDataMap();
		port = lp;
		ServerSocket listenSocket = new ServerSocket(port);
	}

	public OtiNanaiWeb(OtiNanaiListener o, ServerSocket ss) {
		onl = o;
		onp = new OtiNanaiProcessor(o);
		dataMap = onl.getDataMap();
		listenSocket = ss;
	}
    
	public void run() {
		try {
			BufferedReader inFromClient;
			String requestMessageLine;
			while (true) {
				Socket connectionSocket = listenSocket.accept();
				ArrayList<String> results = new ArrayList<String>();
				try {
					inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
					requestMessageLine = inFromClient.readLine().replaceAll("[;\\/]", "").replaceAll("GET|HTTP1.1", "");
					System.err.println("\""+requestMessageLine+"\"");
				} catch (NullPointerException npe) {
					System.err.println("gotcha");
					continue;
				}
				boolean alarms=false;
				boolean graph=false;
				switch (requestMessageLine) {
					case " / ":
					case "  ":
						String bogus = new String("Robert's random piece of junk.");
						sendToClient(bogus.getBytes(), "text/html", false, connectionSocket);
						break;
					case " favicon.ico ":
						FileInputStream fico = new FileInputStream(new File("/root/OtiNanai/favicon.ico"));
						Path path = Paths.get("/root/OtiNanai/favicon.ico");
						byte[] data = Files.readAllBytes(path);
						sendToClient(data, "image/x-icon", true, connectionSocket);
						break;
					default:
						String[] request = requestMessageLine.split("[ .,]|%20");
						StringBuilder title = new StringBuilder();
						Integer metric = new Integer(-10);
						for (String word : request) {
							if (word.equals(""))
									continue;
							try {
								metric = Integer.parseInt(word);
							} catch (NumberFormatException nfe) {
								switch (word) {
									case "getAlarms":
										alarms=true;
										break;
									case "drawGraph":
										graph=true;
										break;
									default:
										title.append(word+" ");
										results = onp.processCommand(results, word.replaceAll("\\s", ""));
										break;
								}
							}
						}
						String text;
						if (alarms) {
							text=getAlarms();
						} else if (metric <= 0) {
							text = toString(results);
						} else if (graph) {
							text = toGraph(results, metric-1, title.toString());
						} else {
							text = toString(results, metric-1);
						}
						sendToClient(text.getBytes(), "text/html", false, connectionSocket);
						connectionSocket.close();
				}
			}
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}

	private boolean sendToClient(byte[] dato, String contType, boolean cache, Socket connectionSocket) {
		try {
			DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
			outToClient.writeBytes("HTTP/1.1 200 OK\r\n");
			outToClient.writeBytes("Content-Type: "+contType+"\r\n");
			int numOfBytes = dato.length;
			outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
			if (cache) 
				outToClient.writeBytes("Expires: Wed, 31 Dec 2014 23:59:59 GMT\r\n");
			outToClient.writeBytes("\r\n");
			outToClient.write(dato, 0, numOfBytes);
			connectionSocket.close();
			return true;
		} catch (IOException ioe) {
			System.err.println("Here");
			return false;
		}
	}

	private String toGraph(ArrayList<String> keyList, int metric, String title) {
		String output = new String("<html><head>\r");
		output = output + "<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>\n";
		output = output + "<script type=\"text/javascript\">\n";
		output = output + "google.load(\"visualization\", \"1\", {packages:[\"corechart\"]});\n";
		output = output + "google.setOnLoadCallback(drawChart);\n";
		output = output + "function drawChart() {\n";
		output = output + "var data = google.visualization.arrayToDataTable([\n";

		output = output + "['Timestamp', 'Value'],\n";
		SomeRecord sr;
		for (String key : keyList) {
			sr = dataMap.get(key);
			if (sr.isMetric(metric)) {
				output = output + "['" + sr.getDate() + "', " + sr.getRecord(metric) + "],\n";
			} else {
				output = output + "['foo', '0'],\n";
			}
		}
//		output = output.substring(0, output.length()-1);
		output = output + "]);\n";
		output = output + "var options = { title: \""+title+"\" };\n";
	   output = output + "var chart = new google.visualization.LineChart(document.getElementById('chart_div'));\n";
	   output = output + "chart.draw(data, options);\n";
		output = output + "}\n";
		output = output + "</script>\n";
		output = output + "</head>\n";
		output = output + "<body>\n";
		output = output + "<div id=\"chart_div\" style=\"width: 900px; height: 500px;\"></div>\n";
		output = output + "</body>\n";
		output = output + "</html>\n";
		return output;
	}

	private String toString(ArrayList<String> keyList, int metric) {
		String output = new String("<html><body><pre>");
		SomeRecord sr;
		for (String key : keyList) {
			sr = dataMap.get(key);
			if (sr.isMetric(metric)) {
				output = output + sr.getTimeStamp() + " " + sr.getHostName() + " " + sr.getRecord(metric);
			}
		}
		if (output.equals("<html><body><pre>")) {
			output = output + "No Data";
		}
		output = output + "</pre></body></html>";
		return output;
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

	private String getAlarms() {
		Collection<KeyWordTracker> allKWs = onl.getKeyTrackerMap().values();
		String output = new String("<html><body><pre>");
		for (KeyWordTracker kwt : allKWs) {
			if (kwt.getAlarm()) {
				output=output + "Alarm Exists: " + kwt.getKeyWord() + "\n";
			}
		}
		output = output + "</pre></body></html>";
		return output;
	}

	private OtiNanaiProcessor onp;
	private OtiNanaiListener onl;
	private HashMap<String,SomeRecord> dataMap;
	private int port;
	private ServerSocket listenSocket;
}
