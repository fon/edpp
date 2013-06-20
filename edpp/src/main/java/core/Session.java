package core;

import java.util.Map;
import java.util.UUID;

public class Session {

	private UUID sessionId;
	private Map<Integer, Execution> executions;
	
	public Session() {
		sessionId = UUID.randomUUID();
	}
	
	public String getSessionId() {
		return sessionId.toString();
	}
	
	public Execution getExecution(int executionNumber) {
		return executions.get(executionNumber);
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
