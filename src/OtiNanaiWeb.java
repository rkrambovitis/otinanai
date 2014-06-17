package gr.phaistosnetworks.admin.otinanai;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.logging.*;
import java.text.SimpleDateFormat;
import java.net.URLDecoder;
import java.util.zip.Deflater;


// random comment
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
         String query = new String("*");
         boolean gzip = false;
			while (true) {
				Socket connectionSocket = listenSocket.accept();
				ArrayList<String> results = new ArrayList<String>();
				try {
					inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
               requestMessageLine = inFromClient.readLine();
					while (requestMessageLine != null && !requestMessageLine.equals("")) {
                  if (requestMessageLine.startsWith("GET ")) {
                     query=requestMessageLine.replaceAll("[;\\/]", "").replaceAll("GET|HTTP1.1|\\?q=", "");
                     logger.info("[Web]: GET: \"" + query + "\"");
                  } else if (requestMessageLine.startsWith("Accept-Encoding:")) {
                     if (requestMessageLine.toLowerCase().contains("gzip"))
                        gzip = true;
                     logger.info("[Web]: gzip? " + gzip + " ("+ requestMessageLine.replaceAll("Accept-Encoding: ", "") +" )");
                  }
                  else {
                     logger.fine("[Web]: Ignoring: " + requestMessageLine);
                  }
                  requestMessageLine = inFromClient.readLine();
               }
				} catch (NullPointerException npe) {
					logger.warning("[Web]: "+npe);
					//continue;
				}
            logger.warning("[Web]: about to switch");
				boolean alarms=false;
				boolean graph=false;
            boolean mergeKeyWords=false;
            Path path;
            byte[] data;
				switch (query) {
               case " favicon.ico ":
					case " otinanai.css ":
               case " otinanai.flot.common.js ":
               case " otinanai.flot.merged.js ":
               case " otinanai.flot.preview.js ":
					case " jquery.js ":
					case " jquery.min.js ":
					case " jquery.flot.min.js ":
					case " jquery.flot.time.min.js ":
					case " jquery.flot.crosshair.min.js ":
               case " jquery.gridster.min.js ":
               case " jquery.gridster.js ":
               case " jquery.gridster.css ":
               case " jquery.flot.resize.js ":
               case " jquery.flot.resize.min.js ":
               case " jquery.flot.selection.min.js ":
               case " raphael.min.js ":
               case " raphael.js ":
               case " justgage.min.js ":
               case " justgage.js ":
                  String noSpaces = query.replaceAll(" ","");
                  logger.info("[Web]: Sending "+noSpaces);
						path = Paths.get("web/"+noSpaces);
						data = Files.readAllBytes(path);
                  if (noSpaces.endsWith(".ico")) {
                     sendToClient(data, "image/x-icon", true, connectionSocket, gzip);
                  } else if (noSpaces.endsWith(".css")) {
                     sendToClient(data, "text/css", true, connectionSocket, gzip);
                  } else if (noSpaces.endsWith(".js")) {
                     sendToClient(data, "application/x-javascript", true, connectionSocket, gzip);
                  }
						break;
					default:
                  try {
                     query = URLDecoder.decode(query, "UTF-8");
                  } catch (UnsupportedEncodingException uee) {
                     logger.info("[Web]: Unsupported encoding");
                  }

                  //logger.warning("[Web]: "+query.length()+" \""+query+"\"");
                  query = query.replaceFirst(" ", "");
                  if (query.length() >= 1 )
                     query = query.substring(0,query.length()-1);


                  if (query.equals("") || query.equals("/") )
                     query = "*";

                  boolean cache = true;
                  if (query.contains("--nc") || query.contains("--no-cache"))
                     cache = false;

                  String text = commonHTML(OtiNanai.HEADER) + webTitle(query) + searchBar(query) + showKeyWords(query, cache);

                  logger.fine("[Web]: got text, sending to client");
						sendToClient(text.getBytes(), "text/html; charset=utf-8", false, connectionSocket, gzip);
						//connectionSocket.close();
				}
			}
		} catch (IOException ioe) {
			logger.severe("[Web]: "+ioe);
		}
	}

/*
	private boolean sendToClient(byte[] dato, String contType, boolean cache, Socket connectionSocket) {
		try {
			DataOutputStream dos = new DataOutputStream(connectionSocket.getOutputStream());
         GZIPOutputStream outToClient = new GZIPOutputStream(dos);
			outToClient.writeBytes("HTTP/1.1 200 OK\r\n");
			outToClient.writeBytes("Content-Type: "+contType+"\r\n");
			int numOfBytes = dato.length;
			outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
			if (cache) {
				//outToClient.writeBytes("Expires: Wed, 31 Dec 2014 23:59:59 GMT\r\n");
            outToClient.writeBytes("Cache-Control: max-age=86400\r\n");
         }
			outToClient.writeBytes("\r\n");
			outToClient.write(dato, 0, numOfBytes);
         outToClient.flush();
			connectionSocket.close();
			return true;
		} catch (IOException ioe) {
			logger.severe("[Web]: "+ioe);
			return false;
		}
	}
*/
	private boolean sendToClient(byte[] dato, String contType, boolean cache, Socket connectionSocket, boolean gzip) {
      //logger.warning("[Web]: SendToClient: "+ dato + "gzip? :" + gzip);
		try {
			DataOutputStream dos = new DataOutputStream(connectionSocket.getOutputStream());
			dos.writeBytes("HTTP/1.1 200 OK\r\n");
			dos.writeBytes("Content-Type: "+contType+"\r\n");
			//int numOfBytes = dato.length;
			if (cache) {
				//outToClient.writeBytes("Expires: Wed, 31 Dec 2014 23:59:59 GMT\r\n");
            dos.writeBytes("Cache-Control: max-age=86400\r\n");
         }

         if (gzip) {
            dos.writeBytes("Content-Encoding: deflate\r\n");
            Deflater compressor = new Deflater(Deflater.BEST_SPEED);
            compressor.setInput(dato);
            compressor.finish();
            byte[] littleDato = new byte[dato.length];
            int contentLength = compressor.deflate(littleDato);
            dos.writeBytes("Content-Length: " + contentLength + "\r\n\r\n");
            dos.write(littleDato, 0, contentLength);
         } else {
            dos.writeBytes("Content-Length: " + dato.length + "\r\n\r\n");
            dos.write(dato, 0, dato.length);
         }

			connectionSocket.close();
			return true;
		} catch (IOException ioe) {
			logger.severe("[Web]: "+ioe);
			return false;
		}
	}

   private String[] toGraph(KeyWordTracker kwt, short type, long time) {
      logger.finest("[Web]: Generating graph from KeyWordTracker: "+kwt.getKeyWord() +" type: "+type);
		//String output = new String("\n");
      StringBuilder output = new StringBuilder("\n");
      SomeRecord sr;
      LinkedList<String> data = new LinkedList<String>();
      data = kwt.getMemory();
      long now=System.currentTimeMillis();
      double total=0;
      int samples=0;
      boolean minMaxSet=false;
      boolean lastSet = false;
      float val=0.0f;
      float min=0;
      float max=0;
      float last=0;
      ArrayList<String> allData = new ArrayList<String>();
      TreeSet sortedValues = new TreeSet<String>();
      for (String dato : data) {
         logger.finest("[Web]: Dato is : "+dato);
         String[] twowords = dato.split("\\s");
         if (type != OtiNanai.GRAPH_FULL && (now - Long.parseLong(twowords[0])) > time)
            break;
         //output = output.concat("[").concat(twowords[0]).concat(",").concat( twowords[1]).concat("],\n");
         //output = output + "[" +twowords[0] + "," + twowords[1] + "],\n";
         if (type != OtiNanai.GAGE)
            output = output.append("[").append(twowords[0]).append(",").append( twowords[1]).append("],\n");
         samples++;
         val=Float.parseFloat(twowords[1]);
         total += val;
         if (!lastSet)
            last=val;
         if (!minMaxSet) {
            min=val;
            max=val;
            minMaxSet=true;
         } else {
            if ( val < min ) {
               min=val;
            } else if (val > max) {
               max=val;
            }
         }
         allData.add(twowords[1]);
      }
      int nfth = 0;
      double mean = 0d;

      if ( samples != 0 ) {
         mean = total / samples;
         nfth = (int)(0.95*samples)-1;
         if (nfth < 0)
            nfth = 0;
         Collections.sort(allData);
         //logger.fine("[Web]: 1st:"+allData.get(0)+" last:"+allData.get(samples-1)+" total:"+samples+" 95th:("+nfth+") "+allData.get(nfth));
      } else {
         allData.add("0");
      }
      String[] toReturn = new String[6];
      //toReturn[0]=Double.toString(nfth);
      //toReturn[0]=String.format("%.3f", sortedValues[nfth]);
      //sortedValues=null;
//      toReturn[1]=Double.toString(mean);
      toReturn[0]=String.format("%.3f", min);
      toReturn[1]=String.format("%.3f", max);
      toReturn[2]=String.format("%.3f", mean);
      toReturn[3]=output.toString();
      toReturn[4]=allData.get(nfth);
      toReturn[5]=String.format("%.3f", last);
      //return output;
      return toReturn;
   }

   private String timeGraph(ArrayList<String> keyList, short type, long time) {
      ArrayList<KeyWordTracker> kws = new ArrayList<KeyWordTracker> ();
      LLString kwtList = onl.getKWTList();

      TreeSet<String> sortedKeys = new TreeSet<String>();
      sortedKeys.addAll(keyList);

      for (String key : sortedKeys) {
         key=key.toLowerCase();
         if (kwtList.contains(key)) {
            kws.add(onl.getKWT(key));
         }
      }

      String output;
      String body = new String("");
      String[] graphData;
      int i=0;

      if (type == OtiNanai.GRAPH_GAUGE) {
         output = commonHTML(OtiNanai.GAGE) + commonHTML(OtiNanai.REFRESH);
         for (KeyWordTracker kwt : kws) {
            graphData = toGraph(kwt, type, time);
            String kw = kwt.getKeyWord().replaceAll("\\.","_");
            output = output
               + "<div id=\"" + kw + "\" class=\"gage\"></div>\n"
               + "<script>\n"
               + "\tvar "+kw+" = new JustGage({\n"
               + "\t\tid: \""+kw+"\",\n"
               + "\t\tmin: "+graphData[0]+",\n"
               + "\t\tmax: "+graphData[1]+",\n"
               + "\t\tvalue: "+graphData[5]+",\n"
               + "\t\ttitle: \""+kw+"\",\n"
               + "\t\tlabel: \"\",\n"
               + "\t\tlevelColorsGradient: true\n"
               + "\t});\n"
               + "</script>\n"
               + commonHTML(OtiNanai.ENDHEAD)
               + commonHTML(OtiNanai.ENDBODY);
         }
      } else {
         if (type == OtiNanai.GRAPH_PREVIEW)
            output = commonHTML(OtiNanai.FLOT) + commonHTML(OtiNanai.FLOT_PREVIEW);
         else 
            output = commonHTML(OtiNanai.FLOT) + commonHTML(OtiNanai.FLOT_MERGED);

         output = output+ commonHTML(OtiNanai.JS)
            + "var datasets = {\n";

         for (KeyWordTracker kwt : kws) {
            graphData = toGraph(kwt, type, time);
            String kw = kwt.getKeyWord();

            output = output + "\"" + kw.replaceAll("\\.","_") + "\": {\n"
               + "label: \""+kw+" = 000.000 k \",\n";

            if (type == OtiNanai.GRAPH_MERGED_AXES) 
               output = output + "yaxis: "+ ++i +",\n";

            output = output + "data: ["
              // + toGraph(kwt, type, time)
               + graphData[3]
               + "]},\n\n";

            if (type == OtiNanai.GRAPH_PREVIEW) {
               body = body 
                  + "<div class=\"wrapper clearfix\">\n"
                  + "\t<li><a href = \""+kw+"\">"+kw+"</a> ("+kwt.getType()+") "
                  + "<script>"
                  + "document.write("
                  + "\"min:\" + addSuffix("+graphData[0]+")"
                  + "+\" max:\" + addSuffix("+graphData[1]+")"
                  + "+\" mean:\" + addSuffix("+graphData[2]+")"
                  + "+\" 95%:\"+ addSuffix("+graphData[4]+")"
                  + ");"
                  + "</script>"
                  + "</li>\n"
                  + "\t<div id=\"" + kw.replaceAll("\\.","_") + "\" class=\"previewGraph\"></div>\n"
                  + "</div>\n";
            }
         }

         if (type != OtiNanai.GRAPH_PREVIEW) {
            body = body
               + "<div>\n"
               + "\t<div id=\"placeholder\" class=\"mergedGraph\"></div>\n"
               + "</div>\n"
               /*
               + "<div class=\"clearfix\">\n"
               + "\t<div id=\"overview\" class=\"previewGraph\"></div>\n"
               */
               + "\t<div id=\"choicesDiv\" class=\"checkList\">\n"
               + "\t\t<p id=\"choices\"></p>\n"
               + "\t</div>\n"
               + "</div>\n";
         }

         output = output + "};\n"
            + commonHTML(OtiNanai.ENDJS)
            + commonHTML(OtiNanai.ENDHEAD)
            //+ commonHTML(OtiNanai.GPSCRIPT)
            + body
            + commonHTML(OtiNanai.ENDBODY);
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

      String output = commonHTML(OtiNanai.ENDHEAD)
         //+ commonHTML(OtiNanai.GPSCRIPT)
         + "<li><a href=\""+oldKeys + " --sa\">Show All (slow) (--sa) "+kws.size()+"</a></li>\n";
         
      for (String key : sortedKeys.keySet()) {
         //output = output + "<li><a href=\""+oldKeys + " +^"+key+"\">"+key+" "+sortedKeys.get(key)+"</a></li>\n";
         output = output + "<li><a href=\"^"+key+"\">"+key+" "+sortedKeys.get(key)+"</a></li>\n";
      }
      output = output + commonHTML(OtiNanai.ENDBODY);
      return output;
   }
   
   private String commonHTML(short out) {
      if (out == OtiNanai.HEADER) {
         return new String("<html><head>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"otinanai.css\" />\n"
//            + "<link rel=\"stylesheet\" type=\"text/css\" href=\"jquery.gridster.css\" />\n"
               + "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'/>\n");
      } else if (out == OtiNanai.GOOGLE) {
         return new String("<script type=\"text/javascript\" src=\"https://www.google.com/jsapi\"></script>\n");
      } else if (out == OtiNanai.ENDHEAD) {
         return new String("</head>\n<body>\n");
      } else if (out == OtiNanai.GPSCRIPT) {
         return new String("<script>\n\tdocument.body.addEventListener('click', function (event) {\n"
               + "\t\tif (event.target.nodeName !== 'A') {\n"
               + "\t\t\treturn false;\n\t\t}\n"
               + "\t\t(window.parent || window.opener).onReceive(event.target);\n\t}, false);\n"
               + "</script>\n");
      } else if (out == OtiNanai.ENDBODY) {
         return new String("</body></html>\n");
      } else if (out == OtiNanai.REFRESH) {
         return new String("<meta http-equiv=\"refresh\" content="+OtiNanai.TICKER_INTERVAL/1000+">\n");
      } else if (out == OtiNanai.GAGE) {
         return new String("<script src=\"raphael.min.js\"></script>\n"
               + "<script src=\"justgage.min.js\"></script>\n");
      } else if (out == OtiNanai.FLOT) {
         return new String("<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.js\"></script>\n"
               + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.js\"></script>\n"
               + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.time.js\"></script>\n"
               + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.crosshair.js\"></script>\n"
               + "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.selection.js\"></script>\n"
               //+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.resize.js\"></script>\n"
               //+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.gridster.min.js\"></script>\n"
               + "<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.flot.common.js\"></script>\n");
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


   private String searchBar(String input) {
      if (input.contains("--no-search") || input.contains("--no-bar") || input.contains("--ns") || input.contains("--nb")) {
         return new String();
      }
      String searchBar=new String("\n<!-- The search bar -->\n");
      /*
      String decInput = new String();
      try {
         decInput = URLDecoder.decode(input, "UTF-8");
      } catch (UnsupportedEncodingException uee) {
         logger.info("[Web]: Unsupported Input Encoding: ("+input+")");
         decInput = "";
      }
      */
      searchBar = searchBar 
         + "<form action=\"/"
         //+ input
         + "\" method=\"get\" >\n"
         + "<input type=\"text\" name=\"q\" id=\"q\" placeholder=\"search\" autofocus value=\""
         + input
         + "\" />\n"
         + "</form>\n"
         + "<!-- END search bar -->\n\n"
         + "<script>onload = function () { document.getElementById('q').selectionStart = document.getElementById('q').value.length;}</script>";
      return searchBar;
   }

   private String webTitle(String search) {
      return new String("<title>OtiNanai Graphs|" + search+"</title>\n");
   }

	private String showKeyWords(String input, boolean cache) {
      String op = onc.getCached(input);
      if (!cache) {
         logger.info("[Web]: non-cached result requested");
      } else if (op != null) {
         logger.info("[Web]: cached: \"" + input + "\"");
         return op;
      } else
         logger.info("[Web]: Not cached: \"" + input + "\"");

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
            case "--dial":
            case "--gauge":
               graphType = OtiNanai.GRAPH_GAUGE;
               break;
            case "--merge":
            case "--m":
            case "--combine":
               graphType = OtiNanai.GRAPH_MERGED;
               matched = true;
               break;
            case "--ma":
            case "--merge-axis":
            case "--merge-axes":
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
            case "--store":
               logger.info("[Web]: Storing query");
               return storeQuery(input);
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

         logger.fine("[Web]: removeKW: "+removeKW+" exclusiveKW: "+exclusiveKW+ " startsWithKW: "+startsWithKW+" endsWithKW: "+endsWithKW);
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
                     logger.info("[Web]: Removing "+key);
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
      }
      /*
      } else if (kws.size() == 1) {
         graphType = OtiNanai.GRAPH_FULL;
      }
      */
      /*
		String output = commonHTML(OtiNanai.HEADER) 
         + timeGraphHeadString(kws, graphType, time)
         + commonHTML(OtiNanai.ENDHEAD)
         + commonHTML(OtiNanai.GPSCRIPT)
         + timeGraphBody(kws, graphType)
         + commonHTML(OtiNanai.ENDBODY);
      return output;
      */
      op  = timeGraph(kws, graphType, time);
      onc.cache(input, op);
      return op;
	}

   private String storeQuery(String input) {
      String output = new String("Storing...: <br/>\n");
      output = output 
         + input.replaceAll(" --store","")
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
