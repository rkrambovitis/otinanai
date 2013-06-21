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
		Vector<SomeRecord> matched = new Vector<SomeRecord>();
		String[] inputWords = input.split("\\s");
		String primary = inputWords[0];
		matched = addWord(matched, primary);
		String word;
		String rest;	
		String firstChar = new String();
		for (int i=1; i<inputWords.length; i++) {
			word = inputWords[i];
			firstChar = word.substring(0,1);
			rest = word.substring(1);
			if (firstChar.equals("-")) {
				matched = delWord(matched, rest);
			} else if (firstChar.equals("+")) {
				matched = addWord(matched, rest);
			} 
		}
		return matched;
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
}
