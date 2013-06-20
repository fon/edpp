package core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import network.Node;

public class Session {

	private UUID sessionId;
	private Map<Integer, Execution> executions;
	private boolean initiator;
	private int numberOfNextExecution;
	private int numberOfRounds;
	private int numberOfExecutions;
	private Node localNode;
	
	public Session(Node localNode, int numberOfExecutions, int numberOfRounds) {
		sessionId = UUID.randomUUID();
		this.initiator = false;
		numberOfNextExecution = 1;
		this.numberOfExecutions = numberOfExecutions;
		this.numberOfRounds = numberOfRounds;
		this.localNode = localNode;
		executions = new HashMap<Integer, Execution>();
	}
	
	public Session(Node localNode, int numberOfExecutions, int numberOfRounds, boolean initiator) {
		sessionId = UUID.randomUUID();
		this.initiator = initiator;
		numberOfNextExecution = 1;
		this.numberOfExecutions = numberOfExecutions;
		this.numberOfRounds = numberOfRounds;
		this.localNode = localNode;
		executions = new HashMap<Integer, Execution>();
	}
	
	public String getSessionId() {
		return sessionId.toString();
	}
	
	public int getNumberOfRounds() {
		return numberOfRounds;
	}
	
	public int getNumberOfExecutions() {
		return numberOfExecutions;
	}
	
	/**
	 * 
	 * @return The number of currently stored executions
	 */
	public int getCurrentNumberOfExecutions() {
		return executions.size();
	}
	
	public boolean createNewExecution(int executionNumber) {
		if(numberOfNextExecution > numberOfExecutions)
			return false;
		Execution execution = new Execution(executionNumber, numberOfRounds, localNode);
		executions.put(executionNumber, execution);
		numberOfNextExecution++;
		return true;
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
	public String toString() {
		return getSessionId();
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
