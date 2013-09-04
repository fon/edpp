package event;

import comm.ProtocolMessage.SessionEvent;
import domain.RecordedSession;

/**
 * Interface for informing about Session-related events. Any object wishing to
 * be notified about a new Session event should use this interface
 * 
 * @author Xenofon Foukas
 * 
 */
public interface SessionListener {

	/**
	 * Event called when a new Session is initiated
	 * 
	 * @param e
	 *            a SessionEvent object providing information about the newly
	 *            initiated Session
	 */
	public void sessionInitiated(SessionEvent e);

	/**
	 * Event called when a Session is completed
	 * 
	 * @param e
	 *            a SessionEvent object providing information about the
	 *            terminated Session
	 */
	public void sessionCompleted(SessionEvent e);

	/**
	 * Event called when a Session is stored in the database
	 * 
	 * @param rs
	 *            a RecordedSession object providing information about the
	 *            stored Session and a timestamp of the time when the event
	 *            occurred
	 */
	public void sessionStored(RecordedSession rs);
}
