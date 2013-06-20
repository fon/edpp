package core;

import java.util.Map;
import java.util.UUID;

public class Session {

	private UUID sessionId;
	private Map<Integer, Execution> executions;
	private boolean initiator;
	
	public Session() {
		sessionId = UUID.randomUUID();
		this.initiator = false;
	}
	
	public Session(boolean initiator) {
		sessionId = UUID.randomUUID();
		this.initiator = initiator;
	}
	
	public String getSessionId() {
		return sessionId.toString();
	}
	
	/**
	 * 
	 * @param executionNumber
	 * @return The execution that corresponds to executionNumber
	 */
	public Execution getExecution(int executionNumber) {
		return executions.get(executionNumber);
	}
	
	/**
	 * 
	 * @return True if this is the node that initiated the session
	 */
	public boolean isInitiator() {
		return initiator;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null) return false;
		if (!(obj instanceof Session)) return false;
		Session s = (Session)obj;
		
		return this.sessionId.equals(s.getSessionId());
	}
	
	@Override
	public int hashCode() {
		return sessionId.hashCode();
	}


	
}
