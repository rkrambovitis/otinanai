package gr.phaistosnetworks.admin.otinanai;

import com.basho.riak.client.*;
import com.basho.riak.client.bucket.*;

import java.util.logging.*;
import java.util.*;

class RiakTracker implements KeyWordTracker {
	public RiakTracker(String key, int as, float at, int acs, Logger l, Bucket bucket) {
		mean = 0f;
		currentCount = 0;
		alarmSamples = as;
		alarmThreshold = at;
		alarmConsecutiveSamples = acs;
		alarmCount = 0;
		keyWord = new String(key);
		step1Key = keyWord + "thirtySec";
		step2Key = keyWord + "fiveMin";
		step3Key = keyWord + "thirtyMin";
		riakBucket = bucket;
		logger = l;
		sampleCount = 1;
		currentDataCount = -1;
		currentFloat = 0f;
		currentLong = 0l;
		currentPrev = 0l;
		recordType = OtiNanai.UNSET;
		alarm = 0L;
		logger.finest("[RiakTracker]: new RiakTracker initialized for \"" +keyWord+"\"");
	}

	public String getKeyWord() {
		return keyWord;
	}

	public void delete() {
		try {
			riakBucket.delete(step3Key).execute();
		} catch (RiakException re) {
			logger.severe("[RiakTracker]: Failed to delete "+step3Key);
		}
		try {
			riakBucket.delete(step2Key).execute();
		} catch (RiakException re) {
			logger.severe("[RiakTracker]: Failed to delete "+step2Key);
		}
		try {
			riakBucket.delete(step1Key).execute();
		} catch (RiakException re) {
			logger.severe("[RiakTracker]: Failed to delete "+step1Key);
		}
	}

	public void put() {
		if (recordType == OtiNanai.UNSET) {
			recordType = OtiNanai.FREQ;
		}
		if (recordType == OtiNanai.FREQ) {
			currentCount ++;
			logger.finest("[RiakTracker]: currentCount is now " +currentCount);
		} else {
			logger.fine("[RiakTracker]: Ignoring put of wrong type (keyword is FREQ)");
		}
	}

	public void put(long value) {
		if (recordType == OtiNanai.UNSET)
			recordType = OtiNanai.COUNTER;
		if (recordType == OtiNanai.COUNTER) {
			currentLong = value;
			logger.finest("[MemTracker]: currentLong is now " +currentCount);
		} else {
			logger.fine("[RiakTracker]: Ignoring put of wrong type (keyword is COUNTER)");
		}
	}

	public void put(float value) {
		if (recordType == OtiNanai.UNSET)
			recordType = OtiNanai.GAUGE;
		if (recordType == OtiNanai.SUM ) {
			currentFloat += value;
		} else if (recordType == OtiNanai.GAUGE ) {
			currentFloat += value;
			currentDataCount ++;
			if (currentDataCount == 0)
				currentDataCount++;
			logger.finest("[RiakTracker]: currentFloat is now " +currentFloat);
		} else {
			logger.fine("[RiakTracker]: Ignoring put of wrong type (keyword is GAUGE)");
		}
	}

	public void tick() {
		if (recordType == OtiNanai.UNSET)
			return;
		logger.fine("[RiakTracker]: ticking " + keyWord );
		long ts = System.currentTimeMillis();
		try {
			flush(ts);
		} catch (RiakRetryFailedException rrfe) {
			logger.severe("[RiakTracker]: tick: "+rrfe);
		}
	}

	private void flush(long ts) throws RiakRetryFailedException{
		if (recordType == OtiNanai.UNSET)
			return;
		if (recordType == OtiNanai.COUNTER && currentLong == 0l)
			return;
		if (recordType == OtiNanai.GAUGE && currentDataCount == 0)
			return;

		LLString step1Memory = new LLString();
		LLString step2Memory = new LLString();
		LLString step3Memory = new LLString();

		logger.fine("[RiakTracker]: fetching existing " + step1Key);
		step1Memory = riakBucket.fetch(step1Key, LLString.class).r(1).execute();
		if (step1Memory == null ) {
			logger.fine("[RiakTracker]: null. Creating new " + step1Key);
			step1Memory = new LLString();
		}




		float perSec = 0f;
		float timeDiff = (float)(ts - lastTimeStamp);
		lastTimeStamp = ts;
		if (recordType == OtiNanai.GAUGE) {
			logger.fine("[RiakTracker]: currentFloat = " +currentFloat);
			perSec = (currentFloat / currentDataCount);
			currentFloat = 0f;
			currentDataCount = 0;
		} else if (recordType == OtiNanai.SUM) {
			perSec = (1000f*currentFloat) / timeDiff;
			currentFloat = 0f;
		} else if (recordType == OtiNanai.COUNTER) {
			if (currentLong != currentPrev) {
				logger.fine("[RiakTracker]: currentLong = " + currentLong);
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
			perSec = ((float)currentCount*1000f) / timeDiff;
			logger.info("[RiakTracker]: "+keyWord+" timeRange: " +timeDiff+ " count: "+currentCount+" perSec: "+perSec);
			currentCount = 0;
		}
		step1Memory.push(new String(ts+" "+String.format("%.3f", perSec)));

		if (step1Memory.size() > 2) {
			logger.fine("[RiakTracker]: step1Memory.size() = "+step1Memory.size());
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


		float lastMerge;
		String lastDatoString = new String();
		long lastts;
		long tsMerge;
		String lastDato = new String();

		/*
		 * Aggregate old 30sec samples and make 5min samples
		 */
		if (step1Memory.size() >= OtiNanai.STEP1_MAX_SAMPLES) {
			lastMerge = 0;
			lastts = 0l;
			tsMerge = 0l;

			logger.fine("[RiakTracker]: fetching existing " + step2Key);
			step2Memory = riakBucket.fetch(step2Key, LLString.class).r(1).execute();
			if (step2Memory == null) {
				logger.fine("[RiakTracker]: null. Creating new " + step2Key);
				step2Memory = new LLString();
			}

			for (int i=1; i<=OtiNanai.STEP1_SAMPLES_TO_MERGE ; i++) {
				lastDatoString=step1Memory.get(OtiNanai.STEP1_MAX_SAMPLES - i);
				lastts = Long.parseLong(lastDatoString.substring(0,lastDatoString.indexOf(" ")));
				lastDato=lastDatoString.substring(lastDatoString.indexOf(" ")+1);

				logger.fine("[RiakTracker]: Data: "+lastMerge+" += "+lastDato+" ts: "+tsMerge+" += "+lastts);
				lastMerge += Float.parseFloat(lastDato);
				tsMerge += lastts;
				step1Memory.remove(OtiNanai.STEP1_MAX_SAMPLES -i);
			}
			float finalSum = lastMerge/OtiNanai.STEP1_SAMPLES_TO_MERGE;
			long finalts = tsMerge/OtiNanai.STEP1_SAMPLES_TO_MERGE;

			logger.fine("[RiakTracker]: "+keyWord+": Aggregated dataSum:"+ lastMerge + " / "+OtiNanai.STEP1_SAMPLES_TO_MERGE+" = "+finalSum+". tsSum: "+tsMerge+" / "+OtiNanai.STEP1_SAMPLES_TO_MERGE+" = "+ finalts);

			String toPush = new String(finalts+" "+String.format("%.3f", finalSum));
			logger.fine("[RiakTracker]: pushing: "+step2Key+ " : " +toPush);
			step2Memory.push(toPush);
			riakBucket.store(step2Key, step2Memory).execute();
		}
		logger.fine("[RiakTracker]: Storing " + step1Key);
		riakBucket.store(step1Key, step1Memory).execute();

		/*
		 * Aggregate old 5min samples and make 30min samples
		 */
		if (step2Memory.size() >= OtiNanai.STEP2_MAX_SAMPLES) {
			lastMerge = 0;
			lastts = 0l;
			tsMerge = 0;

			logger.fine("[RiakTracker]: fetching existing " + step3Key);
			step3Memory = riakBucket.fetch(step3Key, LLString.class).r(1).execute();
			if (step3Memory == null) {
				logger.fine("[RiakTracker]: null. Creating new " + step3Key);
				step3Memory = new LLString();
			}

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

			logger.fine("[RiakTracker]: "+keyWord+": Aggregated dataSum:"+ lastMerge + " / "+OtiNanai.STEP2_SAMPLES_TO_MERGE+" = "+finalSum+". tsSum: "+tsMerge+" / "+OtiNanai.STEP2_SAMPLES_TO_MERGE+" = "+ finalts);

			step3Memory.push(new String(finalts+" "+String.format("%.3f", finalSum)));

			riakBucket.store(step3Key, step3Memory).execute();
			riakBucket.store(step2Key, step2Memory).execute();
		}


		/*
		 * Alarm detection
		 */
		if (sampleCount < alarmSamples)
			sampleCount++;

		if (mean == 0f && perSec != 0f) {
			logger.fine("[RiakTracker]: mean is 0, setting new value");
			mean = perSec;
			sampleCount = 1;
		} else if (perSec != 0f) {
			logger.fine("[RiakTracker]: Calculating new mean");
			float deviation = (perSec-mean)/mean;
			mean += (perSec-mean)/alarmSamples;
			logger.fine("[RiakTracker]: d: "+deviation+" m: "+mean);

			if ((sampleCount >= alarmSamples) && (deviation >= alarmThreshold)) {
				alarmCount++;
				if (alarmCount >= alarmConsecutiveSamples) {
					logger.info("[RiakTracker]: Error conditions met for " + keyWord + " mean: "+mean +" deviation: "+deviation+" consecutive: "+alarmCount);
					if ( alarm == 0 || (ts - alarm > OtiNanai.ALARMLIFE) ) {
						OtiNanaiNotifier onn = new OtiNanaiNotifier("Alarm Threshold Breached by "+keyWord+" mean: "+String.format("%.3f", mean) +" deviation: "+String.format("%.0f", deviation)+"x url: "+OtiNanai.WEBURL+"/"+keyWord);
						onn.send();
					}
					alarm=ts;
				} else {
					logger.fine("[RiakTracker]: Error threshold breached " + keyWord + " mean: "+mean +" deviation: "+deviation+" consecutive: "+alarmCount);
				}
			} else {
				alarmCount = 0;
			}
		}

	}

	public long getAlarm() {
		return alarm;
	}

	public LLString getMemory() {
		LLString returner = new LLString();
		try {
			LLString step1Memory = riakBucket.fetch(step1Key, LLString.class).r(1).execute();
			returner.addAll(step1Memory);
		} catch (Exception e) {
			logger.severe("[RiakTracker]: null step1Memory: "+e);
		}

		try {
			LLString step2Memory = riakBucket.fetch(step2Key, LLString.class).r(1).execute();
			logger.fine("[RiakTracker]: Got step2Memory for " + step2Key + " with size: " + step2Memory.size());
			returner.addAll(step2Memory);
			for (String foo : step2Memory) {
				logger.finest("[RiakTracker]: step2Memory listing: " +foo);
			}
		} catch (Exception e) {
			logger.fine("[RiakTracker]: null step2Memory: "+e);
		}

		try {
			LLString step3Memory = riakBucket.fetch(step3Key, LLString.class).r(1).execute();
			returner.addAll(step3Memory);
		} catch (Exception e) {
			logger.fine("[RiakTracker]: null step3Memory: "+e);
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
	public String keyWord;
	private long currentCount;
	private float mean;
	private int sampleCount;
	private int currentDataCount;
	private float currentFloat;
	private long currentLong;
	private long currentPrev;
	private long lastTimeStamp;
	private String step1Key;
	private String step2Key;
	private String step3Key;
	private Logger logger;
	private int alarmSamples;
	private float alarmThreshold;
	private Bucket riakBucket;
	private short recordType;
	private int alarmConsecutiveSamples;
	private int alarmCount;
}
