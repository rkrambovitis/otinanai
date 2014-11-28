package gr.phaistosnetworks.admin.otinanai;

import java.util.*;

class OtiNanaiNotifier {

	public OtiNanaiNotifier(String notification) {
      command = new String(OtiNanai.NOTIFYSCRIPT+" "+notification);
   }

   public void send() {
      Process p;
      try {
         p = Runtime.getRuntime().exec(command);
         p.waitFor();

      } catch (Exception e) {
         System.err.println(e);
      }
   }

   private String command;
}
