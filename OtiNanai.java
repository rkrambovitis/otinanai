import java.net.ServerSocket;

class OtiNanai {
	public OtiNanai(int webPort, int webThreads){
		OtiNanaiListener onl = new OtiNanaiListener(9876);
		new Thread(onl).start();
		//OtiNanaiCommander onc = new OtiNanaiCommander(onl);
		//new Thread(onc).start();
		try {
			ServerSocket ss = new ServerSocket(webPort);
			OtiNanaiWeb onw = new OtiNanaiWeb(onl, ss);
			for (int i=0; i<webThreads; i++) {
				new Thread(onw).start();
			}
		} catch (java.io.IOException ioe) {
			System.out.println(ioe);
			System.exit(1);
		}
	}

	public static void main(String args[]) {
		OtiNanai non = new OtiNanai(9876, 20);
	}
}
