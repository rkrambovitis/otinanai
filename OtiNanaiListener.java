import java.io.*;
import java.net.*;
import java.util.concurrent.*;

class OtiNanaiListener implements Runnable {
	public OtiNanaiListener() {
		storage = new CopyOnWriteArrayList<SomeRecord>();
	}

	public void run() {
		try {
			DatagramSocket serverSocket = new DatagramSocket(9876);
			while(true) {
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				try {
					serverSocket.receive(receivePacket);
				} catch (IOException ioer) {
					System.out.println(ioer);
					continue;
				}
				String sentence = new String(receivePacket.getData());
				InetAddress IPAddress = receivePacket.getAddress();
				parseData(IPAddress, sentence);
			}
		} catch (SocketException socker) {
			System.out.println(socker);
			System.exit(0);
		}
	}
	
	private void parseData(InetAddress hip, String theDato) {
		SomeRecord newRecord = new SomeRecord(hip, theDato);
		storage.add(newRecord);
	}

	public CopyOnWriteArrayList<SomeRecord> getData() {
		return storage;
	}

	CopyOnWriteArrayList<SomeRecord> storage;
}
