class OtiNanai {
	public OtiNanai(){
		OtiNanaiListener onl = new OtiNanaiListener();
		new Thread(onl).start();
		OtiNanaiCommander onc = new OtiNanaiCommander(onl);
		new Thread(onc).start();
	}

	public static void main(String args[]) {
		OtiNanai non = new OtiNanai();
	}
}
