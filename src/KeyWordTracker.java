package gr.phaistosnetworks.admin.otinanai;

import java.util.logging.*;
import java.util.*;

interface KeyWordTracker {
	public String getKeyWord() ;

   public short getType();

   public void setType(short type);

	public void put() ;

   public void delete() ;

   public void put(float value) ;

   public void put(long value) ;

   public void tick() ;

	public long getAlarm() ;

   public LinkedList<String> getMemory() ;

   public long getCurrentCount() ;
}
