import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.logging.*;


class OtiNanaiWeb implements Runnable {
	public OtiNanaiWeb(OtiNanaiListener o, int lp, Logger l) throws IOException {
		onl = o;
		onp = new OtiNanaiProcessor(o);
		dataMap = onl.getDataMap();
		port = lp;
		ServerSocket listenSocket = new ServerSocket(port);
		logger = l;
		logger.finest("[Web]: New OtiNanaiWeb Initialized");
	}

	public OtiNanaiWeb(OtiNanaiListener o, ServerSocket ss, Logger l) {
		onl = o;
		onp = new OtiNanaiProcessor(o);
		dataMap = onl.getDataMap();
		listenSocket = ss;
		logger = l;
		logger.finest("[Web]: New OtiNanaiWeb Initialized");
	}
    
	public void run() {
		logger.finest("[Web]: New OtiNanaiWeb Thread Started");
		try {
			BufferedReader inFromClient;
			String requestMessageLine;
			while (true) {
				Socket connectionSocket = listenSocket.accept();
				ArrayList<String> results = new ArrayList<String>();
				try {
					inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
					requestMessageLine = inFromClient.readLine().replaceAll("[;\\/]", "").replaceAll("GET|HTTP1.1", "");
					logger.fine("[Web]: Got Web request for : \""+requestMessageLine+"\"");
				} catch (NullPointerException npe) {
					logger.warning("[Web]: "+npe.getMessage());
					continue;
				}
				boolean alarms=false;
				boolean graph=false;
				switch (requestMessageLine) {
					case " / ":
					case "  ":
                  logger.info("[Web]: Sending default blank webpage");
						String bogus = new String("Robert's random piece of junk.");
						sendToClient(bogus.getBytes(), "text/html", false, connectionSocket);
						break;
					case " favicon.ico ":
                  logger.info("[Web]: Sending favicon.ico");
						//FileInputStream fico = new FileInputStream(new File("/home/robert/OtiNanai/favicon.ico"));
						Path path = Paths.get("/home/robert/OtiNanai/favicon.ico");
						byte[] data = Files.readAllBytes(path);
						sendToClient(data, "image/x-icon", true, connectionSocket);
						break;
					default:
						String[] request = requestMessageLine.split("[ .,]|%20");
						StringBuilder title = new StringBuilder();
						Integer metric = new Integer(-10);
						for (String word : request) {
							if (word.equals("")) {
                           logger.fine("[Web]: Skipping blank word");
									continue;
                     }
							try {
								metric = Integer.parseInt(word);
                        logger.fine("[Web]: word is metric");
							} catch (NumberFormatException nfe) {
                        logger.fine("[Web]: word is not a metric");
								switch (word) {
									case "getAlarms":
                              logger.fine("[Web]: getAlarms matched");
										alarms=true;
										break;
									case "drawGraph":
                              logger.fine("[Web]: doGraph matched");
										graph=true;
										break;
									default:
                              logger.fine("[Web]: processing word "+word);
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
                  logger.fine("[Web]: got text, sending to client");
						sendToClient(text.getBytes(), "text/html", false, connectionSocket);
						connectionSocket.close();
				}
			}
		} catch (IOException ioe) {
			logger.severe("[Web]: "+ioe.getMessage());
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
			logger.severe("[Web]: "+ioe.getMessage());
			return false;
		}
	}

	private String toGraph(ArrayList<String> keyList, int metric, String title) {
      logger.finest("[Web]: Generating graph: "+title);
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

	private String toGraph(LinkedList<Long> data, String title) {
      logger.finest("[Web]: Generating graph from KeyWordTracker: "+title);
		String output = new String();

		output = output + "['Value'],\n";
		SomeRecord sr;
		for (Long dato : data) {
         output = output + "['" + dato + "],\n";
		}
      return output;
	}

	private String toString(ArrayList<String> keyList, int metric) {
      logger.finest("[Web]: Generating Web Output for metric :"+metric);
		String output = new String("<html><body><pre>");
		SomeRecord sr;
		for (String key : keyList) {
			sr = dataMap.get(key);
			if (sr.isMetric(metric)) {
				output = output + sr.getTimeStamp() + " " + sr.getHostName() + " " + sr.getRecord(metric)+"\n";
			}
		}
		if (output.equals("<html><body><pre>")) {
			output = output + "No Data";
		}
		output = output + "</pre></body></html>";
		return output;
	}

	private String toString(ArrayList<String> keyList) {
      logger.finest("[Web]: Generating Web Output");
		String output = new String("<html><body><pre>");
		SomeRecord sr;
		for (String key : keyList) {
			sr = dataMap.get(key);
			output = output + sr.getTimeStamp() + " " + sr.getHostName() + " " + sr.getRecord()+"\n";
		}
		output = output + "</pre></body></html>";
		return output;
	}

	private String getAlarms() {
      logger.finest("[Web]: Generating Alarms Output");
		Collection<KeyWordTracker> allKWs = onl.getKeyTrackerMap().values();
		String output = new String("<html><head>");
		output = output + "<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>\n";
		output = output + "<script type=\"text/javascript\">\n";
		output = output + "google.load(\"visualization\", \"1\", {packages:[\"corechart\"]});\n";
		output = output + "google.setOnLoadCallback(drawChart);\n";
		output = output + "function drawChart() {\n";
		output = output + "var data = google.visualization.arrayToDataTable([\n";
		for (KeyWordTracker kwt : allKWs) {
			if (kwt.getAlarm()) {
				//output=output + "Alarm Exists: " + kwt.getKeyWord() + "\n";
            output = output + toGraph(kwt.getFiveMinMemory(), kwt.getKeyWord());
			}
		}
      output = output + "]);\n";
		output = output + "var options = { title: \"FooBar\" };\n";
	   output = output + "var chart = new google.visualization.LineChart(document.getElementById('chart_div'));\n";
	   output = output + "chart.draw(data, options);\n";
		output = output + "}\n";
		output = output + "</script>\n";
		output = output + "</head>\n";
		output = output + "<body>\n";
		output = output + "<div id=\"chart_div\" style=\"width: 900px; height: 500px;\"></div>\n";

		output = output + "</body></html>";
		return output;
	}

	private OtiNanaiProcessor onp;
	private OtiNanaiListener onl;
	private HashMap<String,SomeRecord> dataMap;
	private int port;
	private ServerSocket listenSocket;
	private Logger logger;
}
