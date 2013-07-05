class OtiNanai {
	public OtiNanai(){
		OtiNanaiListener onl = new OtiNanaiListener(9876);
		new Thread(onl).start();
		//OtiNanaiCommander onc = new OtiNanaiCommander(onl);
		//new Thread(onc).start();
		OtiNanaiWeb onw = new OtiNanaiWeb(onl, 9876);
		new Thread(onw).start();
	}

	public static void main(String args[]) {
		OtiNanai non = new OtiNanai();
	}
}
