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

	public OtiNanaiWeb(OtiNanaiListener o, OtiNanaiCache oc, ServerSocket ss, Logger l) {
		onl = o;
      onc = oc;
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
            Path path;
            byte[] data;
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
						path = Paths.get("web/favicon.ico");
						data = Files.readAllBytes(path);
						sendToClient(data, "image/x-icon", true, connectionSocket);
						break;
					case " otinanai.css ":
                  logger.info("[Web]: Sending otinanai.css");
						path = Paths.get("web/otinanai.css");
						data = Files.readAllBytes(path);
						sendToClient(data, "text/css", true, connectionSocket);
						break;
					default:
						String[] request = requestMessageLine.split("[ ,]|%20");
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

   private String toGraph(OtiNanaiMemory onm) {
      logger.finest("[Web]: Generating graph from OtiNanaiMemory: "+onm.getKeyWord());
		String output = new String("\n");
      SomeRecord sr;
      int i=0;
      Set<String> gah = onm.getAllHosts();
      int gahSize = gah.size();
      if (gahSize == 2)
         gahSize = 1;
      for (String host : gah) {
         LinkedList<String> data = onm.getMemory(host);
         if (data.size() == 0 ) {
            output = output + "[new Date(2013,07,30,0,0,0)";
            for (int j=0; j<gahSize; j++) {
               output=output + ",undefined";
            }
            output = output + "],\n";
         } else {
            for (String dato : data) {
               output = output + "[";
               String[] twowords = dato.split("\\s");
               output = output + calcDate(twowords[0]) + ",";
               for (int j=0;j<i;j++) {
                  output=output + "undefined,";
               }
               output = output + twowords[1];
               for (int j=i+1; j<gahSize; j++) {
                  output=output + ",undefined";
               }
               output = output + "],\n";
            }
         }
         if (gahSize == 1)
            break;
         i++;
      }
      return output;
   }

	private String toGraph(LinkedList<String> data, String title) {
      logger.finest("[Web]: Generating graph from OtiNanaiMemory: "+title);
		String output = new String("\n");

		SomeRecord sr;
      if (data.size() == 0 ) {
				output = output + "[new Date(2013,07,30,0,0,0), 0],\n";
      } else {
         for (String dato : data) {
            output = output + "[";
            String[] twowords = dato.split("\\s");
            output = output + calcDate(twowords[0]) + "," + twowords[1];
            output = output + "],\n";
         }
      }
      return output;
	}

	private String drawText(String keyword) {
      ArrayList<String> results = new ArrayList<String>();
      results = onp.processCommand(results, keyword);
      Collections.reverse(results);
      logger.finest("[Web]: Generating Web Output");
		SomeRecord sr;
      String output = new String();
      output = output + "<div id=\"wrapper\">";
      int c=0;
		for (String key : results) {
         sr = dataMap.get(key);
         try {
            output = output 
               + "<div class=\"log\"><span class=\"date\">"
               + sr.getDate() 
               + "</span><span class=\"server\">"
               + sr.getHostName() 
               + "</span><span class=\"data\">"
               + sr.getRecord()
               + "</span></div>\n";
         } catch (NullPointerException npe) {
            logger.severe(npe.getMessage());
         }
         if (c >= MAX_LOG_OUTPUT) {
            break;
         }
         c++;
      }
      output = output + "</div>";
		return output;
	}

   private String timeGraphHead(ArrayList<OtiNanaiMemory> kws) {
      String output = new String("");
		for (OtiNanaiMemory onm : kws) {
         output = output + "<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>\n"
            + "<script type=\"text/javascript\">\n"
            + "google.load(\"visualization\", \"1\", {packages:[\"annotatedtimeline\"]});\n"
            + "google.setOnLoadCallback(drawChart);\n"
            + "function drawChart() {\n"
            + "var data = new google.visualization.DataTable();\n"
            + "data.addColumn('datetime', 'Date');\n";
         for (String host : onm.getAllHosts()) {
            if (onm.getAllHosts().size() == 2) {
               output = output + "data.addColumn('number', '"+onm.getKeyWord()+"');\n";
               break;
            }
            output = output + "data.addColumn('number', '"+host+"');\n";
         }
//            + "data.addColumn('string', 'Host');\n";
//            + "data.addColumn('number', '"+onm.getKeyWord()+"');\n"
         output = output + "data.addRows(["
            + toGraph(onm)
            //+ toGraph(onm.getMemory(), onm.getKeyWord())
            + "]);\n"
            + "var options = { title: \""+onm.getKeyWord()+"\", hAxis: {direction: \"-1\" }};\n"
            + "var chart = new google.visualization.AnnotatedTimeLine(document.getElementById('myGraph'));\n"
            + "chart.draw(data, options);\n"
            + "}\n"
            + "</script>\n";
      }
      return output;
   }

   private String timeGraphBody(ArrayList<OtiNanaiMemory> kws) {
      String output = new String("");
      for (OtiNanaiMemory onm : kws) {
         output = output + "<div id=\"myGraph\"></div><br>\n";
         output = output + drawText(onm.getKeyWord());
      }
      return output;
   }

   private String draw(String[] keyList) {
      logger.info("[Web]: Drawing output for keywords");
		HashMap<String,OtiNanaiMemory> allKWs = onl.getMemoryMap();
      ArrayList<OtiNanaiMemory> graphMe = new ArrayList<OtiNanaiMemory> ();
      String fullString = new String();
      for (String key : keyList) {
         key=key.toLowerCase();
         logger.info("[Web]: looking for keyword: " + key);
         if (allKWs.containsKey(key)) {
            graphMe.add(allKWs.get(key));
            fullString = fullString+key+" ";
         }
      }
      String output = onc.getCached(fullString);
      if (output == null) {
         logger.fine("[Web]: Not cached, will generate \"" + fullString+"\"");
         output = new String("<html><head>\n");
         output = output + "<link rel=\"stylesheet\" type=\"text/css\" href=\"otinanai.css\" />\n"
            + timeGraphHead(graphMe)
            + "</head><body>\n"
            + timeGraphBody(graphMe)
            + "</body></html>";
         onc.cache(fullString, output);
         return output;
      } else {
         logger.fine("[Web]: Cached data for \"" + fullString + "\"");
         return output;
      }
   }

	private String getAlarms() {
      logger.finest("[Web]: Generating Alarms Output");
		Collection<OtiNanaiMemory> allOMs = onl.getMemoryMap().values();
		String output = new String("<html><body>");
      ArrayList<String> kws = new ArrayList<String> ();
      for (OtiNanaiMemory onm : allOMs ) {
         if (onm.getAlarm(System.currentTimeMillis())) {
            kws.add(onm.getKeyWord());//+" "+onm.getFiveMinCount()+" "+onm.getThirtyMinCount());
         }
      }
      output = output + listKeyWords(kws);
		output = output + "</body></html>";
      return output;
   }

   private String listKeyWords(ArrayList<String> keyWords) {
      String output = new String();
      if (keyWords.size() == 0) {
         return new String("No KeyWords");
      }
      //TreeSet<String> sortedKeys = new TreeSet<String>(keyWords);
      TreeSet<String> sortedKeys = new TreeSet<String>();
      sortedKeys.addAll(keyWords);
      for (String kw : sortedKeys) {
         //String first = kw.substring(0,kw.indexOf(" "));
        // output = output + "<li><a href = \""+first+"\">"+kw+"</a></li>\n";
         output = output + "<li><a href = \""+kw+"\">"+kw+"</a></li>\n";
      }
      return output;
   }

	private String showKeyWords() {
      logger.finest("[Web]: Generating List of KeyWords");
		Collection<OtiNanaiMemory> allOMs = onl.getMemoryMap().values();
		String output = new String("<html><body>");
      ArrayList<String> kws = new ArrayList<String>();
      for (OtiNanaiMemory onm : allOMs ) {
         kws.add(onm.getKeyWord());//+" "+onm.getFiveMinCount()+" "+onm.getThirtyMinCount());
      }
      output = output + listKeyWords(kws);
		output = output + "</body></html>";
      return output;
	}

   /**
    * Changes milliseconds into date with format: MM/dd/YY HH:mm:ss
    * @param   millisecs the millisecs to change
    * @return  String containing the date.
    */
   private String calcDate(String millisecs) {
      SimpleDateFormat date_format = new SimpleDateFormat("'new Date('yyyy,MM-1,dd,HH,mm,ss')'");
      Date resultdate = new Date(Long.parseLong(millisecs));
      return date_format.format(resultdate);
   }


	private OtiNanaiProcessor onp;
	private OtiNanaiListener onl;
	private HashMap<String,SomeRecord> dataMap;
	private int port;
	private ServerSocket listenSocket;
	private Logger logger;
   private static int MAX_LOG_OUTPUT=20;
   private OtiNanaiCache onc;
}
