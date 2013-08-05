package storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import core.RecordedSession;
import core.SessionListener;

public class KeyValueDatabase implements Database {

	private RecordManager recMan;
	private PrimaryTreeMap<Integer, RecordedSession> db;
	
	private List<SessionListener> sessionListeners;
	
	public KeyValueDatabase(String dbName, String tableName) {
		sessionListeners  = new ArrayList<SessionListener>();
		try {
			recMan = RecordManagerFactory.createRecordManager(dbName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		db = recMan.treeMap(tableName);
	}
	
	@Override
	public void addSession(RecordedSession rs) {
		int size;
		synchronized (db) {
			try {
				size = db.lastKey()+1;
			} catch (NoSuchElementException nse) {
				size = 0;
			}
			db.put(new Integer(size), rs);
		}
		for (SessionListener sl : sessionListeners) {
			sl.sessionStored(rs);
		}
	}

	@Override
	public RecordedSession getLastRecordedSession() {
		int lastRecord;
		RecordedSession rs = null;
		synchronized (db) {
			try {
				lastRecord = db.lastKey();
			} catch (NoSuchElementException e) {
				lastRecord = 0;
			}
			try {
				rs = db.find(new Integer(lastRecord));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return rs;
	}
	
	@Override
	public synchronized void addSessionListener(SessionListener listener) {
		sessionListeners.add(listener);
	}
	
	@Override
	public synchronized boolean removeSessionListener(SessionListener listener) {
		return sessionListeners.remove(listener);
	}

	@Override
	public void closeDatabase() {
		try {
			recMan.commit();
			recMan.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
