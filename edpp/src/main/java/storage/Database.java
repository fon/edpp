package storage;

import core.RecordedSession;
import core.SessionListener;

public interface Database {

	public void addSession(RecordedSession rs);
	
	public RecordedSession getLastRecordedSession();

	void addSessionListener(SessionListener listener);

	boolean removeSessionListener(SessionListener listener);
	
	public void closeDatabase();
	
}
