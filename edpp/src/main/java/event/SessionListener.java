package event;

import comm.ProtocolMessage.SessionEvent;
import domain.RecordedSession;

public interface SessionListener {

	public void sessionInitiated(SessionEvent e);
	
	public void sessionCompleted(SessionEvent e);
	
	public void sessionStored(RecordedSession rs);
}
