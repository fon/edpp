package domain;

import java.io.Serializable;

public class RecordedSession implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8424294980274800118L;
	private final long timestamp;
	private final Session recordedSession;
	
	public RecordedSession(Session recordedSession) {
		timestamp = System.currentTimeMillis();
		this.recordedSession = recordedSession;
	}
	
	public Session getRecordedSession() {
		return recordedSession;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
}
