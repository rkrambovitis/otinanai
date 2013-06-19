import java.net.InetAddress;

class SomeRecord {

	public SomeRecord(InetAddress ip, String data) {
		timeStamp = System.currentTimeMillis();
		myip = ip;
		theRecord = data;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public InetAddress getIP() {
		return myip;
	}

	public String getHostName() {
		return myip.getHostName();
	}

	public String getRecord() {
		return theRecord;
	}

	private long timeStamp;
	private InetAddress myip;
	private String theRecord;
}
