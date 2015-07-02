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
							query=requestMessageLine.replaceAll("[;\\/]", "").replaceAll("GET|HTTP1.1|HTTP1.0|\\?q=", "");
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
					case " red-pointer.png ":
					case " otinanai.css ":
					case " otinanai.flot.common.js ":
					case " otinanai.flot.merged.js ":
					case " otinanai.flot.preview.js ":
					case " jquery.flot.events.js ":
					case " jquery.js ":
					case " jquery.min.js ":
					case " jquery.flot.min.js ":
					case " jquery.flot.js ":
					case " jquery.flot.time.min.js ":
					case " jquery.flot.time.js ":
					case " jquery.flot.crosshair.min.js ":
					case " jquery.flot.crosshair.js ":
					case " jquery.gridster.min.js ":
					case " jquery.gridster.js ":
					case " jquery.gridster.css ":
					case " jquery.flot.resize.min.js ":
					case " jquery.flot.resize.js ":
					case " jquery.flot.selection.min.js ":
					case " jquery.flot.selection.js ":
					case " jquery.flot.stack.min.js ":
					case " jquery.flot.stack.js ":
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
						} else if (noSpaces.endsWith(".png")) {
							sendToClient(data, "image/png", true, connectionSocket, gzip);
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
						if (query.contains("--nc") || query.contains("--no-cache") || query.contains("--gauge") || query.contains("--dash"))
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

	private boolean sendToClient(byte[] dato, String contType, boolean cache, Socket connectionSocket, boolean gzip) {
		try {
			DataOutputStream dos = new DataOutputStream(connectionSocket.getOutputStream());
			dos.writeBytes("HTTP/1.1 200 OK\r\n");
			dos.writeBytes("Content-Type: "+contType+"\r\n");
			if (cache) {
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

	private String[] toGraph(KeyWordTracker kwt, short type, long startTime, long endTime, int maxMergeCount) {
		logger.finest("[Web]: Generating graph from KeyWordTracker: "+kwt.getKeyWord() +" type: "+type);
		String output = new String("");
		SomeRecord sr;
		ArrayList<String> data = new ArrayList<String>();

		long timePrev = System.currentTimeMillis();
		data = kwt.getMemory(startTime);
		long now=System.currentTimeMillis();
		logger.finest("[Web]: Timing - Total getMemory time: " + (now - timePrev));
		double total=0;
		int samples=0;
		boolean minMaxSet=false;
		float val=0.0f;
		float min=0;
		float max=0;
		boolean lastSet = false;
		long timeStamp = 0l;
		float last = 0f;
		ArrayList<Float> allData = new ArrayList<Float>();
                float multip = onl.getMultiplier(kwt.getKeyWord());
		for (String dato : data) {
			logger.finest("[Web]: Dato is : "+dato);
			String[] twowords = dato.split("\\s");
			val=Float.parseFloat(twowords[1]);
                        if (multip != 1f)
                                val = val * multip;

			timeStamp = Long.parseLong(twowords[0]);

			if (timeStamp < startTime) 
				break;
			if (timeStamp > endTime)
				continue;
			if (type != OtiNanai.GAGE)
				output = "\n[" +timeStamp + "," + val + "]," + output;
			samples++;
			if (!lastSet) {
				last = val;
				lastSet = true;
			}
			total += val;
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
			allData.add(val);
		}

		long post=System.currentTimeMillis();
		logger.finest("[Web]: Timing - Total processing time: " + (post - now));

		int nfth = 0;
		int fifth = 0;
		int tfifth = 0;
		int fiftieth = 0;
		int sfifth = 0;
		int nninth = 0;
		double mean = 0d;

		if ( samples != 0 ) {
			mean = total / samples;
			nfth = (int)(0.95*samples)-1;
			fifth = (int)(0.05*samples)-1;
			tfifth = (int)(0.25*samples)-1;
			fiftieth = (int)(0.50*samples)-1;
			sfifth = (int)(0.75*samples)-1;
			nninth = (int)(0.99*samples)-1;
			if (nfth < 0) nfth = 0;
			if (fifth < 0) fifth = 0;
			if (tfifth < 0) tfifth = 0;
			if (fiftieth < 0) fiftieth = 0;
			if (sfifth < 0) sfifth = 0;
			if (nninth < 0) nninth = 0;
			Collections.sort(allData);
			//logger.fine("[Web]: 1st:"+allData.get(0)+" last:"+allData.get(samples-1)+" total:"+samples+" 95th:("+nfth+") "+allData.get(nfth));
		} else {
			allData.add(0f);
		}
		String[] toReturn = new String[12];
		toReturn[0]=String.format("%.2f", min);
		toReturn[1]=String.format("%.2f", max);
		toReturn[2]=String.format("%.2f", mean);
		toReturn[3]=output;
		toReturn[4]=allData.get(nfth).toString();
		toReturn[5]=Float.toString(last);
		toReturn[6]=Integer.toString(samples);
		toReturn[7]=allData.get(fifth).toString();
		toReturn[8]=allData.get(tfifth).toString();
		toReturn[9]=allData.get(fiftieth).toString();
		toReturn[10]=allData.get(sfifth).toString();
		toReturn[11]=allData.get(nninth).toString();
		return toReturn;
	}

	private short getSuffix(String sample) {
		int len = sample.length();
		if (len > 16) 
			return OtiNanai.PETA;
		else if (len > 13) 
			return OtiNanai.GIGA;
		else if (len > 10)
			return OtiNanai.MEGA;
		else if (len > 7)
			return OtiNanai.KILO;
		else
			return OtiNanai.NADA;
	}

	private String trimKW(String kw) {
		String[] broken = kw.split("\\.");
		int wc = broken.length;
		logger.info("[Web]: Detected "+wc+" words while trimming "+kw);
		String skw = new String(kw);
		if (wc > 3)
			skw  = broken[0]+"."+broken[1]+"..."+broken[wc-2]+"."+broken[wc-1];

		if (skw.length() > OtiNanai.MAX_KW_LENGTH) {
			if (wc > 3)
				skw  = broken[0]+"."+broken[1]+"..."+broken[wc-1];

			if (skw.length() > OtiNanai.MAX_KW_LENGTH)
				skw = skw.substring(0,12) + "..." +skw.substring(skw.length()-12, skw.length()-1);
		}
		return skw;
	}

	private String getMarkings(boolean showEvents, long startTime, long endTime, ArrayList<KeyWordTracker> kws) {
		String marktext = new String("var marktext = [\n");
		if (showEvents) {
			long now = System.currentTimeMillis();
                        long kwa = 0l; 
                        String kw;
			TreeMap<Long, String> allEventMap = onl.getEvents();
                        for (KeyWordTracker kwt : kws) {
                                kw = kwt.getKeyWord();
                                kwa = onl.getAlarm(kw);
                                if (kwa != 0l) {
                                        allEventMap.put(kwa, kw+" Alarm");
                                }
                        }
			NavigableMap<Long, String> eventMap = allEventMap.subMap(startTime, true, endTime, true);
			Long key = 0l;
			String text = new String();
                        int sz=eventMap.size();
			for (int c = 0; c < sz ; c++) {
				Map.Entry<Long, String> event = eventMap.pollFirstEntry();
				key = event.getKey();
				text = event.getValue();
				marktext = marktext + "\t{ min:"+key+", max:"+key+", title: \""+text+"\"},\n";
			}
		}
		marktext = marktext + "];\n";
		return marktext;
	}

	private String timeGraph(ArrayList<String> keyList, short type, long startTime, long endTime, int maxMergeCount, boolean showEvents, int graphLimit, boolean autoRefresh, boolean showSpikes) {
		ArrayList<KeyWordTracker> kws = new ArrayList<KeyWordTracker> ();
		LLString kwtList = onl.getKWTList();

		TreeSet<String> sortedKeys = new TreeSet<String>();
		sortedKeys.addAll(keyList);

		for (String key : sortedKeys) {
			key=key.toLowerCase();
			if (kwtList.contains(key)) {
				logger.fine("[Web]: Matched "+key);
				kws.add(onl.getKWT(key));
			}
		}

		String output;
		String nodata = new String();
		String body = new String("");
		String[] graphData;
                int drawnGraphs = 0;

                if (graphLimit == 0) 
                        graphLimit = kws.size();


		if (type == OtiNanai.GRAPH_GAUGE) {
			output = commonHTML(OtiNanai.GAGE) + commonHTML(OtiNanai.REFRESH);
			for (KeyWordTracker kwt : kws) {
				//String kw = kwt.getKeyWord().replaceAll("\\.","_");
				String kw = kwt.getKeyWord();
				String skw = kw;
				if (kw.length() > OtiNanai.MAX_KW_LENGTH) 
					skw = trimKW(kw);
				kw = kw.replaceAll("\\.","_");
				graphData = toGraph(kwt, type, startTime, endTime, maxMergeCount);
				if (graphData[6].equals("0")) {
					logger.fine("[Web]: Skipping "+kw+ " due to insufficient data points. - 0");
					nodata = nodata + "No data in timerange for "+kw+"<br>";
					continue;
				}
				output = output
					+ "<div id=\"" + kw + "\" class=\"gage\"></div>\n"
					+ "<script>\n"
					+ "\tvar "+kw+" = new JustGage({\n"
					+ "\t\tid: \""+kw+"\",\n"
					+ "\t\tvalue: "+graphData[5]+",\n"
					+ "\t\tmin: "+graphData[0]+",\n"
					+ "\t\tmax: "+graphData[1]+",\n"
					+ "\t\ttitle: \""+skw+"\",\n"
					+ "\t\tlabel: \"\",\n"
					//+ "\t\tdonut: true,\n"
					//+ "\t\tsymbol: \"c\",\n"
					//+ "\t\tlabelFontColor: \"#ABC\",\n"
					//+ "\t\ttitleFontColor: \"#ABC\",\n"
					+ "\t\tlevelColorsGradient: true\n"
					+ "\t});\n"
					+ "</script>\n";
                                drawnGraphs++;
                                if (drawnGraphs >= graphLimit)
                                        break;
			}
			output = output 
				+ commonHTML(OtiNanai.ENDHEAD)
				+ commonHTML(OtiNanai.ENDBODY);

		} else if (type == OtiNanai.GRAPH_PREVIEW) {
			output = commonHTML(OtiNanai.FLOT) 
                                + (autoRefresh ? commonHTML(OtiNanai.REFRESH) : "")
                                + commonHTML(OtiNanai.FLOT_PREVIEW);

			output = output+ commonHTML(OtiNanai.JS)
				+ getMarkings(showEvents, startTime, endTime, kws)
                                + "var showSpikes = "+showSpikes+";\n"
				+ "var datasets = {\n";

			for (KeyWordTracker kwt : kws) {
				graphData = toGraph(kwt, type, startTime, endTime, maxMergeCount);
				String kw = kwt.getKeyWord();
				if (graphData[6].equals("0") || graphData[6].equals("1")) {
					logger.fine("[Web]: Skipping "+kw+ " due to insufficient data points - "+ graphData[6]);
					nodata = nodata + "No data in timerange for "+kw+"<br>";
					continue;
				}


				output = output + "\"" + kw.replaceAll("\\.","_") + "\": {\n"
					+ "label: \""+kw+"\",\n";

				//output = output + "nn:  "+ (showSpikes ? "null" : graphData[11]) +",\n";
				output = output + "nn:  "+ graphData[11] +",\n";

				output = output + "data: ["
					+ graphData[3]
					+ "]},\n\n";

				body = body 
					+ "<div class=\"wrapper clearfix\">\n"
					+ "\t<li><a href = \""+kw+"\">"+kw+"</a> " 
					+ onl.getUnits(kw)
					+ " ("+parseType(kwt.getType())+") "
					+ "<script>"
					+ "document.write("
					+ "\"<span id=output_values>min:\" + addSuffix("+graphData[0]+")"
					+ "+\"</span><span id=output_values> max:\" + addSuffix("+graphData[1]+")"
					+ "+\"</span><span id=output_values> mean:\" + addSuffix("+graphData[2]+")"
					+ "+\"</span><span id=output_values> 5%:\"+ addSuffix("+graphData[7]+")"
					+ "+\"</span><span id=output_values> 25%:\"+ addSuffix("+graphData[8]+")"
					+ "+\"</span><span id=output_values> 50%:\"+ addSuffix("+graphData[9]+")"
					+ "+\"</span><span id=output_values> 75%:\"+ addSuffix("+graphData[10]+")"
					+ "+\"</span><span id=output_values> 95%:\"+ addSuffix("+graphData[4]+")"
					+ "+\"</span><span id=output_values> 99%:\"+ addSuffix("+graphData[11]+")"
					+ "+\"</span><span id=output_values> samples:\" + " + graphData[6]
					+ "+\"</span><span> alarm:\" + " + onl.alarmEnabled(kw)
					+ "+\"</span>\""
					+ ");"
					+ "</script>"
					+ "</li>\n"
					+ "\t<div id=\"" + kw.replaceAll("\\.","_") + "\" class=\"previewGraph\"></div>\n"
					+ "</div>\n";

                                drawnGraphs++;
                                if (drawnGraphs >= graphLimit)
                                        break;
			}
		} else {
			output = commonHTML(OtiNanai.FLOT) 
                                + (autoRefresh ? commonHTML(OtiNanai.REFRESH) : "")
                                + commonHTML(OtiNanai.FLOT_MERGED);

			int idx = (new Random()).nextInt(200);
			output = output+ commonHTML(OtiNanai.JS)
				+ getMarkings(showEvents, startTime, endTime, kws)
				+ "var maxMergeCount = "+maxMergeCount+";\n"
				+ "var stackedGraph = "+(type == OtiNanai.GRAPH_STACKED)+";\n"
                                + "var idx = "+idx+";\n"
                                + "var showSpikes = "+showSpikes+";\n"
				+ "var datasets = {\n";

			HashMap <String, String[]> dataMap = new HashMap<String, String[]>();
			for (KeyWordTracker kwt : kws) {
				graphData = toGraph(kwt, type, startTime, endTime, maxMergeCount);
				String kw = kwt.getKeyWord();
				if (graphData[6].equals("0") || graphData[6].equals("1")) {
					logger.fine("[Web]: Skipping "+kw+ " due to insufficient data points - "+ graphData[6]);
					nodata = nodata + "No data in timerange for "+kw+"<br>";
					continue;
				}
				dataMap.put(kw, graphData);
			}

                        NNComparator nnc = new NNComparator(dataMap);
                        TreeMap <String, String[]> sortedMap = new TreeMap<String, String[]>(nnc);
                        sortedMap.putAll(dataMap);

			if (sortedMap.size() < maxMergeCount)
                                maxMergeCount=sortedMap.size();


                        if (graphLimit > sortedMap.size())
                                graphLimit = sortedMap.size();
                        int graphCount = (int)Math.ceil(graphLimit / (float)maxMergeCount);
                        int totalKeys = sortedMap.size();
                        for (int j=0 ; j < totalKeys ; j++) {
                                Map.Entry<String, String[]> foo = sortedMap.pollLastEntry();
				String kw = foo.getKey();
                                graphData = foo.getValue();

				output = output + "\"" + kw.replaceAll("\\.","_") + "\": {\n"
					+ "label: \""+kw+" "+onl.getUnits(kw)+"\",\n";
					//+ "label: \""+kw+" = 000.000 k \",\n";

				
				//output = output + "nn:  "+ (showSpikes ? "null" : graphData[11]) + ",\n";
				output = output + "nn:  "+ graphData[11] + ",\n";

				output = output + "data: ["
					+ graphData[3]
					+ "]},\n\n";

                                drawnGraphs++;
                                if (drawnGraphs >= graphLimit)
                                        break;
			}

			for (int j = 0 ; j < graphCount ; j++) {
				body = body
					+ "<div>\n"
					+ "\t<div id=\"placeholder_"+(idx+j)+"\" class=\"mergedGraph\"></div>\n"
					+ "</div>\n";
			}
		}

		if (type != OtiNanai.GRAPH_GAUGE) {
			output = output + "};\n"
				+ commonHTML(OtiNanai.ENDJS)
				+ commonHTML(OtiNanai.ENDHEAD)
				+ body
				+ (nodata.length() > 1 ? nodata : "")
				+ commonHTML(OtiNanai.ENDBODY);
		}

		return output;
	}

	private String parseType(short t) {
		switch (t) {
			case OtiNanai.GAUGE: return new String("gauge");
			case OtiNanai.COUNTER: return new String("count");
			case OtiNanai.FREQ: return new String("freq");
			case OtiNanai.SUM: return new String("sum"); 
		}
		return new String("unset");
	}

	private String kwTree(ArrayList<String> kws, String[] existingKeyWords, ArrayList<String> words) {
		TreeMap<String, Integer> keyMap = new TreeMap<String, Integer>();
		String tmp;
		int sofar;
		int totalCount = kws.size();
		int length = 0;
		int nextDot = 0;
		for (String word : words) {
			//System.out.println(word);
			ArrayList<String> tmpAS = new ArrayList<String>();
			tmpAS.addAll(kws);
			for (String kw : tmpAS) {
				if (kw.contains(word)) {
					length=kw.indexOf(word)+word.length();
					nextDot=kw.indexOf(".", length+1);
					if (nextDot > 0)
						tmp = kw.substring(0,nextDot);
					else
						tmp = kw.substring(0,length);
					sofar = 0;
					if (keyMap.containsKey(tmp)) {
						sofar = keyMap.get(tmp);
					}
					keyMap.put(tmp, ++sofar);
					kws.remove(kw);
				}
			}
		}
		for (String kw : kws) {
			if (kw.contains("."))
				tmp = kw.substring(0, kw.indexOf("."));
			else
				tmp = kw;
			sofar = 0;
			if (keyMap.containsKey(tmp)) {
				sofar = keyMap.get(tmp);
			}
			keyMap.put(tmp, ++sofar);
		}

		//ValueComparator vc = new ValueComparator(keyMap);
		//TreeMap <String, Integer> sortedKeys = new TreeMap<String, Integer>(vc);
		//sortedKeys.putAll(keyMap);

		String oldKeys = new String();
		for (String foo : existingKeyWords) {
			oldKeys = oldKeys + foo + " ";
		}
		oldKeys = oldKeys.substring(0,oldKeys.length()-1);

		String output = commonHTML(OtiNanai.ENDHEAD)
			+ "<ul><li><a href=\""+oldKeys + " --sa --merge\">Show All (slow) (--sa) "+totalCount+"</a></li>\n";

		for (String key : keyMap.keySet()) {
			output = output + "<li><a href=\"^"+key+" --merge\">^"+key+" "+keyMap.get(key)+"</a></li>\n";
		}
		output = output + "</ul>\n" + commonHTML(OtiNanai.ENDBODY);
		return output;
	}

	private String commonHTML(short out) {
		if (out == OtiNanai.HEADER) {
			return new String("<html><head>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"otinanai.css\" />\n"
					+ "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'/>\n");
		} else if (out == OtiNanai.ENDHEAD) {
			return new String("</head>\n<body>\n");
		} else if (out == OtiNanai.ENDBODY) {
			return new String("</body></html>\n");
		} else if (out == OtiNanai.REFRESH) {
			return new String("<meta http-equiv=\"refresh\" content="+OtiNanai.TICKER_INTERVAL/1000+">\n");
		} else if (out == OtiNanai.GAGE) {
			return new String("<script src=\"raphael.min.js\"></script>\n"
					+ "<script src=\"justgage.min.js\"></script>\n");
		} else if (out == OtiNanai.FLOT) {
			return new String("<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.time.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.crosshair.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.selection.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.events.js\"></script>\n"
					//+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.resize.min.js\"></script>\n"
					//+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.gridster.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.flot.common.js\"></script>\n");
		} else if (out == OtiNanai.FLOT_MERGED) {
			return new String("<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.stack.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.flot.merged.js\"></script>\n");
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
		searchBar = searchBar 
			+ "<form action=\"/"
			+ "\" method=\"get\" >\n"
			+ "<input type=\"text\" name=\"q\" id=\"q\" placeholder=\"search\" autofocus value=\""
			+ input
			+ "\" />\n"
			+ "</form>\n"
			+ "<!-- END search bar -->\n\n"
			+ "<script>onload = function () { document.getElementById('q').selectionStart = document.getElementById('q').value.length;}</script>\n";
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
		ArrayList<String> words = new ArrayList<String>();

		String firstChar = new String();
		String secondChar = new String();
		String lastChar = new String();
		String rest;
		boolean wipe = false;
		boolean force = false;
		boolean alarm = false;
		boolean showAll = false;
		boolean showAlarms = false;
		boolean showEvents = true;
		int maxMergeCount = OtiNanai.MAXMERGECOUNT;
		short graphType = OtiNanai.GRAPH_PREVIEW;
		long now = System.currentTimeMillis();
		long endTime = now;
		long startTime = now - OtiNanai.PREVIEWTIME;
		boolean setUnits = false;
		boolean nextWordIsUnit = false;
                boolean nextWordIsLimit = false;
                boolean setMultip = false;
                boolean nextWordIsMultip = false;
		String units = new String();
                int graphLimit = 0;
                float multip = 1f;
                boolean disableAlarm = false;
                boolean enableAlarm = false;
                boolean autoRefresh = false;
                boolean showSpikes = false;

		for (String word : keyList) {
			if (nextWordIsUnit) {
				units=word;
				nextWordIsUnit = false;
				continue;
			}
                        if (nextWordIsLimit) {
                                try {
                                        graphLimit = Integer.parseInt(word);
                                        nextWordIsLimit = false;
                                } catch (NumberFormatException nfe) {
                                        logger.info("Invalid argument for --limit : "+word);
                                }
                                continue;
                        }
                        if (nextWordIsMultip) {
                                try {
                                        multip = Float.parseFloat(word);
                                        nextWordIsMultip = false;
                                } catch (NumberFormatException nfe) {
                                        logger.info("Invalid argument for --multip : "+word);
                                        setMultip = false;
                                }
                                continue;
                        }

			boolean removeKW = false;
			boolean exclusiveKW = false;
			boolean startsWithKW = false;
			boolean endsWithKW = false;
			boolean setTime = false;
			boolean setStartTime = false;
			boolean setMaxMergeCount = false;
			switch (word) {
				case "--showall":
				case "--sa":
				case "--show":
					showAll = true;
					continue;
				case "--delete":
					wipe = true;
					continue;
                                case "--no-alarm":
                                case "--noalarm":
                                case "--na":
                                case "--da":
                                        disableAlarm = true;
                                        continue;
                                case "--enable-alarm":
                                case "--enalarm":
                                case "--ea":
                                        enableAlarm = true;
                                        continue;
				case "--units":
				case "--setunits":
				case "--unit":
					setUnits=true;
					nextWordIsUnit=true;
					continue;
                                case "--multip":
                                case "--setmultip":
                                case "--multiplier":
                                case "--setmultiplier":
                                        setMultip=true;
                                        nextWordIsMultip=true;
                                        continue;
                                case "--limit":
                                        nextWordIsLimit=true;
                                        continue;
				case "--force":
					force = true;
					continue;
				case "--dial":
				case "--gauge":
					graphType = OtiNanai.GRAPH_GAUGE;
					continue;
				case "--stack":
					graphType = OtiNanai.GRAPH_STACKED;
					continue;
				case "--merge":
				case "--m":
				case "--combine":
					graphType = OtiNanai.GRAPH_MERGED;
					continue;
				case "--alarms":
				case "--alerts":
					showAlarms = true;
					logger.info("[Web]: Showing Alarms");
					continue;
				case "--store":
					logger.info("[Web]: Storing query");
					return storeQuery(input);
				case "--no-events":
				case "--ne":
					logger.info("[Web]: Showing Events");
					showEvents = false;
					continue;
                                case "--auto-refresh":
                                case "--refresh":
                                case "--ar":
                                        autoRefresh = true;
                                        continue;
                                case "--show-spikes":
                                case "--ss":
                                        showSpikes = true;
                                        continue;
				case "--no-search":
				case "--no-bar":
				case "--ns":
				case "--nb":
				case "--nc":
				case "--no-cache":
				case "":
					continue;
			}

			word = word.replaceAll("%5E", "^");
			word = word.replaceAll("%24", "$");
			word = word.replaceAll("%40", "@");
			logger.fine("[Web]: word is: \""+word+"\"");
			firstChar = word.substring(0,1);
			rest = word;

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
			else if (firstChar.equals("#"))
				setMaxMergeCount = true;
			if (exclusiveKW || removeKW || setTime || setMaxMergeCount)
				rest = rest.substring(1);

			if (lastChar.equals("$")) {
				endsWithKW = true;
				rest = rest.substring(0,rest.length()-1);
			}
			if (firstChar.equals("^") || secondChar.equals("^")) {
				startsWithKW = true;
				rest = rest.substring(1);
			}


			if (setTime) {
				try {
					String pt1 = new String();
					String pt2 = new String();
					long pt1l = 0l;
					long pt2l = 0l;

					if (rest.contains("-")) {
						pt1 = rest.substring(0, rest.indexOf("-"));
						pt2 = rest.substring(rest.indexOf("-")+1);
						if (pt2.length() > 0) {
							if (pt2.substring(pt2.length()-1).equals("d")) {
								pt2 = pt2.substring(0, pt2.length()-1);
								pt2l = 86400000 * Long.parseLong(pt2);
							} else {
								pt2l = 3600000 * Long.parseLong(pt2);
							}
						}
					} else {
						pt1 = rest;
					}

					if (pt1.length() > 0) {
						if (pt1.substring(pt1.length()-1).equals("d")) {
							pt1 = pt1.substring(0, pt1.length()-1);
							pt1l = 86400000 * Long.parseLong(pt1);
						} else {
							pt1l = 3600000 * Long.parseLong(pt1);
						}
					}

					if (pt1l < pt2l) {
						startTime = now-pt2l;
						endTime = now-pt1l;
					} else {
						startTime = now-pt1l;
						endTime = now-pt2l;
					}
				} catch (Exception e) {
					System.err.println("bogus entry for time range");
				}
			}
			if (setMaxMergeCount) {
				try {
					maxMergeCount = Integer.parseInt(rest);
				} catch (NumberFormatException nfe) {
					logger.severe("[Web]: Not a valid number for max count\n"+nfe);
				}
			}
			/*
			System.out.println("now  : "+now);
			System.out.println("start: "+startTime);
			System.out.println("end  : "+endTime);
			*/


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
			words.add(rest);
		}
		if (showAlarms) {
			long timeNow = System.currentTimeMillis();
			long lastAlarm;
			logger.info("[Web]: kws.size() = "+kws.size());
			for (String kw : allKWTs ) {
				lastAlarm=onl.getAlarm(kw);
				if (lastAlarm == 0L || (timeNow - lastAlarm) > OtiNanai.ALARMLIFE) {
					logger.info("[Web]: No alarm for "+kw+ " - Removing");
					kws.remove(kw);
				} else {
					logger.info("[Web]: Alarm for "+kw);
				}
			}
                } else if (enableAlarm || disableAlarm) {
                        boolean onOrOff = (enableAlarm ? true : false);
                        logger.info("[Web]: Setting alarmEnabled for matching keywords to "+onOrOff);
                        String listOP = new String("<h2>Setting alarmEnabled to "+onOrOff+" for keywords:</h2><br />\n<ul>\n");
                        for (String kw : kws) {
                                logger.info("[Web]: Setting alarmEnabled for "+kw+" to "+onOrOff);
                                listOP = listOP + "<li>"+kw+"</li>\n";
                                onl.alarmEnabled(kw, onOrOff);
                        }
                        listOP = listOP + "</ul>\n"
                                + commonHTML(OtiNanai.ENDBODY);
                        return listOP;
		} else if (setUnits) {
			logger.info("[Web]: Setting matching Keyword Units to "+units);
			String unitsOP = new String("<h2>Setting units to "+units+" for keywords:</h2><br />\n<ul>\n");
			for (String kw : kws) {
				logger.info("[Web]: Setting "+kw+" units to "+units);
				unitsOP = unitsOP + "<li>"+kw+"</li>\n";
				onl.setUnits(kw, units);
			}
                        unitsOP = unitsOP + "</ul>\n"
                                + commonHTML(OtiNanai.ENDBODY);
			return unitsOP;
		} else if (setMultip) {
			logger.info("[Web]: Setting matching Keyword Multiplier to "+multip);
			String multipOP = new String("<h2>Setting multiplier to "+multip+" for keywords:</h2><br />\n<ul>\n");
			for (String kw : kws) {
				logger.info("[Web]: Setting "+kw+" multiplier to "+multip);
				multipOP = multipOP + "<li>"+kw+"</li>\n";
				onl.setMultiplier(kw, multip);
			}
                        multipOP = multipOP + "</ul>\n"
                                + commonHTML(OtiNanai.ENDBODY);
			return multipOP;
		} else if (wipe && force) {
			logger.info("[Web]: --delete received with --force. Deleting matched keywords Permanently");
			KeyWordTracker kwt;
			String delOP = new String("<h2>RIP Data for keywords:</h2><br />\n<ul>\n");
			for (String todel : kws) {
				logger.info("[Web]: Deleting data for " + todel);
				delOP = delOP + "<li>"+todel+"</li>\n";
				onl.deleteKWT(todel);
			}
                        delOP = delOP + "</ul>\n"
                                + commonHTML(OtiNanai.ENDBODY);
			return delOP;
		} else if (wipe) {
			logger.fine("[Web]: Wipe command received. Sending Warning");
			String delOP = new String("<h2>[WARNING] : You are about to permanently delete the following keywords</h2><br/>Add --force to actually delete<br />\n<ul>\n");
			for (String todel : kws) {
				delOP = delOP + "<li><a href=\"" + todel + "\">"+todel+"</a></li>\n";
			}
                        delOP = delOP + "</ul>\n"
                                + commonHTML(OtiNanai.ENDBODY);
			return delOP;
		}
		if (!showAll && kws.size() > OtiNanai.MAXPERPAGE) {
			logger.info("[Web]: Exceeded MAXPERPAGE: "+ kws.size() + " > " +OtiNanai.MAXPERPAGE);
			return kwTree(kws, keyList, words);
		}
		op  = timeGraph(kws, graphType, startTime, endTime, maxMergeCount, showEvents, graphLimit, autoRefresh, showSpikes);
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
