import java.io.*;
import java.util.*;
import java.util.concurrent.*;

class OtiNanaiProcessor {
	public OtiNanaiProcessor(OtiNanaiListener o) {
		setONL(o);
	}

	public void setONL(OtiNanaiListener o) {
		onl = o;
	}


	public Vector<SomeRecord> processCommand(String input) {
		storage = onl.getData();
		dataMap = onl.getDataMap();
		keyMaps = onl.getKeyMaps();
		Vector<SomeRecord> matched = new Vector<SomeRecord>();
		ArrayList<String> matchedIDs = new ArrayList<String>();
		String[] inputWords = input.split("\\s");
		String primary = inputWords[0];
	//	matched = addWord(matched, primary);

		matchedIDs = addWord(matchedIDs, primary);
		
		String word;
		String rest;	
		String firstChar = new String();
		for (int i=1; i<inputWords.length; i++) {
			word = inputWords[i];
			firstChar = word.substring(0,1);
			rest = word.substring(1);
			if (firstChar.equals("-")) {
				//matched = delWord(matched, rest);
			} else if (firstChar.equals("+")) {
				//matched = addWord(matched, rest);
				matchedIDs = addWord(matchedIDs, rest);
			} 
		}
		return matched;
	}

	private ArrayList<String> addWord(ArrayList<String> mid, String key) {
		if (keyMaps.containsKey(key)) 
			mid.addAll(keyMaps.get(key));
		return mid;
	}

	private ArrayList<String> delWord(ArrayList<String> mid, String key) {
		if (keyMaps.containsKey(key)) 
			mid.removeAll(keyMaps.get(key));
		return mid;
	}

	private Vector<SomeRecord> addWord(Vector<SomeRecord> matched, String key) {
		for (SomeRecord aRecord : storage ) {
			if (aRecord.hasKeyword(key)) {
				matched.add(aRecord);
			}
		}
		return matched;
	}

	private Vector<SomeRecord> delWord(Vector<SomeRecord> matched, String key) {
		Vector<SomeRecord> toDel = new Vector<SomeRecord>();
		for (SomeRecord aRecord : matched ) {
			if (aRecord.hasKeyword(key)) {
				toDel.add(aRecord);
			}
		}
		for (SomeRecord foo : toDel) {
			matched.remove(foo);
		}
		return matched;
	}

	private OtiNanaiListener onl;
	private CopyOnWriteArrayList<SomeRecord> storage;
	private HashMap<String,SomeRecord> dataMap;
	private HashMap<String,ArrayList<String>> keyMaps;
}
