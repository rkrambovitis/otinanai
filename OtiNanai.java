import java.io.*;
import java.net.*;
import java.util.concurrent.*;

class OtiNanai {
	public static void main(String args[]) {
		OtiNanaiListener foo = new OtiNanaiListener();
		new Thread(foo).start();
	}
	
	CopyOnWriteArrayList<SomeRecord> storage;
}
