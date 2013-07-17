package core;

public interface SessionListener {

	public void sessionInitiated(SessionEvent e);
	
	public void sessionCompleted(SessionEvent e);
}
