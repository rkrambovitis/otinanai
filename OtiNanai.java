import java.io.*;
import java.net.*;
//import java.util.Date;
//import java.sql.Timestamp;

class OtiNanai {
	public static void main(String args[]) {
		OtiNanai foo = new OtiNanai();
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
				//System.out.println(IPAddress.getHostName().toString() + " Sent us: " + sentence);
				parseData(IPAddress, sentence);
			}
		} catch (SocketException socker) {
			System.out.println(socker);
			System.exit(0);
		}
	}
	
	private void parseData(InetAddress hip, String theDato) {
//		Timestamp now = new Timestamp(new Date().getTime());
		long timeNow = System.currentTimeMillis();
		System.out.println(timeNow + " " + hip.getHostName() + " " + theDato);
		String typeOfDato = new String();

		/*
		String[] tokenakia = theDato.split("\\s");
		for (int i=0; i<tokenakia.length-1; i++) {
			System.out.println("Token : " + tokenakia[i]);
		}
		*/
	}
}
