package gr.phaistosnetworks.admin.otinanai;

import com.basho.riak.client.*;

import java.util.logging.*;
import java.util.*;

interface KeyWordTracker {
	public String getKeyWord() ;

	public void put() ;

   public void delete() ;

   public void put(float value) ;

   public void put(long value) ;

   public void tick(long ts) ;

	public long getAlarm() ;

   public LinkedList<String> getMemory() ;

   public long getThirtySecCount() ;

   public long getFiveMinCount() ;

   public long getThirtyMinCount() ;
}
