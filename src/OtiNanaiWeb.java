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
            boolean showKeyWords=false;
            boolean mergeKeyWords=false;
            boolean draw=false;
            Path path;
            byte[] data;
				switch (requestMessageLine) {
					case " / ":
					case "  ":
                  logger.info("[Web]: Sending default blank webpage");
						String bogus = commonHTML(OtiNanai.HEADER) 
                     + commonHTML(OtiNanai.ENDHEAD)
                     +"<h3>Robert's random piece of junk.</h3>"
                     +"<hr>Keys:"
                     +"<li><a href=\"A\">A</a> : show alarms"
                     +"<li><a href=\"*\">*</a> : show all keywords"
                     +"<li>word1 word2, +word3, -word4 : search keywords"
                     + commonHTML(OtiNanai.ENDBODY);
						sendToClient(bogus.getBytes(), "text/html", false, connectionSocket);
						break;
					case " favicon.ico ":
					case " otinanai.css ":
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
						String[] request = requestMessageLine.split("[ ,]|%20");
                  String rest = new String("");
						for (String word : request) {
                     switch (word) {
                        case "":
                           logger.info("[Web]: Skipping blank word");
                           break;
                        case "a":
                        case "A":
                           logger.info("[Web]: getAlarms matched");
                           alarms=true;
                           break;
                        default:
                           logger.info("[Web]: Draw matched");
                           draw = true;
                           if (rest.equals("")) {
                              rest = word;
                           } else {
                              rest = rest + " " + word;
                           }
                           break;
							}
						}
						String text = new String();
                  if (alarms) {
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
      /*
      if (type == OtiNanai.GRAPH_FULL) {
         data = onm.getMemory();
      } else if (type == OtiNanai.GRAPH_PREVIEW || type == OtiNanai.GRAPH_MERGED) {
         data = onm.getPreview();
      }       
      */
      if (type == OtiNanai.GRAPH_MERGED) {
         for (String dato : data) {
            String[] twowords = dato.split("\\s");
            if ((now - Long.parseLong(twowords[0])) > OtiNanai.PREVIEWTIME)
               break;
            output = output + "[" + twowords[0] + "," + twowords[1] + "],\n";
         }
      } else {
         try {
            if (data.size() == 0 ) {
               output = output + "[new Date(2013,07,30,0,0,0), 0],\n";
            } else {
               for (String dato : data) {
                  String[] twowords = dato.split("\\s");
                  output = output + "[" + calcDate(twowords[0]) + "," + twowords[1] + "],\n";
               }
            }
         } catch (NullPointerException npe) {
            logger.severe("[Web]: "+npe);
            output = output + "[new Date(2013,07,30,0,0,0), 0],\n";
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
      if (type == OtiNanai.GRAPH_MERGED) {
         output = output + commonHTML(OtiNanai.FLOT)
            + commonHTML(OtiNanai.JS)
            + "$(function() {\nvar datasets = {\n";
         for (OtiNanaiMemory onm : kws) {
            output = output + "\""+onm.getKeyWord()+"\": {\n"
               + "label: \""+onm.getKeyWord()+"\",\n"
               + "data: ["
               + toGraph(onm, type)
               + "]},\n\n";
         }
         output = output + "};\n"
            +"var i = 0;\n"
            +"$.each(datasets, function(key, val) {val.color = i;++i;});\n\n"

            +"var choiceContainer = $(\"#choices\");\n\n"

            +"$.each(datasets, function(key, val) {"
            +"choiceContainer.append(\"<br/><input type='checkbox' name='\" + key + \"' checked='checked' id='id\" + key + \"'></input>\" + \"<label for='id\" + key + \"'>\" + val.label + \"</label>\");"
            +"});\n\n"

            +"choiceContainer.find(\"input\").click(plotAccordingToChoices);"
            +"\n\n"

            +"var legends = $(\"#placeholder .legendLabel\");\n"
            +"legends.each(function () {$(this).css('width', $(this).width());});\n\n"

            +"var updateLegendTimeout = null;\n"
            +"var latestPosition = null;\n\n"

            +"var myplot = null;"
            +"updatePlot(datasets);"

            +"function updateLegend() {\n"
            +"updateLegendTimeout = null;\n"
            +"var pos = latestPosition;\n"
            +"var axes = myplot.getAxes();\n"
            +"if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max ||"
            +"pos.y < axes.yaxis.min || pos.y > axes.yaxis.max) {return;}\n"
            +"var i, j, dataset = myplot.getData();\n"
            +"for (i = 0; i < dataset.length; ++i) {\n"
            +"var series = dataset[i];\n"
            +"for (j = 0; j < series.data.length; ++j) {\n"
            +"if (series.data[j][0] > pos.x) {break;}}\n"
            +"var y,p1 = series.data[j - 1],p2 = series.data[j];\n"
            +"if (p1 == null) {y = p2[1];} else if (p2 == null) {y = p1[1];} else {y = p1[1] + (p2[1] - p1[1]) * (pos.x - p1[0]) / (p2[0] - p1[0]);}\n"
            +"legends.eq(i).text(series.label.replace(/=.*/, \"= \" + y.toFixed(2)));}};\n\n"

            +"$(\"#placeholder\").bind(\"plothover\",  function (event, pos, item) {latestPosition = pos;\n"
            +"if (!updateLegendTimeout) {updateLegendTimeout = setTimeout(updateLegend, 50);}});\n\n"

            +"function updatePlot(data) {\n"
            +"if (data.length > 0) {\n"
            +"currentData=data;\n"
            +"myplot=$.plot(\"#placeholder\", data, {\n"
            +"legend: { position: \"sw\" },\n"
            +"xaxis: {mode: \"time\", tickDecimals: 0},\n"
            +"series: {lines: {show: true}},\n"
            +"crosshair: {mode: \"x\"},"
            +"grid: {hoverable: true,autoHighlight: false}}\n"
            +");}}\n\n"

            +"function plotAccordingToChoices() {\n"
            +"var data = [];\n"
            +"choiceContainer.find(\"input:checked\").each(function () {\n"
            +"var key = $(this).attr(\"name\");\n"
            +"if (key && datasets[key]) {\n"
            +"data.push(datasets[key]);}});\n"
            +"updatePlot(data);}\n\n"

            +"plotAccordingToChoices();\n"
            +"});\n\n"
            + commonHTML(OtiNanai.ENDJS);
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
      if (graphMe.size() == 0) {
      } else {
         String output = onc.getCached(fullString);
         if (output == null) {
            logger.fine("[Web]: Not cached, will generate \"" + fullString+"\"");
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
   }

	private String getAlarms() {
      logger.finest("[Web]: Generating Alarms Output");
		Collection<OtiNanaiMemory> allOMs = onl.getMemoryMap().values();
      ArrayList<String> kws = new ArrayList<String> ();
      for (OtiNanaiMemory onm : allOMs ) {
         if (onm.getAlarm(System.currentTimeMillis())) {
            kws.add(onm.getKeyWord());
         }
      }

      String output = commonHTML(OtiNanai.HEADER) 
         + timeGraphHeadString(kws, OtiNanai.GRAPH_MERGED)
         + commonHTML(OtiNanai.ENDHEAD)
         + timeGraphBody(kws, OtiNanai.GRAPH_MERGED)
         + commonHTML(OtiNanai.ENDBODY);
      return output;
   };

   /*
      <div class="fullGraph">
      <div id="placeholder" class="previewGraph" style="float:left; width:675px; height: 30%;"></div>
      <p id="choices" style="float:right; width:135px;"></p>
      </div>
    */

   private String timeGraphBody(ArrayList<String> keyWords, short type) {
      String output = new String();
      if (keyWords.size() == 0) {
         return new String("No KeyWords");
      }
      TreeSet<String> sortedKeys = new TreeSet<String>();
      sortedKeys.addAll(keyWords);
      if (type == OtiNanai.GRAPH_MERGED) {
         output = output
            + "<div>"
            + "<div id=\"placeholder\" class=\"mergedGraph\"></div>\n"
            + "<p id=\"choices\" style=\"float:left;\"></p>"
            + "</div>";
      } else {
         for (String kw : sortedKeys) {
            output = output + "<li><a href = \""+kw+"\">"+kw+"</a></li>\n";
            output = output + "<div id=\""+kw+"\" class=\"previewGraph\"></div>\n";
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
            + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.crosshair.js\"></script>\n";

         return op;
      } else if ( out == OtiNanai.JS) {
         return new String("<script type=\"text/javascript\">\n");
      } else if (out == OtiNanai.ENDJS) {
         return new String ("</script>\n");
      }
      return new String();
   }

	private String showKeyWords(String[] keyList) {
      logger.fine("[Web]: Searching for keywords");
		Collection<OtiNanaiMemory> allOMs = onl.getMemoryMap().values();
      ArrayList<String> kws = new ArrayList<String>();
      boolean matched;
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

      logger.fine("[Web]: removing \"+/-\" keywords");
      String firstChar;
      String rest;
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
      }
      if (kws.size() > OtiNanai.MAXPERPAGE) {
         logger.info("[Web]: Exceeded MAXPERPAGE: "+ kws.size() + " > " +OtiNanai.MAXPERPAGE);
         return kwTree(kws, keyList);
      }
		String output = commonHTML(OtiNanai.HEADER) 
         + timeGraphHeadString(kws, OtiNanai.GRAPH_MERGED)
         + commonHTML(OtiNanai.ENDHEAD)
         + timeGraphBody(kws, OtiNanai.GRAPH_MERGED)
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
