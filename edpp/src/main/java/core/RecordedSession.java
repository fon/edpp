package core;

public class RecordedSession {

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
