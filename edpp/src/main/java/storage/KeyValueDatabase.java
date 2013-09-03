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

/**
 * Class for providing storage capabilities to the protocol using key-value
 * pairs. The underlying storage mechanism used is jdbm2
 * 
 * @author Xenofon Foukas
 * 
 */
public class KeyValueDatabase implements Database {

	private RecordManager recMan;
	private PrimaryTreeMap<Integer, RecordedSession> db;

	private final Object lock;

	private List<SessionListener> sessionListeners;

	/**
	 * Constructor class
	 * 
	 * @param dbName
	 *            A string with the name of the database where the sessions are
	 *            stored. If no database exists with that name, it will be
	 *            created
	 * @param tableName
	 *            A string with the name of the table in the database for
	 *            storing completed Session information. If such a table does
	 *            not exist, it will be created
	 */
	public KeyValueDatabase(String dbName, String tableName) {
		sessionListeners = Collections
				.synchronizedList(new ArrayList<SessionListener>());
		lock = new Object();
		try {
			recMan = RecordManagerFactory.createRecordManager(dbName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		db = recMan.treeMap(tableName);
	}

	@Override
	public void addSession(RecordedSession rs) {
		int size;
		synchronized (lock) {
			try {
				// Compute the index of the new record by finding the index of
				// the last stored session and adding one to it
				size = db.lastKey() + 1;
			} catch (NoSuchElementException nse) {
				// If no previous record is found, the first index will be 0
				size = 0;
			}
			db.put(new Integer(size), rs);
		}
		// Notify all listeners that a new record was inserted
		synchronized (sessionListeners) {
			Iterator<SessionListener> iter = sessionListeners.iterator();
			while (iter.hasNext()) {
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
				// Get the index of the last recorded session
				lastRecord = db.lastKey();
			} catch (NoSuchElementException e) {
				// If no index is found, set it to 0
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
			// commit any changes before closing the database
			recMan.commit();
			recMan.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
