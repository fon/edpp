package core;

import java.io.IOException;
import java.util.concurrent.Callable;

import jdbm.PrimaryTreeMap;
import jdbm.RecordListener;

public class ProtocolRun implements Callable<Session>, RecordListener<Integer, RecordedSession> {

	public static final long TIME_THRESHOLD = 10000;
	
	private Session s;
	private PrimaryTreeMap<Integer, RecordedSession> db;
	
	public ProtocolRun(PrimaryTreeMap<Integer, RecordedSession> db) {
		s = null;
		this.db = db;
		synchronized (db) {
			db.addRecordListener(this);
		}
	}
	
	@Override
	public Session call() throws Exception {
		
		synchronized (db) {
			
			int lastRecord = db.size();
			RecordedSession rs = db.get(lastRecord);
			
			if (System.currentTimeMillis() - rs.getTimestamp() <= TIME_THRESHOLD) {
				s = rs.getRecordedSession();
			} else {
				db.wait();
			}

			return s;
		}
		
	}

	@Override
	public void recordInserted(Integer arg0, RecordedSession arg1) throws IOException {
		synchronized (db) {
			s = arg1.getRecordedSession();
			db.notify();
		}
	}

	@Override
	public void recordRemoved(Integer arg0, RecordedSession arg1) throws IOException {
		return;
	}

	@Override
	public void recordUpdated(Integer arg0, RecordedSession arg1, RecordedSession arg2)
			throws IOException {
		return;
	}

}
