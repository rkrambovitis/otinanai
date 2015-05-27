package gr.phaistosnetworks.admin.otinanai;

import java.util.logging.*;
import java.util.*;

interface KeyWordTracker {
        public String getKeyWord() ;

        public short getType();

        public void setType(short type);

        public void putFreq() ;

        public void delete() ;

        public void putCounter(long value) ;

        public void putGauge(float value) ;

        public void putSum(float value) ;

        public void tick() ;

        public long getAlarm() ;

        public void alarmEnabled(boolean onOrOff) ;
	
        public boolean alarmEnabled() ;

        public ArrayList<String> getMemory(Long startTime) ;

        public long getCurrentCount() ;
}
