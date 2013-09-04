package domain;

import java.io.Serializable;

/**
 * Class storing a terminated Session and the time of termination. When an
 * object of this class is created, a timestamp is stored indicating the time
 * the event occurred
 * 
 * @author Xenofon Foukas
 * 
 */
public class RecordedSession implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8424294980274800118L;
	private final long timestamp;
	private final Session recordedSession;

	/**
	 * Class constructor
	 * 
	 * @param recordedSession
	 *            the terminated Session object
	 */
	public RecordedSession(Session recordedSession) {
		timestamp = System.currentTimeMillis();
		this.recordedSession = recordedSession;
	}

	/**
	 * 
	 * @return Returns the terminated Session object
	 */
	public Session getRecordedSession() {
		return recordedSession;
	}

	/**
	 * 
	 * @return the timestamp of when the Session object was stored, i.e. the
	 *         timestamp of when this object was created
	 */
	public long getTimestamp() {
		return timestamp;
	}

}
