package gr.phaistosnetworks.admin.otinanai;


import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

class OtiNanaiProcessor {
	public OtiNanaiProcessor(OtiNanaiListener o) {
		setONL(o);
	}

	public void setONL(OtiNanaiListener o) {
		onl = o;
		logger = o.getLogger();
	}

	/**
	 * Send me some input and I'll give you some data
	 * Basically looks up the word you send it, and adds/removes matching unique ids into/from a List.
	 * IF the word starts with - then it looks up and removes ids.
	 * ELSE it adds the ids it finds.
	 * @param	matchedIDs	An arraylist of existing ids to add too or remove from (can be blank)
	 * @param 	input	the word to look up
	 * @return	An Arralist containing the unique ids of records
	 */
	public ArrayList<String> processCommand(ArrayList<String> matchedIDs, String input) {
		logger.info("[processor]: Attempting to processCommand \""+input+"\" on List with length "+matchedIDs.size());
		//storage = onl.getData();
		dataMap = onl.getDataMap();
		keyMaps = onl.getKeyMaps();
		memoryMap = onl.getMemoryMap();
		String word = input;
		String rest;
		String firstChar;
		firstChar = word.substring(0,1);
		rest = word.substring(1);
		if (firstChar.equals("-")) {
			matchedIDs = delWord(matchedIDs, rest);
		} else if (firstChar.equals("+")) {
			matchedIDs = addWord(matchedIDs, rest);
		} else {
			matchedIDs = addWord(matchedIDs, word);
		}
		return matchedIDs;
	}

	/**
	 * Add ids with keyword to list of ids
	 * @param mid	existing ArrayList to add to
	 * @param key	keyword
	 * @return	Arraylist containing new (and old) ids
	 */
	private ArrayList<String> addWord(ArrayList<String> mid, String key) {
		logger.fine("[processor]: Adding to search results : "+key);
		if (keyMaps.containsKey(key)) 
			mid.addAll(keyMaps.get(key));
		return mid;
	}

	/**
	 * Remove ids with keyword from list of ids
	 * @param mid	existing ArrayList to remove from
	 * @param key	keyword
	 * @return	Arraylist containing new set of ids
	 */
	private ArrayList<String> delWord(ArrayList<String> mid, String key) {
		logger.fine("[processor]: Removing from search results : "+key);
		if (keyMaps.containsKey(key)) 
			mid.removeAll(keyMaps.get(key));
		return mid;
	}

	private OtiNanaiListener onl;
	private Logger logger;
//	private CopyOnWriteArrayList<SomeRecord> storage;
	private HashMap<String,SomeRecord> dataMap;
	private HashMap<String,ArrayList<String>> keyMaps;
	private HashMap<String,OtiNanaiMemory> memoryMap;
}
