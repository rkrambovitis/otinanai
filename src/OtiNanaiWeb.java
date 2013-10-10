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
		port = lp;
		ServerSocket listenSocket = new ServerSocket(port);
		logger = l;
		logger.finest("[Web]: New OtiNanaiWeb Initialized");
	}

	public OtiNanaiWeb(OtiNanaiListener o, OtiNanaiCache oc, ServerSocket ss, Logger l) {
		onl = o;
      onc = oc;
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
                  path = Paths.get("web/index.html");
                  data = Files.readAllBytes(path);
                  sendToClient(data, "text/html; charset=utf-8", true, connectionSocket);
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
               case " jquery.flot.selection.js ":
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
                  String text = showKeyWords(requestMessageLine);
                  logger.fine("[Web]: got text, sending to client");
						sendToClient(text.getBytes(), "text/html; charset=utf-8", false, connectionSocket);
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

   private String toGraph(KeyWordTracker kwt, short type, long time) {
      logger.finest("[Web]: Generating graph from KeyWordTracker: "+kwt.getKeyWord() +" type: "+type);
		String output = new String("\n");
      SomeRecord sr;
      LinkedList<String> data = new LinkedList<String>();
      data = kwt.getMemory();
      long now=System.currentTimeMillis();
      for (String dato : data) {
         String[] twowords = dato.split("\\s");
         if (type != OtiNanai.GRAPH_FULL && (now - Long.parseLong(twowords[0])) > time)
            break;
         output = output + "[" + twowords[0] + "," + twowords[1] + "],\n";
      }
      return output;
   }

   private String timeGraphHeadString(ArrayList<String> keyList, short type, long time) {
      ArrayList<KeyWordTracker> graphMe = new ArrayList<KeyWordTracker> ();
//		HashMap<String,OtiNanaiMemory> allKWs = onl.getMemoryMap();
//      HashMap<String,KeyWordTracker> allKWs = onl.getTrackerMap();
      LLString kwtList = onl.getKWTList();

      TreeSet<String> sortedKeys = new TreeSet<String>();
      sortedKeys.addAll(keyList);

      for (String key : sortedKeys) {
         key=key.toLowerCase();
         if (kwtList.contains(key)) {
            graphMe.add(onl.getKWT(key));
         }
      }
      return timeGraphHead(graphMe, type, time);
   }

   private String timeGraphHead(ArrayList<KeyWordTracker> kws, short type, long time) {
      String output = new String("");
      int i=0;
      output = output + commonHTML(OtiNanai.FLOT);

      if (type == OtiNanai.GRAPH_PREVIEW)
         output = output + commonHTML(OtiNanai.FLOT_PREVIEW);
      else 
         output = output + commonHTML(OtiNanai.FLOT_MERGED);

      output = output+ commonHTML(OtiNanai.JS)
         + "var datasets = {\n";

      for (KeyWordTracker kwt : kws) {
         output = output + "\"" + kwt.getKeyWord().replaceAll("\\.","_") + "\": {\n"
            + "label: \""+kwt.getKeyWord()+" = 000.000 k \",\n";

         if (type == OtiNanai.GRAPH_MERGED_AXES) 
            output = output + "yaxis: "+ ++i +",\n";

         output = output + "data: ["
            + toGraph(kwt, type, time)
            + "]},\n\n";
      }
      output = output + "};\n"
         + commonHTML(OtiNanai.ENDJS);
      return output;
   }



   private String timeGraphBody(ArrayList<String> keyWords, short type) {
      String output = new String();
      if (keyWords.size() == 0) {
         return new String("No KeyWords");
      }
      TreeSet<String> sortedKeys = new TreeSet<String>();
      sortedKeys.addAll(keyWords);
      if (type == OtiNanai.GRAPH_MERGED || type == OtiNanai.GRAPH_MERGED_AXES || type == OtiNanai.GRAPH_FULL) {
         output = output
            + "<div>\n"
            + "\t<div id=\"placeholder\" class=\"mergedGraph\"></div>\n"
            + "</div>\n"
            + "<div class=\"clearfix\">\n"
            + "\t<div id=\"overview\" class=\"previewGraph\"></div>\n"
            + "\t<div id=\"choicesDiv\" class=\"checkList\">\n"
            + "\t\t<p id=\"choices\"></p>\n"
            + "\t</div>\n"
            + "</div>\n";
      } else {
         for (String kw : sortedKeys) {
            output = output 
               + "<div class=\"wrapper clearfix\">\n"
               + "\t<li><a href = \""+kw+"\">"+kw+"</a></li>\n"
               + "\t<div id=\"" + kw.replaceAll("\\.","_") + "\" class=\"previewGraph\"></div>\n"
               + "</div>\n";
         }
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
         + commonHTML(OtiNanai.ENDHEAD)
         + commonHTML(OtiNanai.GPSCRIPT)
         + "<li><a href=\""+oldKeys + " --sa\">Show All (slow) (--sa) "+kws.size()+"</a></li>\n";
         
      for (String key : sortedKeys.keySet()) {
         output = output + "<li><a href=\""+oldKeys + " +^"+key+"\">"+key+" "+sortedKeys.get(key)+"</a></li>\n";
      }
      output = output + commonHTML(OtiNanai.ENDBODY);
      return output;
   }
   
   private String commonHTML(short out) {
      if (out == OtiNanai.HEADER) {
         String op = new String("<html><head>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"otinanai.css\" />\n");
         op = op + "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'/>";
         return op;
      } else if (out == OtiNanai.GOOGLE) {
         return new String("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>\n");
      } else if (out == OtiNanai.ENDHEAD) {
         return new String("</head><body>\n");
      } else if (out == OtiNanai.GPSCRIPT) {
         String op = new String("<script>\n\tdocument.body.addEventListener('click', function (event) {\n"
            + "\t\tif (event.target.nodeName !== 'A') {\n"
            + "\t\t\treturn false;\n\t\t}\n"
            + "\t\t(window.parent || window.opener).onReceive(event.target);\n\t}, false);\n"
            + "</script>\n");
         return op;
      } else if (out == OtiNanai.ENDBODY) {
         return new String("</body></html>\n");
      } else if (out == OtiNanai.FLOT) {
         String op = new String("<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.js\"></script>\n");
         op = op + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.js\"></script>\n"
            + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.time.js\"></script>\n"
            + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.crosshair.js\"></script>\n"
            + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.selection.js\"></script>\n"
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


	private String showKeyWords(String input) {
      String op = onc.getCached(input);
      if (op != null) {
         logger.fine("[Web]: Cached");
         return op;
      }

      String [] keyList = input.split("[ ,]|%20");
      logger.fine("[Web]: Searching for keywords");
		//Collection<KeyWordTracker> allKWTs = onl.getTrackerMap().values();
      LLString allKWTs = new LLString();
      allKWTs.addAll(onl.getKWTList());
      ArrayList<String> kws = new ArrayList<String>();

      String firstChar = new String();
      String secondChar = new String();
      String lastChar = new String();
      String rest;
      boolean wipe = false;
      boolean force = false;
      boolean alarm = false;
      boolean showAll = false;
      boolean showAlarms = false;
      short graphType = OtiNanai.GRAPH_PREVIEW;
      long time = OtiNanai.PREVIEWTIME;

      for (String word : keyList) {
         boolean removeKW = false;
         boolean exclusiveKW = false;
         boolean startsWithKW = false;
         boolean endsWithKW = false;
         boolean matched = false;
         boolean setTime = false;
         switch (word) {
            case "":
               matched = true;
               break;
            case "--showall":
            case "--sa":
            case "--show":
               showAll = true;
               matched = true;
               break;
            case "--delete":
               wipe = true;
               matched = true;
               break;
            case "--force":
               force = true;
               matched = true;
               break;
            case "--merge":
            case "--m":
            case "--combine":
               graphType = OtiNanai.GRAPH_MERGED;
               matched = true;
               break;
            case "--ma":
            case "--am":
               graphType = OtiNanai.GRAPH_MERGED_AXES;
               matched = true;
               break;
            case "--alarms":
            case "--alerts":
               showAlarms = true;
               matched = true;
               logger.info("[Web]: Showing Alarms");
               break;
         }
         if (matched)
            continue;

         word = word.replaceAll("%5E", "^");
         word = word.replaceAll("%24", "$");
         word = word.replaceAll("%40", "@");
         logger.fine("[Web]: word is: \""+word+"\"");
         firstChar = word.substring(0,1);
         rest = word.replaceAll("[\\+\\-\\^\\$\\@]", "");

         if (rest.length() == 0)
            continue;
         if (word.length() > 1) {
            secondChar = word.substring(1,2);
            lastChar = word.substring(word.length()-1);
         }
         if (firstChar.equals("-"))
            removeKW = true;
         else if (firstChar.equals("+"))
            exclusiveKW = true;
         else if (firstChar.equals("@"))
            setTime = true;
         if (firstChar.equals("^") || secondChar.equals("^"))
            startsWithKW = true;
         if (lastChar.equals("$"))
            endsWithKW = true;

         logger.info("[Web]: removeKW: "+removeKW+" exclusiveKW: "+exclusiveKW+ " startsWithKW: "+startsWithKW+" endsWithKW: "+endsWithKW);
         ArrayList<String> kwsClone = new ArrayList<String>();

         kwsClone.addAll(kws);
         logger.fine("[Web]: Current kws.size(): " + kws.size());
         for (String key : kwsClone) {
            if (removeKW) {
               if (startsWithKW && endsWithKW) {
                  if (key.startsWith(rest) && key.endsWith(rest))
                     kws.remove(key);
               } else if (startsWithKW) {
                  if (key.startsWith(rest))
                     kws.remove(key);
               } else if (endsWithKW) {
                  if (key.endsWith(rest))
                     kws.remove(key);
               } else {
                  if (key.contains(rest)) {
                     kws.remove(key);
                     logger.info("Removing "+key);
                  }
               }
            } else if (exclusiveKW) {
               if (startsWithKW && endsWithKW) {
                  if (!key.startsWith(rest) && !key.endsWith(rest))
                     kws.remove(key);
               } else if (startsWithKW) {
                  if (!key.startsWith(rest))
                     kws.remove(key);
               } else if (endsWithKW) {
                  if (!key.endsWith(rest))
                     kws.remove(key);
               } else {
                  if (!key.contains(rest))
                     kws.remove(key);
               }
            } else if (setTime) {
               try {
                  time = 3600000 * Long.parseLong(rest);
               } catch (NumberFormatException nfe) {
                  logger.severe("[Web]: Not a valid number\n"+nfe);
               }
            }
         }
         if (removeKW || exclusiveKW) 
            continue;

         if (startsWithKW && endsWithKW) {
            for (String kw : allKWTs ) {
               if (kw.startsWith(rest) && kw.endsWith(rest) && !kws.contains(kw))
                  kws.add(kw);
            }
         } else if (startsWithKW) {
            for (String kw : allKWTs ) {
               if (kw.startsWith(rest) && !kws.contains(kw))
                  kws.add(kw);
            }
         } else if (endsWithKW) {
            for (String kw : allKWTs ) {
               if (kw.endsWith(rest) && !kws.contains(kw)) 
                  kws.add(kw);
            }
         } else {
            for (String kw : allKWTs ) {
               if ((kw.contains(word) || rest.equals("*")) && !kws.contains(kw)) 
                  kws.add(kw);
            }
         }
      }
      if (showAlarms) {
         long alarmLife = onl.getAlarmLife();
         long timeNow = System.currentTimeMillis();
         long lastAlarm;
         logger.info("[Web]: kws.size() = "+kws.size());
         for (String kw : allKWTs ) {
            lastAlarm=onl.getAlarm(kw);
            if (lastAlarm == 0L || (timeNow - lastAlarm) > alarmLife) {
               logger.info("[Web]: No alarm for "+kw+ " - Removing");
               kws.remove(kw);
            } else {
               logger.info("[Web]: Alarm for "+kw);
            }
         }
      } else if (wipe && force) {
         logger.info("[Web]: --delete received with --force. Deleting matched keywords Permanently");
         KeyWordTracker kwt;
         String delOP = new String("RIP Data for keywords:");
         for (String todel : kws) {
            logger.info("[Web]: Deleting data for " + todel);
            delOP = delOP + "<li>"+todel+"</li>";
            onl.deleteKWT(todel);
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
         + timeGraphHeadString(kws, graphType, time)
         + commonHTML(OtiNanai.ENDHEAD)
         + commonHTML(OtiNanai.GPSCRIPT)
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


	private OtiNanaiListener onl;
	private int port;
	private ServerSocket listenSocket;
	private Logger logger;
   private OtiNanaiCache onc;
}
