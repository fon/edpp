package storage;



import java.util.Stack;

import domain.RecordedSession;
import event.SessionListener;

public class FakeDatabase implements Database {

	private Stack<RecordedSession> storedSessions;
	
	public FakeDatabase() {
		storedSessions = new Stack<RecordedSession>();
	}
	
	@Override
	public void addSession(RecordedSession rs) {
		storedSessions.add(rs);
	}

	@Override
	public RecordedSession getLastRecordedSession() {
		if (storedSessions.isEmpty())
			return null;
		return storedSessions.peek();
	}

	@Override
	public void addSessionListener(SessionListener listener) {
		return;
	}

	@Override
	public boolean removeSessionListener(SessionListener listener) {
		return true;
	}

	@Override
	public void closeDatabase() {
		return;
	}

}
