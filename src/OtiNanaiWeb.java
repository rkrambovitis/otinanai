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
import java.net.URLEncoder;
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
			String query = new String("");
			boolean gzip = false;
			while (true) {
				String currentDashboard = "default";
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
						} else if (requestMessageLine.startsWith("Cookie: ")) {
							logger.info("[Web]: Processing "+requestMessageLine);
							requestMessageLine = requestMessageLine.replaceAll("Cookie: ", "");
							String[] cookies = requestMessageLine.split(";");
							for (String cookie : cookies ) {
								String[] c = cookie.split("=");
								logger.info("[Web]: cookie: "+c[0]+" = "+c[1]);
								if (c[0].equals("dashboard"))
									currentDashboard = c[1];
							}
						} else {
							logger.fine("[Web]: Ignoring: " + requestMessageLine);
						}
						requestMessageLine = inFromClient.readLine();
					}
				} catch (NullPointerException npe) {
					logger.warning("[Web]: "+npe);
					//continue;
				}
				/*logger.fine("[Web]: about to switch");*/
				boolean alarms=false;
				boolean graph=false;
				boolean mergeKeyWords=false;
				Path path;
				byte[] data;
				switch (query) {
					case " test.html ":
					case " favicon.ico ":
					case " red-pointer.png ":
					case " otinanai.css ":
					case " otinanai.js ":
					case " otinanai.flot.js ":
					case " otinanai.flot.common.js ":
					case " otinanai.sortable.js ":
					case " otinanai.stupidtable.js ":
					case " jquery.flot.events.js ":
					case " jquery.js ":
					case " jquery.min.js ":
					case " jquery.flot.min.js ":
					case " jquery.flot.js ":
					case " jquery.flot.time.min.js ":
					case " jquery.flot.time.js ":
					case " jquery.flot.crosshair.min.js ":
					case " jquery.flot.crosshair.js ":
					case " jquery.flot.resize.min.js ":
					case " jquery.flot.resize.js ":
					case " jquery.flot.selection.min.js ":
					case " jquery.flot.selection.js ":
					case " jquery.flot.stack.min.js ":
					case " jquery.flot.stack.js ":
					case " stupidtable.min.js ":
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
						} else if (noSpaces.endsWith(".html")) {
							sendToClient(data, "text/html", true, connectionSocket, gzip);
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
							query = "";

						boolean cache = true;
						if (query.contains("--nc") || query.contains("--no-cache") || query.contains("--gauge") || query.contains("--dashboard"))
							cache = false;

						String text;
						if (query.contains("--toggleStar"))
							text = String.valueOf(onl.toggleStar(query.replaceAll(" --toggleStar","")));
						else if (query.contains("--toggleDashboard"))
							text = showKeyWords(query.toLowerCase(), currentDashboard, false);
						else if (query.contains("--updateDashboard"))
							text = showKeyWords(query.toLowerCase(), currentDashboard, false);
						else if (query.contains("--starred"))
							text = commonHTML(OtiNanai.HEADER) + webTitle(query) + searchBar(query, currentDashboard) + starList();
						else if (query.contains("--stored"))
							text = commonHTML(OtiNanai.HEADER) + webTitle(query) + searchBar(query, currentDashboard) + storeList();
						else
							text = commonHTML(OtiNanai.HEADER) + webTitle(query) + searchBar(query, currentDashboard) + showKeyWords(query.toLowerCase(), currentDashboard, cache);

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
				byte[] littleDato = new byte[dato.length + 30];
				int contentLength = compressor.deflate(littleDato);
				compressor.end();
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

	private String[] toGraph(KeyWordTracker kwt, short type, long startTime, long endTime, long offset, boolean isOffset) {
		String[] toReturn = new String[14];
		if (kwt == null) {
			logger.fine("[Web]: null kwt");
			toReturn[6] = new String("0");
			return toReturn;
		}
		logger.fine("[Web]: Generating graph from KeyWordTracker: "+kwt.getKeyWord() +" type: "+type);
		String output = new String("");
		SomeRecord sr;
		ArrayList<String> data = new ArrayList<String>();

		long timePrev = System.currentTimeMillis();
                if (isOffset) {
                        startTime -= offset;
                        endTime -= offset;
                }
                // isOffset is false for main graph, so we use offset to get lower resolution data
                if (!isOffset)
                        data = kwt.getMemory(startTime, offset);
                else
                        data = kwt.getMemory(startTime, 0l);
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
                        dato = dato.replaceAll(",", ".");
			String[] twowords = dato.split("\\s");
			val=Float.parseFloat(twowords[1]);
                        if (multip != 1f)
                                val = val * multip;

			timeStamp = Long.parseLong(twowords[0]);

			if (timeStamp < startTime) 
				break;
			if (timeStamp > endTime)
				continue;
                        if (isOffset)
                                timeStamp += offset;
			if (type != OtiNanai.GRAPH_GAUGE && type != OtiNanai.GRAPH_PERCENTILES && type != OtiNanai.GRAPH_NONE)
				output = "\t\t\t[" +timeStamp + "," + val + "],\n" + output;
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
		float mean = 0f;

		if ( samples != 0 ) {
			mean = (float)(total / samples);
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

		long divisor = 1l;
		float nn = allData.get(nninth);
		if (nn > 500000000000l) {
			divisor = 1000000000000l;
			toReturn[12] = "P";
		} else if (nn > 500000000l) {
			divisor = 1000000000l;
			toReturn[12] = "G";
		} else if (nn > 500000) {
			divisor = 1000000l;
			toReturn[12] = "M";
		} else if (nn > 500) {
			divisor = 1000l;
			toReturn[12] = "k";
		} else
			toReturn[12] = "";

		if (type == OtiNanai.GRAPH_PERCENTILES) {
                        for (float k = 0.8f ; k < 1 ; ) {
                                try {
                                        output = output + "\t\t\t["+String.format("%.2f", (k*100)).replaceAll(",", ".")+","+allData.get((int)(k*samples)-1)+"],\n";
                                } catch (ArrayIndexOutOfBoundsException aioobe) {}
                                if (k > 0.9)
                                        k+=0.001;
                                else
                                        k+=0.01;
                        }
			//output = output + "\t\t\t[100, "+allData.get(samples-1)+"],\n";
                }

		toReturn[0]=formatNumber(min, divisor);
		toReturn[1]=formatNumber(max, divisor);
		toReturn[2]=formatNumber(mean, divisor);
		toReturn[3]=output;
		toReturn[4]=formatNumber(allData.get(nfth), divisor);
		toReturn[5]=formatNumber(last, divisor);
		toReturn[6]=Integer.toString(samples);
		toReturn[7]=formatNumber(allData.get(fifth), divisor);
		toReturn[8]=formatNumber(allData.get(tfifth), divisor);
		toReturn[9]=formatNumber(allData.get(fiftieth), divisor);
		toReturn[10]=formatNumber(allData.get(sfifth), divisor);
		toReturn[11]=formatNumber(allData.get(nninth), divisor);
		toReturn[13]=formatNumber(allData.get(nninth), 1f);

		return toReturn;
	}

	private String formatNumber(float value, float divisor) {
		Float rv = value / divisor;
		String op = new String();
		if (Math.abs(rv) < 1)
			op = String.format("%.3f", rv);
		else
			op = String.format("%.2f", rv);
		return op.replaceAll(",", ".");
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

	private String timeGraph(ArrayList<String> keyList, short type, long startTime, long endTime, int maxMergeCount, boolean showEvents, int graphLimit, boolean autoRefresh, boolean showSpikes, boolean showDetails, String currentDashboard, long vsTime) {
		ArrayList<KeyWordTracker> kws = new ArrayList<KeyWordTracker> ();
		LLString kwtList = onl.getKWTList();

		TreeSet<String> sortedKeys = new TreeSet<String>();
		sortedKeys.addAll(keyList);

                if (type != OtiNanai.GRAPH_DASHBOARD) {
                        for (String key : sortedKeys) {
                                key=key.toLowerCase();
                                if (kwtList.contains(key)) {
                                        logger.fine("[Web]: Matched "+key);
                                        kws.add(onl.getKWT(key));
                                }
                        }
                }

		String output = new String();
		String nodata = new String();
		String body = new String("");
		String[] graphData;
                int drawnGraphs = 0;

                if (graphLimit == 0) 
                        graphLimit = kws.size();

                String deleteAll = new String();
		if (type == OtiNanai.GRAPH_GAUGE) {
			output = commonHTML(OtiNanai.GAGE) + commonHTML(OtiNanai.REFRESH);
			for (KeyWordTracker kwt : kws) {
				//String kw = kwt.getKeyWord().replaceAll("\\.","_");
				String kw = kwt.getKeyWord();
				String skw = kw;
				if (kw.length() > OtiNanai.MAX_KW_LENGTH) 
					skw = trimKW(kw);
				kw = kw.replaceAll("\\.","_");
				graphData = toGraph(kwt, type, startTime, endTime, 0l, false);
				if (graphData[6].equals("0")) {
					logger.fine("[Web]: Skipping "+kw+ " due to insufficient data points. - 0");
                                        nodata = nodata + "\t<li>No data in timerange for "+kw+"</li>\n";
                                        deleteAll = deleteAll + "^"+kw+"$ ";
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
		} else if (type == OtiNanai.GRAPH_DASHBOARD) {
			output = commonHTML(OtiNanai.FLOT)
                                + (autoRefresh ? commonHTML(OtiNanai.REFRESH) : "");

			LLString dashKWs = onl.getDashboard(currentDashboard);
                        ArrayList<KeyWordTracker> dashMarks = new ArrayList<KeyWordTracker> ();
                        KeyWordTracker indTracker;
                        for (String kwlist : dashKWs) {
				String [] individualKWs = kwlist.split(" ");
                                for (String ind : individualKWs) {
                                        indTracker = onl.getKWT(ind);
                                        if (indTracker != null) {
                                                dashMarks.add(onl.getKWT(ind));
                                        }
                                }
                        }

			int idx = (new Random()).nextInt(200);
			output = output+ commonHTML(OtiNanai.JS)
				+ getMarkings(showEvents, startTime, endTime, dashMarks)
                                + "var idx = "+idx+";\n"
                                + "var showSpikes = "+showSpikes+";\n"
				+ "var stackedGraph = false;\n"
				+ "var percentilesGraph = false;\n"
				+ "var preStarred = true;\n"
				+ "var datasets = {\n";

			body = body
				+ "<div id=\"sortable\">\n";

			int j=0;
			for (String kwlist : dashKWs) {
				logger.info("[Web]: Processing dashboard list : \""+kwlist+"\"");
				String [] dashkws = kwlist.split("[ ,]|%20");
				output = output + "graph"+ (idx+j) +": {\n";
				body = body + "\t<ul data-id=\""+kwlist+"\" class=\"graphListing\">\n";
				for (String kw : dashkws) {
					logger.info("[Web]: Processing dashboard keyword : "+kw);
					if (kw.equals("--stack")) {
						output = output + "\tstackedGraph: true,\n";
						continue;
					}
					graphData = toGraph(onl.getKWT(kw), type, startTime, endTime, vsTime, false);
					logger.info("[Web]: Got Data for "+kw+", processing");
					if (graphData[6].equals("0") || graphData[6].equals("1")) {
						logger.info("[Web]: Skipping "+kw+ " due to insufficient data points - "+ graphData[6]);
						nodata = nodata + "\t<li>No data in timerange for "+kw+"</li>\n";
                                                deleteAll = deleteAll + "^"+kw+"$ ";
						continue;
					} else {
						output = output + "\t\"" + kw.replaceAll("\\.","_") + "\": {\n"
                                                        + "\t\tkeyword: \""+kw+"\",\n"
							+ "\t\tlabel: \""+kw+" ("+graphData[12]+onl.getUnits(kw)+")\",\n"
							+ "\t\tnn: "+ graphData[13] + ",\n"
							+ "\t\tdata: [\n"
							+ graphData[3]
							+ "\t\t]\n\t},\n\n";

						if (showDetails) {
							body = body
								+ "\t\t<li class=\"draggable\">\n"
								+ "\t\t\t<a href = \""+kw+"\">"+kw+"</a>\n"
                                                                + "\t\t\t<div style=\"text-align: right\">\n"
                                                                + "<span id=output_values>type: "+ onl.getType(kw) +"</span>"
                                                                + "<span id=output_values>min: "+graphData[0]+"</span>"
                                                                + "<span id=output_values>max: "+graphData[1]+"</span>"
                                                                + "<span id=output_values>95%: "+graphData[4]+"</span>"
                                                                + "<span id=output_values>99%: "+graphData[11]+"</span>"
                                                                + "<span id=output_values>now: "+graphData[5]+"</span>"
                                                                + "\t\t\t</div>\n"
								+ "\t\t</li>\n";
						}
                                                if (vsTime != 0) {
                                                        graphData = toGraph(onl.getKWT(kw), type, startTime, endTime, vsTime, true);
                                                        output = output + "\t\"" + kw.replaceAll("\\.","_") + "@vs\": {\n"
                                                                + "\t\tlabel: \""+kw+"@vs ("+graphData[12]+onl.getUnits(kw)+")\",\n"
                                                                + "\t\tnn: "+ graphData[13] + ",\n"
                                                                + "\t\tdata: [\n"
                                                                + graphData[3]
                                                                + "\t\t]\n\t},\n\n";
                                                }
					}
				}
				output = output + "},\n";


				body = body
					+ "\t\t<div id=\"placeholder_"+(idx+j) +"\" class=\"dashGraph\"></div>\n"
					+ "\t</ul>\n";
				j++;
			}
			body = body
				+ "</div>\n"
				+ "<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.sortable.js\"></script>\n";
                } else if (type == OtiNanai.GRAPH_NONE) {
			body = body
				+ "<table id=\"sortMe\">\n"
				+ "\t<thead><tr>\n"
				+ "\t\t<th data-sort=\"string\">keyword</th>\n"
				+ "\t\t<th data-sort=\"string\">type</th>\n"
				+ "\t\t<th data-sort=\"float\">min</th>\n"
				+ "\t\t<th data-sort=\"float\">max</th>\n"
				+ "\t\t<th data-sort=\"float\">95%</th>\n"
				+ "\t\t<th data-sort=\"float\">99%</th>\n"
				+ "\t\t<th data-sort=\"float\">now</th>\n"
				+ "\t\t<th data-sort=\"string\">units</th>\n"
				+ "\t</tr></thead>\n"
				+ "\t<tbody>\n";

			for (KeyWordTracker kwt : kws) {
				graphData = toGraph(kwt, type, startTime, endTime, vsTime, false);
				String kw = kwt.getKeyWord();
				if (graphData[6].equals("0")) {
					nodata = nodata + "\t<li>No data in timerange for "+kw+"</li>\n";
                                        deleteAll = deleteAll + "^"+kw+"$ ";
					continue;
				}

                                body = body
                                        + "\t\t<tr>\n"
                                        + "\t\t\t<td><a href = \""+kw+"\">"+kw+"</a></td>\n"
                                        + "\t\t\t<td>"+onl.getType(kw)+"</td>\n"
                                        + "\t\t\t<td>"+graphData[0]+"</td>\n"
                                        + "\t\t\t<td>"+graphData[1]+"</td>\n"
                                        + "\t\t\t<td>"+graphData[4]+"</td>\n"
                                        + "\t\t\t<td>"+graphData[11]+"</td>\n"
                                        + "\t\t\t<td>"+graphData[5]+"</td>\n"
					+ "\t\t\t<td>("+graphData[12]+onl.getUnits(kw)+")</td>\n"
                                        + "\t\t</tr>\n";
                        }

			body = body
				+ "\t</tbody>\n"
				+ "</table>\n";

                        if (nodata.length() > 0 )
                                nodata = "<ul class=\"nodata\">\n"+nodata + "<li><a href=\""+deleteAll+" --delete\">Delete Empty</a>&nbsp;</li>\n</ul>\n";

			output = output
				+ commonHTML(OtiNanai.STUPIDTABLE)
				+ commonHTML(OtiNanai.ENDHEAD)
				+ body
				+ nodata
				+ commonHTML(OtiNanai.ENDBODY);
		} else {
			output = commonHTML(OtiNanai.FLOT) 
                                + (autoRefresh ? commonHTML(OtiNanai.REFRESH) : "");

			if (type == OtiNanai.GRAPH_PREVIEW)
				maxMergeCount = 1;

			int idx = (new Random()).nextInt(200);
			output = output+ commonHTML(OtiNanai.JS)
				+ getMarkings(showEvents, startTime, endTime, kws)
				+ "var stackedGraph = "+(type == OtiNanai.GRAPH_STACKED)+";\n"
				+ "var percentilesGraph = "+(type == OtiNanai.GRAPH_PERCENTILES)+";\n"
                                + "var idx = "+idx+";\n"
                                + "var showSpikes = "+showSpikes+";\n"
				+ "var preStarred = false;\n"
				+ "var datasets = {\n";

			HashMap <String, String[]> dataMap = new HashMap<String, String[]>();
			HashMap <String, String[]> vsMap = new HashMap<String, String[]>();
			for (KeyWordTracker kwt : kws) {
				graphData = toGraph(kwt, type, startTime, endTime, vsTime, false);
				String kw = kwt.getKeyWord();
				if (graphData[6].equals("0") || graphData[6].equals("1")) {
					logger.fine("[Web]: Skipping "+kw+ " due to insufficient data points - "+ graphData[6]);
					nodata = nodata + "\t<li>No data in timerange for "+kw+"</li>\n";
                                        deleteAll = deleteAll + "^"+kw+"$ ";
					continue;
				}
				dataMap.put(kw, graphData);

				if (vsTime != 0) {
					graphData = toGraph(kwt, type, startTime, endTime, vsTime, true);
					vsMap.put(kw, graphData);
				}
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
			output = output + "graph"+idx +": {\n";
			body = body
				+ "<div id=\"sortable\">\n"
				+ "\t<ul data-id=\"donotsavetodashboard\" class=\"graphListing\">\n";

                        int mergedGraphs = 0;
			String keysInGraph = new String();
                        for (int j=0 ; j < totalKeys ; j++) {
                                Map.Entry<String, String[]> foo = sortedMap.pollLastEntry();
				String kw = foo.getKey();
                                graphData = foo.getValue();

				if (keysInGraph.length() > 0)
					keysInGraph += " ";
				keysInGraph += kw;

				output = output + "\t\"" + kw.replaceAll("\\.","_") + "\": {\n"
					+ "\t\tkeyword: \""+kw+"\",\n"
					+ "\t\tlabel: \""+kw+" ("+graphData[12]+onl.getUnits(kw)+")\",\n";

				if (showDetails) {
					body = body
						+ "\t\t<li class=\"draggable\">\n"
						+ "\t\t\t<a href = \""+kw+"\">"+kw+"</a>\n"
                                                + "\t\t\t<div style=\"text-align: right\">\n"
						+ "<span id=output_values>type: "+ onl.getType(kw) +"</span>"
						+ "<span id=output_values>min: "+graphData[0]+"</span>"
						+ "<span id=output_values>max: "+graphData[1]+"</span>"
						+ "<span id=output_values>95%: "+graphData[4]+"</span>"
						+ "<span id=output_values>99%: "+graphData[11]+"</span>"
						+ "<span id=output_values>now: "+graphData[5]+"</span>"
                                                + "\t\t\t</div>\n"
						+ "\t\t</li>\n";
				}

				
				output = output
					+ "\t\tnn: "+ graphData[13] + ",\n"
					+ "\t\tdata: [\n"
					+ graphData[3]
					+ "\t\t]\n\t},\n\n";

				if (vsTime != 0) {
					graphData = vsMap.get(kw);
					output = output + "\t\"" + kw.replaceAll("\\.","_") + "@vs\": {\n"
						+ "\t\tlabel: \""+kw+"@vs ("+graphData[12]+onl.getUnits(kw)+")\",\n"
						+ "\t\tnn: "+ graphData[13] + ",\n"
						+ "\t\tdata: [\n"
						+ graphData[3]
						+ "\t\t]\n\t},\n\n";
				}

                                drawnGraphs++;
				if (drawnGraphs % maxMergeCount == 0 || drawnGraphs >= graphLimit) {
					body = body
						+ "\t\t<div id=\"placeholder_"+(idx+mergedGraphs)+"\" class=\"mergedGraph\"></div>\n"
						+ "\t</ul>\n";

                                        if (onl.dashContainsKey(currentDashboard, keysInGraph, (type == OtiNanai.GRAPH_STACKED)))
                                                output += "\tpreStarred: true,\n";
					keysInGraph = new String();

                                        mergedGraphs++;
					output = output 
						+ "},\n";
                                        if (drawnGraphs < graphLimit) {
						output += "graph"+ (idx + mergedGraphs) +": {\n";
						body += "\t<ul data-id=\"donotsavetodashboard\" class=\"graphListing\">\n";
					}
                                        else
                                                break;
				}
			}
			body = body
				+ "</div>\n"
				+ "<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.sortable.js\"></script>\n";
		}

                if (nodata.length() > 0 )
                        nodata = "<ul class=\"nodata\">\n"+nodata + "<li><a href=\""+deleteAll+" --delete\">Delete Empty</a>&nbsp;</li>\n</ul>\n";

		if (type != OtiNanai.GRAPH_GAUGE && type != OtiNanai.GRAPH_NONE) {
			output = output + "};\n"
				+ commonHTML(OtiNanai.ENDJS)
				+ commonHTML(OtiNanai.ENDHEAD)
				+ body
				+ nodata
				+ commonHTML(OtiNanai.ENDBODY);
		}

		return output;
	}

	private String starList() {
		String output = commonHTML(OtiNanai.ENDHEAD) + "<ul>\n";
		LLString starList = (LLString)onl.getStarList().clone();
		int size = onl.getStarList().size();
		String star;
		for (int i=0; i < size ; i++) {
			star = starList.removeLast();
                        try {
                                output = output + "<li><a href=\""+URLEncoder.encode(star, "UTF-8")+"\">"+star+"</a></li>\n";
                        } catch (UnsupportedEncodingException uee) {
                                output = output + "<li><a href=\""+star+"\">"+star+"</a></li>\n";
                        }
		}
		output = output + "</ul>\n" + commonHTML(OtiNanai.ENDBODY);
		return output;
	}

	private String storeList() {
		String output = commonHTML(OtiNanai.ENDHEAD)
                        + "<div id=\"tickBox\" class=\"fa fa-4x fa-check\"></div>\n"
                        + "<ul class=\"storeList\">\n";
		LLString storeList = (LLString)onl.getStoreList().clone();
		int size = onl.getStoreList().size();
		String stored;
		for (int i=0; i < size ; i++) {
			stored = storeList.removeLast();
                        try {
                                output = output
                                        + "<li><a href=\""+URLEncoder.encode(stored, "UTF-8")+"\">"+stored+"</a>"
                                        + "</li>\n"
                                        + "<span class=\"runXHR fa fa-1x fa-rotate-right\" onclick='runXHR(\""+URLEncoder.encode(stored, "UTF-8")+"\")'></span>\n";
                        } catch (UnsupportedEncodingException uee) {
                                output = output + "<li><a href=\""+stored+"\">"+stored+"</a></li>\n";
                        }
		}
		output = output + "</ul>\n" + commonHTML(OtiNanai.ENDBODY);
		return output;
	}


	private String kwTree(ArrayList<String> kws, String[] existingKeyWords, ArrayList<String> words) {
		TreeMap<String, Integer> keyMap = new TreeMap<String, Integer>();
		HashMap<String, LLString> keyContents = new HashMap<String, LLString>();
		LLString contents;
		String tmp;
		int sofar;
		int totalCount = kws.size();
		int length = 0;
		int nextWB = 0;
		LLString wordBreaks = new LLString();
		wordBreaks.add(".");
		wordBreaks.add(",");
		wordBreaks.add("_");
		wordBreaks.add("-");

		for (String word : words) {
			if (word.equals("*"))
				word = "";
			ArrayList<String> tmpAS = new ArrayList<String>();
			tmpAS.addAll(kws);
			for (String kw : tmpAS) {
				if (kw.contains(word)) {
					length=kw.indexOf(word)+word.length();
					tmp = kw.substring(0,length);
					for (String wb : wordBreaks) {
						nextWB=kw.indexOf(wb, length+1);
						if (nextWB > 0) {
							tmp = kw.substring(0,nextWB+1);
							break;
						}
					}

					sofar = 0;
					if (keyMap.containsKey(tmp)) {
						sofar = keyMap.get(tmp);
					}
					keyMap.put(tmp, ++sofar);

					if (keyContents.containsKey(tmp))
						contents = keyContents.get(tmp);
					else
						contents = new LLString();
					contents.add(kw);
					keyContents.put(tmp, contents);
				}
			}
		}

		String oldKeys = new String();
		for (String foo : existingKeyWords) {
			oldKeys = oldKeys + foo + " ";
		}
		oldKeys = oldKeys.substring(0,oldKeys.length()-1);

		String output = commonHTML(OtiNanai.ENDHEAD)
			+ "<ul><li><a href=\""+oldKeys.replaceAll("\\+","%2B") + " --sa --merge\">Show All (slow) (--sa) "+totalCount+"</a></li>\n";

		for (String key : keyMap.keySet()) {
			String title = new String();
			String url = new String("^"+key);
			contents = keyContents.get(key);
			Collections.sort(contents);

			//Special case. if you have foo.bar and foo.bar.lala, clicking foo.bar shows the same
			if (Arrays.asList(existingKeyWords).contains(url)) {
				for (String toExclude : keyMap.keySet()) {
					if (toExclude.equals(key))
						continue;
					url += " -^"+toExclude;
				}
			}

			for (String foo : contents)
				title += foo+"\n";
			output = output + "<li><a title=\""+title+"\" href=\""+url+" --merge\">"+key+" "+keyMap.get(key)+"</a></li>\n";
		}
		output = output + "</ul>\n" + commonHTML(OtiNanai.ENDBODY);
		return output;
	}

	private String commonHTML(short out) {
		if (out == OtiNanai.HEADER) {
			return new String("<html><head>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"otinanai.css\" />\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.js\"></script>\n"
					+ "<link rel=\"stylesheet\" href=\"//maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css\"/>\n"
                                        + "<meta name=\"viewport\" content=\"width=device-width\" initial-scale=\"1.0\">\n"
					+ "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'/>\n");
		} else if (out == OtiNanai.ENDHEAD) {
			return new String("</head>\n<body>\n");
		} else if (out == OtiNanai.ENDBODY) {
			return new String("</body>\n</html>\n");
		} else if (out == OtiNanai.REFRESH) {
			if ( OtiNanai.TICKER_INTERVAL == 0 )
				return new String();
			return new String("<meta http-equiv=\"refresh\" content="+OtiNanai.TICKER_INTERVAL/1000+">\n");
		} else if (out == OtiNanai.GAGE) {
			return new String("<script src=\"raphael.min.js\"></script>\n"
					+ "<script src=\"justgage.min.js\"></script>\n");
		} else if (out == OtiNanai.FLOT) {
			return new String("<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.time.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.crosshair.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.selection.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.stack.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.events.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"jquery.flot.resize.min.js\"></script>\n"
					//https://github.com/RubaXa/Sortable
					+ "<script src=\"//cdn.jsdelivr.net/sortable/latest/Sortable.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.flot.js\"></script>\n");
		} else if ( out == OtiNanai.JS) {
			return new String("<script type=\"text/javascript\">\n");
		} else if (out == OtiNanai.ENDJS) {
			return new String ("</script>\n");
		} else if (out == OtiNanai.STUPIDTABLE) {
			return new String("<script language=\"javascript\" type=\"text/javascript\" src=\"stupidtable.min.js\"></script>\n"
					+ "<script language=\"javascript\" type=\"text/javascript\" src=\"otinanai.stupidtable.js\"></script>\n");
		}
		return new String();
	}


	private String searchBar(String input, String currentDashboard) {
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
			+ "<a href=\"--dashboard --nd\" class=\"goToDashboard fa fa-dashboard fa-2x\"></a>"
			+ "<span id=\"currentDashboard\" onclick=\"showDashSelector()\">\n"
			+ currentDashboard
			+ "</span>\n"
			//+ "<span id=\"star\" class=\"fa "+ (onl.isStarred(input) ? "fa-star" : "fa-star-o") + " fa-2x\" "
			//+ "onClick=\"toggleStar('"+input+"')\" ></span>\n"
/*
			+ "<div class=\"helpTrigger fa fa-info-circle fa-2x\">\n"
			+ " <div>\n"
			+ "  <ul class=\"helpContent\">\n"
			+ "   <lh>Examples</lh>\n"
			+ "   <li>thor.hammer odin.spear +output --merge</li>\n"
			+ "   <li>mysql +myserver -rubbish</li>\n"
                        + "   <li>order matters +servername -netstuff otherserver.netstuff -something</li>"
			+ "   <li>dataroom +temperature @24</li>\n"
			+ "   <li>interesting.data @5d-30d</li>\n"
			+ "   <li>dataroom +temperature --gauge</li>\n"
			+ "   <li>server1 server2 server3 +usage --stack</li>\n"
			+ "   <li>^du +data$</li>\n"
			+ "   <li>something --show-spikes</li>\n"
			+ "   <li>many methods --show-all</li>\n"
			+ "   <li>many methods --show-all --merge --limit 30</li>\n"
			+ "   <li>show.me.five.per.graph --merge #5</li>\n"
			+ "   <li>something.interesting --no-refresh</li>\n"
			+ "   <li>something.to.embed --no-bar</li>\n"
			+ "   <li>bandwidth --multiplier 8</li>\n"
			+ "   <li>bandwidth --units bps</li>\n"
			+ "   <li>crap --delete</li>\n"
			+ "   <li>dont.care --noalarm</li>\n"
			+ "   <li>do.care --enable-alarm</li>\n"
			+ "  </ul>\n"
			+ " </div>\n"
			+ "</div>\n"
*/
			//+ "<a class=\"github fa fa-github fa-2x\" href=\"https://github.com/rkrambovitis/otinanai\"></a>\n"
			+ "</form>\n"
			+ "<!-- END search bar -->\n\n"
			+ "<script>onload = function () { document.getElementById('q').selectionStart = document.getElementById('q').value.length;}</script>\n"
			+ getDashSelector();
		return searchBar;
	}


	private String getDashSelector() {
		LLString list = onl.getDashBoardList();
		String op =
			"<span id=\"dashSelector\">\n"
			+ "<ul>\n";
		for ( String s : list ) {
			op = op + "\t<li onclick=\"setDashboard('"+s+"')\">"+s+"</li>\n";
		}
		op = op
			+ "\t<li><input type=\"text\" id=\"NewDashboard\"/> <span onclick=\"setDashboard(document.getElementById('NewDashboard').value)\">add</span></li>\n"
			+ "</ul>\n"
			+ "</span>\n";
		return op;
	}

	private String webTitle(String search) {
		return new String("<title>OtiNanai Graphs|" + search+"</title>\n");
	}

	private String showKeyWords(String input, String currentDashboard, boolean cache) {
		String op = onc.getCached(input);
		if (!cache) {
			logger.info("[Web]: non-cached result requested");
		} else if (op != null) {
			logger.info("[Web]: cached: \"" + input + "\"");
			return op;
		} else
			logger.info("[Web]: Not cached: \"" + input + "\"");

		if (input.contains("--toggledashboard")) {
			input = input.replaceFirst("--toggledashboard", "");
			return String.valueOf(onl.toggleDashboard(input, currentDashboard));
		}

		if (input.contains("--updatedashboard")) {
			input = input.replaceFirst("--updatedashboard", "");
			return String.valueOf(onl.updateDashboard(input, currentDashboard));
		}

		String [] keyList = input.split("[ ,]|%20");
		logger.fine("[Web]: Searching for keywords");
		//Collection<KeyWordTracker> allKWTs = onl.getTrackerMap().values();
		LLString allKWTs = new LLString();
                try {
                        allKWTs.addAll(onl.getKWTList());
                } catch (ArrayIndexOutOfBoundsException aioobe) {
			return new String("Pls let me know what you were doing at the time :) <br /><br />\n\n" +aioobe.getCause());
                }
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
                boolean autoRefresh = true;
                boolean showSpikes = false;
		boolean showDetails = true;
		boolean showDashboard = false;
		long vsTime = 0l;
		boolean nextWordIsVsTime = false;
		LLString properOrder = new LLString();
		LLString pluses = new LLString();
		LLString minuses = new LLString();
		LLString special = new LLString();
		boolean isArg = false;
		for (String word:keyList) {
			if (word.length() == 0)
				continue;
			firstChar = word.substring(0,1);
			secondChar = "";
			if (word.length() > 1)
                                secondChar = word.substring(1,2);

			if (isArg) {
				special.add(word);
				isArg = false;
			} else if (word.equals("--limit") || word.equals("--units") || word.equals("--multiplier") || word.equals("--vs")) {
				special.add(word);
				isArg = true;
			}
			else if (firstChar.equals("+"))
				pluses.add(word);
			else if (firstChar.equals("-") && !secondChar.equals("-"))
				minuses.add(word);
			else if (firstChar.equals("@") || firstChar.equals("#") || (firstChar.equals("-") && secondChar.equals("-")))
				special.add(word);
			else
				properOrder.add(word);
		}
		if (properOrder.size() == 0)
			properOrder.add(new String("*"));
		properOrder.addAll(pluses);
		properOrder.addAll(minuses);
		properOrder.addAll(special);

		for (String word : properOrder) {
			if (word.length() == 0)
				continue;
			if (nextWordIsUnit) {
				units=word;
				nextWordIsUnit = false;
				continue;
			}
                        if (nextWordIsLimit) {
                                try {
                                        graphLimit = Integer.parseInt(word);
                                        nextWordIsLimit = false;
					showAll = true;
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
			if (nextWordIsVsTime) {
				try {
					lastChar = word.substring(word.length()-1);
					rest = word.replaceAll("[dh]", "");
					//logger.info("[Web]: word: "+word +" rest: "+rest +" lastChar: "+lastChar);
					long multiplier = 3600000l;
					if (lastChar.equals("d"))
						multiplier = 86400000l;
					vsTime = Integer.parseInt(rest) * multiplier;
				} catch (NumberFormatException nfe) {
					logger.severe("[Web]: "+nfe.getCause());
				} finally {
					nextWordIsVsTime = false;
				}
				logger.info("[Web]: vsTime is "+vsTime);
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
				case "--nd":
				case "--no-details":
					showDetails = false;
					continue;
				case "--ng":
				case "--no-graphs":
					graphType = OtiNanai.GRAPH_NONE;
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
					setUnits=true;
					nextWordIsUnit=true;
					continue;
                                case "--multiplier":
                                        setMultip=true;
                                        nextWordIsMultip=true;
                                        continue;
                                case "--limit":
                                        nextWordIsLimit=true;
                                        continue;
				case "--vs":
					nextWordIsVsTime = true;
					continue;
				case "--dashboard":
					showDashboard = true;
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
				case "--pcnt":
				case "--percentiles":
					graphType = OtiNanai.GRAPH_PERCENTILES;
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
                                case "--no-auto-refresh":
                                case "--no-refresh":
                                case "--nar":
                                case "--nr":
                                        autoRefresh = false;
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
                                        try {
                                                if ((kw.contains(word) || rest.equals("*")) && !kws.contains(kw))
                                                        kws.add(kw);
                                        } catch (NullPointerException npe) {
                                                logger.info("[Web]: word is \""+word+"\"");
                                        }
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
			String unitsOP = new String();
                        unitsOP = "<div class=\"aTitle\">Setting units to "+units+" for keywords:</div>\n"
                                + "<div class=\"aDescription\">use <a href=\"--stored\">--stored</a> to view history</div>\n"
                                + "<ul>\n";
			for (String kw : kws) {
				logger.info("[Web]: Setting "+kw+" units to "+units);
				unitsOP = unitsOP + "<li>"+kw+"</li>\n";
				onl.setUnits(kw, units);
			}
                        unitsOP = unitsOP + "</ul>\n"
                                + commonHTML(OtiNanai.ENDBODY);
                        onl.storeQuery(input);
			return unitsOP;
		} else if (setMultip) {
			logger.info("[Web]: Setting matching Keyword Multiplier to "+multip);
			String multipOP = new String();
                        multipOP = "<div class=\"aTitle\">Setting multiplier to "+multip+" for keywords:</div>\n"
                                + "<div class=\"aDescription\">use <a href=\"--stored\">--stored</a> to view history</div>\n"
                                + "<ul>\n";
			for (String kw : kws) {
				logger.info("[Web]: Setting "+kw+" multiplier to "+multip);
				multipOP = multipOP + "<li>"+kw+"</li>\n";
				onl.setMultiplier(kw, multip);
			}
                        multipOP = multipOP + "</ul>\n"
                                + commonHTML(OtiNanai.ENDBODY);
                        onl.storeQuery(input);
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

		if (showDashboard)
			graphType = OtiNanai.GRAPH_DASHBOARD;
		else if (!showAll && kws.size() > OtiNanai.MAXPERPAGE) {
			logger.info("[Web]: Exceeded MAXPERPAGE: "+ kws.size() + " > " +OtiNanai.MAXPERPAGE);
			return kwTree(kws, keyList, words);
		}
		op  = timeGraph(kws, graphType, startTime, endTime, maxMergeCount, showEvents, graphLimit, autoRefresh, showSpikes, showDetails, currentDashboard, vsTime);
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
