package gr.phaistosnetworks.admin.otinanai;

import java.io.*;
import java.util.*;

class OtiNanaiCommander implements Runnable {
  public OtiNanaiCommander(OtiNanaiListener o) {
    onp = new OtiNanaiProcessor(o);
  }

  public void run() {
    Console console = System.console();
    while (true) {
      String s = console.readLine();
      if (s.length() == 0) continue;
    }
  }
    
  private OtiNanaiProcessor onp;
}
