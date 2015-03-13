package gr.phaistosnetworks.admin.otinanai;

import java.util.logging.*;
import java.util.*;

class MemTracker implements KeyWordTracker {

	public MemTracker(String key, int as, float at, int acs, Logger l) {
		mean = 0f;
		currentCount = 0;
		alarmSamples = as;
		alarmThreshold = at;
		alarmConsecutiveSamples = acs;
		alarmCount = 0;
		step1Memory = new ArrayList<String> ();
		step2Memory = new ArrayList<String> ();
		step3Memory = new ArrayList<String> ();
		keyWord = new String(key);
		logger = l;
		sampleCount = 1;
		currentDataCount = 0;
		currentFloat = 0f;
		currentLong = 0l;
		currentPrev = 0l;
		recordType = OtiNanai.UNSET;
		alarm = 0L;
		logger.finest("[MemTracker]: new MemTracker initialized for \"" +keyWord+"\"");
	}

	public String getKeyWord() {
		return keyWord;
	}

	public void delete() {
		step1Memory = new ArrayList<String> ();
		step2Memory = new ArrayList<String> ();
		step3Memory = new ArrayList<String> ();
	}

	public void putFreq() {
		if (recordType == OtiNanai.UNSET) 
			recordType = OtiNanai.FREQ;
		if (recordType == OtiNanai.FREQ) {
			currentCount ++;
			logger.finest("[MemTracker]: currentCount is now " +currentCount);
		} else {          
			logger.fine("[MemTracker]: Ignoring put of wrong type (keyword is FREQ)");
		}
	}

	public void putCounter(long value) {
		currentLong = value;
	}

	public void putGauge(float value) {
		currentFloat += value;
		currentDataCount ++;
	}

	public void putSum(float value) {
		currentFloat += value;
	}


	public void tick() {
		long ts = System.currentTimeMillis();
		logger.fine("[MemTracker]: ticking " + keyWord );
		flush(ts);
	}

	private void flush(long ts) {
		if (recordType == OtiNanai.UNSET)
			return;
		if (recordType == OtiNanai.COUNTER && currentLong == 0l)
			return;
		if (recordType == OtiNanai.GAUGE && currentDataCount == 0)
			return;

		float perSec = 0f;
		float timeDiff = (float)(ts - lastTimeStamp);
		lastTimeStamp = ts;
		if (recordType == OtiNanai.GAUGE) {
			logger.fine("[MemTracker]: currentFloat = " +currentFloat);
			perSec = (currentFloat / currentDataCount);
			currentFloat = 0f;
			currentDataCount = 0;
		} else if (recordType == OtiNanai.SUM) {
			perSec = (1000f*currentFloat)/timeDiff;
			currentFloat = 0f;
		} else if (recordType == OtiNanai.COUNTER) {
			if (currentLong != currentPrev) {
				logger.fine("[MemTracker]: currentLong = " + currentLong);
				if (currentPrev == 0l || currentPrev > currentLong) {
					logger.fine("Last count is 0 or decrementing. Setting and Skipping");
				} else {
					float valueDiff = (float)(currentLong - currentPrev);
					perSec = ((float)((valueDiff*1000f)/timeDiff));
				}
				currentPrev = currentLong;
				currentLong = 0l;
			}
		} else if (recordType == OtiNanai.FREQ ) {
			logger.finest("[MemTracker]: currentCount = " +currentCount);
			perSec = ((float)currentCount*1000f)/timeDiff;
			logger.finest("[MemTracker]: perSec = " +perSec);
			currentCount = 0;
		}
		logger.fine("[MemTracker]: "+keyWord+" timeDiff: " +timeDiff+ " perSec: "+perSec);
		step1Memory.add(0, new String(ts+" "+String.format("%.3f", perSec)));


		if (step1Memory.size() > 2) {
			//ugly deduplication
			String dato0 = step1Memory.get(0);
			String dato1 = step1Memory.get(1);
			String dato2 = step1Memory.get(2);
			dato0 = dato0.substring(dato0.indexOf(" ") +1);
			dato1 = dato1.substring(dato1.indexOf(" ") +1);
			dato2 = dato2.substring(dato2.indexOf(" ") +1);
			if (dato0.equals(dato1)) {
				if (dato1.equals(dato2)) {
					step1Memory.remove(1);
				}
			}
		}



		/*
		 * Aggregate old 30sec samples and make 5min samples
		 */

		float lastMerge;
		long lastts;
		long tsMerge;
		String lastDato = new String();
		String lastDatoString = new String();

		if (step1Memory.size() >= OtiNanai.STEP1_MAX_SAMPLES) {
			lastMerge = 0;
			lastts = 0l;
			tsMerge = 0l;

			for (int i=1; i<=OtiNanai.STEP1_SAMPLES_TO_MERGE ; i++) {
				lastDatoString=step1Memory.get(OtiNanai.STEP1_MAX_SAMPLES - i);
				lastts = Long.parseLong(lastDatoString.substring(0,lastDatoString.indexOf(" ")));
				lastDato=lastDatoString.substring(lastDatoString.indexOf(" ")+1);

				logger.fine("[MemTracker]: Data: "+lastMerge+" += "+lastDato+" ts: "+tsMerge+" += "+lastts);
				lastMerge += Float.parseFloat(lastDato);
				tsMerge += lastts;
				step1Memory.remove(OtiNanai.STEP1_MAX_SAMPLES -i);
			}
			float finalSum = lastMerge/OtiNanai.STEP1_SAMPLES_TO_MERGE;
			long finalts = tsMerge/OtiNanai.STEP1_SAMPLES_TO_MERGE;

			logger.fine("[MemTracker]: "+keyWord+": Aggregated dataSum:"+ lastMerge + " / "+OtiNanai.STEP1_SAMPLES_TO_MERGE+" = "+finalSum+". tsSum: "+tsMerge+" / "+OtiNanai.STEP1_SAMPLES_TO_MERGE+" = "+ finalts);

			String toPush = new String(finalts+" "+String.format("%.3f", finalSum));
			step2Memory.add(0,toPush);
		}


		/*
		 * Aggregate old 5min samples and make 30min samples
		 */

		if (step2Memory.size() >= OtiNanai.STEP2_MAX_SAMPLES) {
			lastMerge = 0;
			lastts = 0l;
			tsMerge = 0;

			for (int i=1; i<=OtiNanai.STEP2_SAMPLES_TO_MERGE ; i++) {
				lastDatoString = step2Memory.get(OtiNanai.STEP2_MAX_SAMPLES - i);
				lastts = Long.parseLong(lastDatoString.substring(0,lastDatoString.indexOf(" ")));
				lastDato = lastDatoString.substring(lastDatoString.indexOf(" ")+1);
				lastMerge += Float.parseFloat(lastDato);
				tsMerge += lastts;
				step2Memory.remove(OtiNanai.STEP2_MAX_SAMPLES -i);
			}
			float finalSum = lastMerge/OtiNanai.STEP2_SAMPLES_TO_MERGE;
			long finalts = tsMerge/OtiNanai.STEP2_SAMPLES_TO_MERGE;

			logger.fine("[MemTracker]: "+keyWord+": Aggregated dataSum:"+ lastMerge + " / "+OtiNanai.STEP2_SAMPLES_TO_MERGE+" = "+finalSum+". tsSum: "+tsMerge+" / "+OtiNanai.STEP2_SAMPLES_TO_MERGE+" = "+ finalts);
			step3Memory.add(0,new String(finalts+" "+String.format("%.3f", finalSum)));
		}



		/*
		 * Alarm detection
		 */
		if ( sampleCount < alarmSamples)
			sampleCount++;
		if (mean == 0f && perSec != 0f) {
			logger.fine("[MemTracker]: mean is 0, setting new value");
			mean = perSec;
			sampleCount = 1;
		} else if (perSec != 0f) {
			logger.fine("[MemTracker]: Calculating new mean");
			float deviation = (perSec-mean)/mean;
			mean += (perSec-mean)/alarmSamples;
			logger.fine("[MemTracker]: d: "+deviation+" m: "+mean);

			if ((sampleCount >= alarmSamples) && (deviation >= alarmThreshold)) {
				alarmCount++;
				if (alarmCount >= alarmConsecutiveSamples) {
					logger.info("[MemTracker]: Error conditions met for " + keyWord + " mean: "+mean +" deviation: "+deviation+" consecutive: "+alarmCount);
					if ( alarm == 0 || (ts - alarm > OtiNanai.ALARMLIFE) ) {
						OtiNanaiNotifier onn = new OtiNanaiNotifier("Alarm Threshold Breached by "+keyWord+" mean: "+String.format("%.3f", mean) +" deviation: "+String.format("%.0f", deviation)+"x url: "+OtiNanai.WEBURL+"/"+keyWord);
						onn.send();
					}
					alarm=ts;
				} else {
					logger.info("[MemTracker]: Error threshold breached " + keyWord + " mean: "+ mean +" deviation: "+ deviation + "x count: "+alarmCount);
				}
			} else {
				alarmCount = 0;
			}
		}

		currentFloat = 0f;
		if (currentDataCount > 0)
			currentDataCount = 0;
	}

	public long getAlarm() {
		return alarm;
	}

	public ArrayList<String> getMemory(Long startTime) {
		ArrayList<String> returner = new ArrayList<String>();
		long initTime = System.currentTimeMillis();
		try {
			returner.addAll(step1Memory);
			String ldp = returner.get(returner.size()-1);
			Long lastts = Long.parseLong(ldp.substring(0,ldp.indexOf(" ")));
			if (lastts < (initTime-startTime))
				return returner;

			returner.addAll(step2Memory);
			ldp = returner.get(returner.size()-1);
			lastts = Long.parseLong(ldp.substring(0,ldp.indexOf(" ")));
			if (lastts < (initTime-startTime))
				return returner;

			returner.addAll(step3Memory);
		} catch (NullPointerException e) {
			logger.severe("[MemTracker] :"+e.getStackTrace());
		}
		return returner;
	}

	public long getCurrentCount() {
		return currentCount;
	}

	public short getType() {
		return recordType;
	}

	public void setType(short type) {
		if (recordType == OtiNanai.UNSET)
			recordType = type;
	}

	private long alarm;
	private String keyWord;
	private long currentCount;
	private float mean;
	private int sampleCount;
	private int currentDataCount;
	private float currentFloat;
	private long currentLong;
	private long currentPrev;
	private long lastTimeStamp;
	private ArrayList<String> step1Memory;
	private ArrayList<String> step2Memory;
	private ArrayList<String> step3Memory;
	private Logger logger;
	private int alarmSamples;
	private float alarmThreshold;
	private short recordType;
	private int alarmConsecutiveSamples;
	private int alarmCount;
}
