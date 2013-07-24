import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.logging.*;
import java.text.SimpleDateFormat;



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
            boolean timeGraph=false;
            boolean showKeyWords=false;
            boolean draw=false;
				switch (requestMessageLine) {
					case " / ":
					case "  ":
                  logger.info("[Web]: Sending default blank webpage");
						String bogus = new String();
                  bogus = bogus 
                     +"<html><body>"
                     +"<h3>Robert's random piece of junk.</h3>"
                     +"<hr>Keys:"
                     +"<li><a href=\"k\">k</a> : show keywords"
                     +"<li><a href=\"a\">a</a> : show alarms"
                     +"</body></html>";
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
                           case "a":
                              logger.info("[Web]: getAlarms matched");
										alarms=true;
										break;
                           case "k":
                              logger.info("[Web]: showKeyWords matched");
                              showKeyWords = true;
                              break;
                           default:
                              logger.info("[Web]: Draw matched");
                              draw = true;
                              break;
                              /*
                           case "g":
                              logger.info("[Web]: doGraph matched");
										graph=true;
										break;
                           case "t":
                              logger.info("[Web]: timeGraph matched");
										timeGraph=true;
										break;
                           default:
                              logger.info("[Web]: processing word "+word);
										title.append(word+" ");
										results = onp.processCommand(results, word.replaceAll("\\s", ""));
										break;
                              */
								}
							}
						}
						String text = new String();
                  if (showKeyWords) {
                     text = showKeyWords();
                  } else if (alarms) {
							text = getAlarms();
                  } else if (draw) {
                     text = draw(request);
                  }
                     /*
                  } else if (timeGraph) {
                     text = getTimeGraph(request);
						} else if (metric <= 0) {
							text = toString(results);
						} else if (graph) {
							text = toGraph(results, metric-1, title.toString());
						} else {
							text = toString(results, metric-1);
						}
                  */
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
		output = output + "var options = { title: \""+title+"\", hAxis: { direction: \"-1\" }};\n";
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

	private String toGraph(LinkedList<String> data, String title) {
      logger.finest("[Web]: Generating graph from KeyWordTracker: "+title);
		String output = new String("");

		output = output + "['Time','"+title+"'],\n";
		SomeRecord sr;
      if (data.size() == 0 ) {
         output = "['foo', 0],\n";
      } else {
         for (String dato : data) {
            output = output + "['";
            String[] twowords = dato.split("\\s");
            output = output + calcDate(twowords[0]) + "'," + twowords[1];
            output = output + "],\n";
         }
      }
      return output;
	}

   /*
	private String toString(ArrayList<String> keyList, int metric) {
      logger.finest("[Web]: Generating Web Output for metric :"+metric);
		String output = new String("<html><body>");
		SomeRecord sr;
		for (String key : keyList) {
			sr = dataMap.get(key);
			if (sr.isMetric(metric)) {
				output = output 
               + "<div style=\"text-decoration:none;color:green;\">"
               + sr.getDate() 
               + "</div><div style=\"text-decoration:none;color:orange;\">"
               + " " + sr.getHostName() 
               + "</div><div style=\"text-decoration:none;color:black;\">"
               + " " + sr.getRecord(metric)
               + "</div><br>";
			}
		}
		if (output.equals("<html><body>")) {
			output = output + "No Data";
		}
		output = output + "</body></html>";
		return output;
	}
   */

	private String toString(ArrayList<String> keyList) {
      logger.finest("[Web]: Generating Web Output");
		SomeRecord sr;
      String output = new String();
		for (String key : keyList) {
			sr = dataMap.get(key);
         output = output 
            + "<div style=\"text-decoration:none;color:green;\">"
            + sr.getDate() 
            + "</div><div style=\"text-decoration:none;color:orange;\">"
            + " " + sr.getHostName() 
            + "</div><div style=\"text-decoration:none;color:black;\">"
            + " " + sr.getRecord()
            + "</div><br>";
//			output = output + sr.getTimeStamp() + " " + sr.getHostName() + " " + sr.getRecord()+"\n";
		}
		return output;
	}

	private String drawText(String keyword) {
      ArrayList<String> results = new ArrayList<String>();
      results = onp.processCommand(results, keyword);
      logger.finest("[Web]: Generating Web Output");
		SomeRecord sr;
      String output = new String();
		for (String key : results) {
         sr = dataMap.get(key);
         output = output 
            + "<div style=\"text-decoration:none;color:green;\">"
            + sr.getDate() 
            + "</div><div style=\"text-decoration:none;color:orange;\">"
            + " " + sr.getHostName() 
            + "</div><div style=\"text-decoration:none;color:black;\">"
            + " " + sr.getRecord()
            + "</div><br>";
      }
		return output;
	}

   private String timeGraphHead(ArrayList<KeyWordTracker> kws) {
      String output = new String("");
		for (KeyWordTracker kwt : kws) {
         output = output + "<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>\n";
         output = output + "<script type=\"text/javascript\">\n";
         output = output + "google.load(\"visualization\", \"1\", {packages:[\"corechart\"]});\n";
         output = output + "google.setOnLoadCallback(drawChart);\n";
         output = output + "function drawChart() {\n";
         output = output + "var data = google.visualization.arrayToDataTable([\n";
         output = output + toGraph(kwt.getFiveMinMemory(), kwt.getKeyWord());
         output = output + "]);\n";
         output = output + "var options = { title: \""+kwt.getKeyWord()+"\", hAxis: {direction: \"-1\" }};\n";
         output = output + "var chart = new google.visualization.LineChart(document.getElementById('"+kwt.getKeyWord()+"'));\n";
         output = output + "chart.draw(data, options);\n";
         output = output + "}\n";
         output = output + "</script>\n";
      }
      return output;
   }

   private String timeGraphBody(ArrayList<KeyWordTracker> kws) {
      String output = new String("");
      for (KeyWordTracker kwt : kws) {
         output = output + "<div id=\""+kwt.getKeyWord()+"\" style=\"width: 900px; height: 500px;\"></div><br>\n";
         output = output + drawText(kwt.getKeyWord());
      }
      return output;
   }

   private String draw(String[] keyList) {
      logger.info("[Web]: Drawing Output for keywords");
		HashMap<String,KeyWordTracker> allKWs = onl.getKeyTrackerMap();
      ArrayList<KeyWordTracker> toGraph = new ArrayList<KeyWordTracker> ();
      for (String key : keyList) {
         if (allKWs.containsKey(key)) {
            toGraph.add(allKWs.get(key));
         }
      }
		String output = new String("<html><body>");
      output = output + timeGraphHead(toGraph);
		output = output + "</head><body>\n";
      output = output + timeGraphBody(toGraph);
		output = output + "</body></html>";
      return output;
   }
/*
	private String getTimeGraph(String[] keyList) {
      logger.info("[Web]: Generating TimeGraph Output for keywords");
		HashMap<String,KeyWordTracker> allKWs = onl.getKeyTrackerMap();
      ArrayList<KeyWordTracker> toGraph = new ArrayList<KeyWordTracker> ();
      for (String key : keyList) {
         if (allKWs.containsKey(key)) {
            toGraph.add(allKWs.get(key));
         }
      }
      String output = new String("<html><head>");
      output = output + timeGraphHead(toGraph);
		output = output + "</head><body>\n";
      output = output + timeGraphBody(toGraph)
		output = output + "</body></html>";
      return output;
	}
   */

	private String getAlarms() {
      logger.finest("[Web]: Generating Alarms Output");
		Collection<KeyWordTracker> allKWs = onl.getKeyTrackerMap().values();
//      ArrayList<KeyWordTracker> alarmKWs = new ArrayList<KeyWordTracker> ();
      ArrayList<String> kws = new ArrayList<String> ();
      for (KeyWordTracker kwt : allKWs ) {
         if (kwt.getAlarm()) {
            //alarmKWs.add(kwt);
            kws.add(kwt.getKeyWord());
         }
      }
      if (kws.size() == 0 ) {
         return new String("No Alarms");
      }
      String[] asd = new String[kws.size()];
      for (int i=0; i< kws.size(); i++) {
         asd[i] = kws.get(i);
      }
      return draw(asd);
      /*
      return draw((String[])kws.toArray());
      */
	}

	private String showKeyWords() {
      logger.finest("[Web]: Generating List of KeyWords");
		Collection<KeyWordTracker> allKWs = onl.getKeyTrackerMap().values();
		String output = new String("<html><body>");
      for (KeyWordTracker kwt : allKWs ) {
         output = output + "<li><a href = \""+kwt.getKeyWord()+"\">"+kwt.getKeyWord()+"</a></li>\n";
      }
		if (output.equals("<html><body>")) {
			output = output + "No Data";
		}
		output = output + "</body></html>";
      return output;
	}

   /**
    * Changes milliseconds into date with format: MM/dd/YY HH:mm:ss
    * @param   millisecs the millisecs to change
    * @return  String containing the date.
    */
   private String calcDate(String millisecs) {
      SimpleDateFormat date_format = new SimpleDateFormat("MM/dd/YY HH:mm:ss");
      Date resultdate = new Date(Long.parseLong(millisecs));
      return date_format.format(resultdate);
   }


	private OtiNanaiProcessor onp;
	private OtiNanaiListener onl;
	private HashMap<String,SomeRecord> dataMap;
	private int port;
	private ServerSocket listenSocket;
	private Logger logger;
}
