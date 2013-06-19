import java.io.*;
import java.net.*;
import java.util.concurrent.*;
//import java.util.Date;
//import java.sql.Timestamp;

class OtiNanai {
	public static void main(String args[]) {
		OtiNanai foo = new OtiNanai();
		foo.init();
		foo.mainLoop();
	}

	private void mainLoop() {
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
		System.out.println(newRecord.getTimeStamp() + " " + newRecord.getHostName() + " " + newRecord.getRecord());
		storage.add(newRecord);
	}

	private void init() {
		storage = new CopyOnWriteArrayList<SomeRecord>();
	}

	CopyOnWriteArrayList<SomeRecord> storage;
}
