package storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import domain.RecordedSession;
import event.SessionListener;

public class KeyValueDatabase implements Database {

	private RecordManager recMan;
	private PrimaryTreeMap<Integer, RecordedSession> db;
	
	private final Object lock;
	
	private List<SessionListener> sessionListeners;
	
	public KeyValueDatabase(String dbName, String tableName) {
		sessionListeners  = Collections.synchronizedList(new ArrayList<SessionListener>());
		lock = new Object();
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
		synchronized (lock) {
			try {
				size = db.lastKey()+1;
			} catch (NoSuchElementException nse) {
				size = 0;
			}
			db.put(new Integer(size), rs);
		}
		synchronized (sessionListeners) {
			Iterator<SessionListener> iter = sessionListeners.iterator();
			while(iter.hasNext()) {
				iter.next().sessionStored(rs);
			}
		}
	}

	@Override
	public RecordedSession getLastRecordedSession() {
		int lastRecord;
		RecordedSession rs = null;
		synchronized (lock) {
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
