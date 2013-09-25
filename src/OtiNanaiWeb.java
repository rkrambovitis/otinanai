package gr.phaistosnetworks.admin.otinanai;

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
					logger.warning("[Web]: "+npe);
					continue;
				}
				boolean alarms=false;
				boolean graph=false;
            boolean timeGraph=false;
            boolean mergeKeyWords=false;
            Path path;
            byte[] data;
				switch (requestMessageLine) {
					case " / ":
					case "  ":
                  logger.info("[Web]: Sending default webpage");
                  /*
						String bogus = commonHTML(OtiNanai.HEADER) 
                     + commonHTML(OtiNanai.ENDHEAD)
                     +"<h3>Robert's random piece of junk.</h3>"
                     +"<hr>Keys:"
                     +"<li><a href=\"A\">A</a> : show alarms"
                     +"<li><a href=\"*\">*</a> : show all keywords"
                     +"<li>word1 word2, +word3, -word4 : search keywords"
                     + commonHTML(OtiNanai.ENDBODY);
						sendToClient(bogus.getBytes(), "text/html", false, connectionSocket);
                  */
                  path = Paths.get("web/index.html");
                  data = Files.readAllBytes(path);
                  sendToClient(data, "text/html", true, connectionSocket);
						break;
					case " favicon.ico ":
					case " otinanai.css ":
               case " otinanai.flot.common.js ":
               case " otinanai.flot.merged.js ":
               case " otinanai.flot.preview.js ":
					case " jquery.js ":
					case " jquery.flot.js ":
					case " jquery.flot.time.js ":
					case " jquery.flot.crosshair.js ":
                  String noSpaces = requestMessageLine.replaceAll(" ","");
                  logger.info("[Web]: Sending "+noSpaces);
						path = Paths.get("web/"+noSpaces);
						data = Files.readAllBytes(path);
                  if (noSpaces.endsWith(".ico")) {
                     sendToClient(data, "image/x-icon", true, connectionSocket);
                  } else if (noSpaces.endsWith(".css")) {
                     sendToClient(data, "text/css", true, connectionSocket);
                  } else if (noSpaces.endsWith(".js")) {
                     sendToClient(data, "application/x-javascript", true, connectionSocket);
                  }
						break;
					default:
						//String[] request = requestMessageLine.split("[ ,]|%20");
                  String text = showKeyWords(requestMessageLine);
                  logger.fine("[Web]: got text, sending to client");
						sendToClient(text.getBytes(), "text/html", false, connectionSocket);
						connectionSocket.close();
				}
			}
		} catch (IOException ioe) {
			logger.severe("[Web]: "+ioe);
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
			logger.severe("[Web]: "+ioe);
			return false;
		}
	}

   private String toGraph(OtiNanaiMemory onm, short type) {
      logger.finest("[Web]: Generating graph from OtiNanaiMemory: "+onm.getKeyWord() +" type: "+type);
		String output = new String("\n");
      SomeRecord sr;
      LinkedList<String> data = new LinkedList<String>();
      data = onm.getMemory();
      long now=System.currentTimeMillis();
 //     if (type == OtiNanai.GRAPH_MERGED || type == OtiNanai.GRAPH_MERGED_AXES || type == OtiNanai.GRAPH_PREVIEW) {
         for (String dato : data) {
            String[] twowords = dato.split("\\s");
            if (type != OtiNanai.GRAPH_FULL && (now - Long.parseLong(twowords[0])) > OtiNanai.PREVIEWTIME)
               break;
            output = output + "[" + twowords[0] + "," + twowords[1] + "],\n";
         }
  /*    } else {
         try {
            if (data.size() == 0 ) {
               output = output + "[new Date(2013,07,30,0,0,0), 0],\n";
            } else {
               for (String dato : data) {
                  String[] twowords = dato.split("\\s");
                  if (type == OtiNanai.GRAPH_PREVIEW && (now - Long.parseLong(twowords[0])) > OtiNanai.PREVIEWTIME)
                     break;
                  output = output + "[" + calcDate(twowords[0]) + "," + twowords[1] + "],\n";
               }
            }
         } catch (NullPointerException npe) {
            logger.severe("[Web]: "+npe);
            output = output + "[new Date(2013,07,30,0,0,0), 0],\n";
         }
      }
      */
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
            logger.severe("[Web]: "+npe);
         }
         if (c >= OtiNanai.MAX_LOG_OUTPUT) {
            break;
         }
         c++;
      }
      output = output + "</div>";
		return output;
	}

   private String timeGraphHeadString(ArrayList<String> keyList, short type) {
      ArrayList<OtiNanaiMemory> graphMe = new ArrayList<OtiNanaiMemory> ();
		HashMap<String,OtiNanaiMemory> allKWs = onl.getMemoryMap();

      TreeSet<String> sortedKeys = new TreeSet<String>();
      sortedKeys.addAll(keyList);

      for (String key : sortedKeys) {
         key=key.toLowerCase();
         if (allKWs.containsKey(key)) {
            graphMe.add(allKWs.get(key));
         }
      }
      return timeGraphHead(graphMe, type);
   }

   private String timeGraphHead(ArrayList<OtiNanaiMemory> kws, short type) {
      String output = new String("");
      int i=0;
//      if (type == OtiNanai.GRAPH_MERGED || type == OtiNanai.GRAPH_MERGED_AXES) {
      output = output + commonHTML(OtiNanai.FLOT);

      if (type == OtiNanai.GRAPH_PREVIEW || type == OtiNanai.GRAPH_FULL)
         output = output + commonHTML(OtiNanai.FLOT_PREVIEW);
      else 
         output = output + commonHTML(OtiNanai.FLOT_MERGED);

      output = output+ commonHTML(OtiNanai.JS)
         + "var datasets = {\n";

      for (OtiNanaiMemory onm : kws) {
         output = output + "\"" + onm.getKeyWord().replaceAll("\\.","_") + "\": {\n"
            + "label: \""+onm.getKeyWord()+" = 000.000 k \",\n";

         if (type == OtiNanai.GRAPH_MERGED_AXES) 
            output = output + "yaxis: "+ ++i +",\n";

         output = output + "data: ["
            + toGraph(onm, type)
            + "]},\n\n";
      }
      output = output + "};\n"
         + commonHTML(OtiNanai.ENDJS);
         /*
      } else {
         output = output + commonHTML(OtiNanai.GOOGLE);

         for (OtiNanaiMemory onm : kws) {
            output = output + commonHTML(OtiNanai.JS);
            if (type == OtiNanai.GRAPH_FULL) {
               output = output + "google.load(\"visualization\", \"1\", {packages:[\"annotatedtimeline\"]});\n";
            } else {
               output = output + "google.load(\"visualization\", \"1\", {packages:[\"corechart\"]});\n";
            }
            output = output + "google.setOnLoadCallback(drawChart);\n"
               + "function drawChart() {\n"
               + "var data = new google.visualization.DataTable();\n"
               + "data.addColumn('datetime', 'Date');\n"
               + "data.addColumn('number', '"+onm.getKeyWord()+"');\n";
            output = output + "data.addRows(["
               + toGraph(onm, type)
               + "]);\n";

            if (type == OtiNanai.GRAPH_FULL) {
               output = output + "var options = { title: \""+onm.getKeyWord()+"\", hAxis: {direction: \"-1\" }};\n"
                  + "var chart = new google.visualization.AnnotatedTimeLine(document.getElementById('"+onm.getKeyWord()+"'));\n";
            } else {
               output = output + "var options = { title: \""+onm.getKeyWord()+"\", hAxis: {direction: \"1\" }};\n"
                  + "var chart = new google.visualization.AreaChart(document.getElementById('"+onm.getKeyWord()+"'));\n";
            }
            output = output + "chart.draw(data, options);\n"
               + "}\n"
               + commonHTML(OtiNanai.ENDJS);
         }
      }
      */
      return output;
   }



   private String timeGraphBody(ArrayList<String> keyWords, short type) {
      String output = new String();
      if (keyWords.size() == 0) {
         return new String("No KeyWords");
      }
      TreeSet<String> sortedKeys = new TreeSet<String>();
      sortedKeys.addAll(keyWords);
      if (type == OtiNanai.GRAPH_MERGED || type == OtiNanai.GRAPH_MERGED_AXES) {
         output = output
            + "<div>"
            + "<div id=\"placeholder\" class=\"mergedGraph\"></div>\n"
            + "<p id=\"choices\" style=\"float:left;\"></p>"
            + "</div>";
      } else {
         for (String kw : sortedKeys) {
            output = output + "<li><a href = \""+kw+"\">"+kw+"</a></li>\n";
            output = output + "<div id=\"" + kw.replaceAll("\\.","_") + "\" class=\"previewGraph\"></div>\n";
         }
      }
      return output;
   }

   private String timeGraphBody(ArrayList<OtiNanaiMemory> kws) {
      String output = new String("");
      for (OtiNanaiMemory onm : kws) {
         output = output + "<div id=\""+onm.getKeyWord()+"\" class=\"myGraph\"></div><br>\n";
         output = output + drawText(onm.getKeyWord());
      }
      return output;
   }


   private TreeMap<String, Integer> subTree(ArrayList<String> kws, String start) {
      TreeMap<String, Integer> sortedKeys = new TreeMap<String, Integer>();
      String portion = new String();
      for (String kw : kws) {

         if (!start.equals("")) {
            if (kw.startsWith(start))
               kw = kw.replaceFirst(start, "");

            if (kw.startsWith("."))
               kw = kw.substring(1);
         }

         if (kw.contains(".")) {
            portion = kw.substring(0, kw.indexOf("."));
         } else {
            portion = kw;
         }
         if (!start.equals("")) 
            portion = start + "." + portion;

         int sofar = 0;
         if (sortedKeys.containsKey(portion)) {
            sofar = sortedKeys.get(portion);
         } 
         sortedKeys.put(portion, ++sofar);
      }
      return sortedKeys;
   }

   private String kwTree(ArrayList<String> kws, String[] existingKeyWords) {
      TreeMap<String, Integer> sortedKeys = new TreeMap<String, Integer>();
      sortedKeys = subTree(kws, "");
      while (sortedKeys.size() == 1) {
         sortedKeys = subTree(kws, sortedKeys.firstKey());
      }
      String oldKeys = new String();
      for (String foo : existingKeyWords) {
         oldKeys = oldKeys + foo + " ";
      }
      oldKeys = oldKeys.substring(0,oldKeys.length()-1);

      String output = commonHTML(OtiNanai.HEADER) 
         + commonHTML(OtiNanai.ENDHEAD);
      for (String key : sortedKeys.keySet()) {
         output = output + "<li><a href=\""+oldKeys + " +"+key+"\">"+key+" "+sortedKeys.get(key)+"</a></li>\n";
      }
      output = output + commonHTML(OtiNanai.ENDBODY);
      return output;
   }
   
   private String commonHTML(short out) {
      if (out == OtiNanai.HEADER) {
         return new String("<html><head>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"otinanai.css\" />\n");
      } else if (out == OtiNanai.GOOGLE) {
         return new String("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>\n");
      } else if (out == OtiNanai.ENDHEAD) {
         return new String("</head><body>\n");
      } else if (out == OtiNanai.ENDBODY) {
         return new String("</body></html>\n");
      } else if (out == OtiNanai.FLOT) {
         String op = new String("<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.js\"></script>\n");
         op = op + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.js\"></script>\n"
            + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.time.js\"></script>\n"
            + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.crosshair.js\"></script>\n"
            + "<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.flot.common.js\"></script>\n";
         return op;
      } else if (out == OtiNanai.FLOT_MERGED) {
         return new String("<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.flot.merged.js\"></script>\n");
      } else if (out == OtiNanai.FLOT_PREVIEW) {
         return new String("<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.flot.preview.js\"></script>\n");
      } else if ( out == OtiNanai.JS) {
         return new String("<script type=\"text/javascript\">\n");
      } else if (out == OtiNanai.ENDJS) {
         return new String ("</script>\n");
      }
      return new String();
   }


      /*
   private String draw(String[] keyList) {
      logger.fine("[Web]: Drawing output for keywords");
		HashMap<String,OtiNanaiMemory> allKWs = onl.getMemoryMap();
      ArrayList<OtiNanaiMemory> graphMe = new ArrayList<OtiNanaiMemory> ();
      String fullString = new String();

      if (graphMe.size() == 0) {
      } else {
         String output = onc.getCached(fullString);
         if (output == null) {
            output = commonHTML(OtiNanai.HEADER) 
               + timeGraphHead(graphMe, OtiNanai.GRAPH_FULL)
               + commonHTML(OtiNanai.ENDHEAD)
               + timeGraphBody(graphMe)
               + commonHTML(OtiNanai.ENDBODY);
            onc.cache(fullString, output);
            return output;
         } else {
            logger.fine("[Web]: Cached data for \"" + fullString + "\"");
            return output;
         }
      }
      return showKeyWords(keyList);
   }*/


	private String showKeyWords(String input) {
      String op = onc.getCached(input);
      if (op != null) {
         logger.fine("[Web]: Cached");
         return op;
      }

      String [] keyList = input.split("[ ,]|%20");
      logger.fine("[Web]: Searching for keywords");
		Collection<OtiNanaiMemory> allOMs = onl.getMemoryMap().values();
      ArrayList<String> kws = new ArrayList<String>();
      if (keyList.length == 2 && keyList[1].equals("a")){
         logger.fine("[Web]: Searching for alarms");
         for (OtiNanaiMemory onm : allOMs ) {
            if (onm.getAlarm(System.currentTimeMillis())) {
               kws.add(onm.getKeyWord());
            }
         }
      } else {
         for (OtiNanaiMemory onm : allOMs ) {
            for (String word : keyList) {
               if (word.equals("")) {
                  logger.finest("[Web]: Skipping blank word (2)");
                  continue;
               }
               logger.fine("[Web]: Searching for keywords containing: \""+word+"\"");
               String test = onm.getKeyWord();
               if (test.contains(word) || word.equals("*")) {
                  logger.fine("[Web]: : Matched: "+test);
                  kws.add(test);
                  break;
               }
            }
         }
      }

      logger.fine("[Web]: removing \"+/-\" keywords");
      String firstChar;
      String rest;
      boolean wipe = false;
      boolean force = false;
      boolean alarm = false;
      boolean showAll = false;
      short graphType = OtiNanai.GRAPH_PREVIEW;
      ArrayList<String> kwsClone = new ArrayList<String>();
      kwsClone.addAll(kws);
      for (String word : keyList) {
         if (word.equals("")) {
            logger.finest("[Web]: Skipping blank word (3)");
            continue;
         }
         firstChar = word.substring(0,1);
         rest = word.substring(1);
         if (firstChar.equals("-")) {
            for (String key : kwsClone) {
               if (key.contains(rest)) {
                  kws.remove(key);
               }
            }
         } else if (firstChar.equals("+")) {
            for (String key : kwsClone) {
               if (!key.contains(rest)) {
                  kws.remove(key);
               }
            }
         }
         switch (word) {
            case "--showall":
            case "--sa":
            case "--show":
            case "--s":
               showAll = true;
               break;
            case "--delete":
               wipe = true;
               break;
            case "--force":
               force = true;
               break;
            case "--merge":
            case "--m":
            case "--combine":
               graphType = OtiNanai.GRAPH_MERGED;
               break;
            case "--axes":
            case "--axis":
            case "--a":
            case "--ma":
            case "--am":
               graphType = OtiNanai.GRAPH_MERGED_AXES;
               break;
         }
      }
      if (wipe && force) {
         logger.info("[Web]: --delete received with --force. Deleting matched keywords Permanently");
         OtiNanaiMemory onm;
         String delOP = new String("RIP Data for keywords:");
         for (String todel : kws) {
            logger.info("[Web]: Deleting data for " + todel);
            delOP = delOP + "<li>"+todel+"</li>";
            onm = onl.getMemoryMap().get(todel);
            onm.delete();
            onl.getMemoryMap().remove(todel);
         }
         return delOP;
      } else if (wipe) {
         logger.fine("[Web]: Wipe command received. Sending Warning");
         String delOP = new String("[WARNING] : You are about to permanently delete the following keywords<br>Add --force to actually delete"); 
         for (String todel : kws) {
            delOP = delOP + "<li><a href=\"" + todel + "\">"+todel+"</a></li>";
         }
         return delOP;
      }
      if (!showAll && kws.size() > OtiNanai.MAXPERPAGE) {
         logger.info("[Web]: Exceeded MAXPERPAGE: "+ kws.size() + " > " +OtiNanai.MAXPERPAGE);
         return kwTree(kws, keyList);
      } else if (kws.size() == 1) {
         graphType = OtiNanai.GRAPH_FULL;
      }
		String output = commonHTML(OtiNanai.HEADER) 
         + timeGraphHeadString(kws, graphType)
         + commonHTML(OtiNanai.ENDHEAD)
         + timeGraphBody(kws, graphType)
         + commonHTML(OtiNanai.ENDBODY);
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
   private OtiNanaiCache onc;
}
