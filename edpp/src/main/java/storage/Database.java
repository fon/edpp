package storage;

import domain.RecordedSession;
import event.SessionListener;

/**
 * Interface for the database component used for storing the completed Sessions
 * 
 * @author Xenofon Foukas
 * 
 */
public interface Database {

	/**
	 * Adds a completed session to the database
	 * 
	 * @param rs
	 *            a session of type RecordedSession, i.e. containing a timestamp
	 */
	public void addSession(RecordedSession rs);

	/**
	 * Finds the last Session stored in the database
	 * 
	 * @return an object of type RecordedSession containing the last session
	 *         stored
	 */
	public RecordedSession getLastRecordedSession();

	/**
	 * This method adds a SessionListener to the database, for notifying an
	 * object when a new event related to the database occurs
	 * 
	 * @param listener
	 *            the new SessionListener to be added
	 */
	void addSessionListener(SessionListener listener);

	/**
	 * This method removes a SessionListener from the database
	 * 
	 * @param listener
	 *            the SessionListener to be removed
	 * @return true if the listener existed and was removed, false otherwise
	 */
	boolean removeSessionListener(SessionListener listener);

	/**
	 * This method closes the database, once the protocol terminates. It should
	 * be called only in the end
	 */
	public void closeDatabase();

}
