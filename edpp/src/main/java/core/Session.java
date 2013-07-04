package core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import network.Node;

public class Session implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9215101229263020198L;
	
	private final UUID sessionId;
	private Map<Integer, Execution> executions;
	private final boolean initiator;
	private  AtomicInteger numberOfNextExecution;
	private final int numberOfRounds;
	private final int numberOfExecutions;
	private final transient Node localNode;
	private Execution initExecution;
	private int roundOffset;
	private AtomicInteger completedExecutions;
	private AtomicBoolean completedSession;
	private double [] computedEigenvalues;
	
	public Session(final Node localNode, final int numberOfExecutions, final int numberOfRounds) {
		sessionId = UUID.randomUUID();
		this.initiator = false;
		numberOfNextExecution = new AtomicInteger(1);
		completedExecutions = new AtomicInteger(0);
		this.numberOfExecutions = numberOfExecutions;
		this.numberOfRounds = numberOfRounds;
		this.localNode = localNode;
		executions = new ConcurrentHashMap<Integer, Execution>();
		initExecution = null;
		roundOffset = numberOfRounds/numberOfExecutions;
		completedSession = new AtomicBoolean(false);
	}
	
	public Session(final Node localNode, final int numberOfExecutions, 
			final int numberOfRounds, final boolean initiator) {
		sessionId = UUID.randomUUID();
		this.initiator = initiator;
		numberOfNextExecution = new AtomicInteger(1);
		this.numberOfExecutions = numberOfExecutions;
		this.numberOfRounds = numberOfRounds;
		this.localNode = localNode;
		executions = new ConcurrentHashMap<Integer, Execution>();
		initExecution = null;
		roundOffset = numberOfRounds/numberOfExecutions;
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
	
	/**
	 * 
	 * @return The initial Execution of the session
	 * or null if no execution has been created yet
	 */
	public Execution getInitExecution() {
		return initExecution;
	}
	
	public Execution createNewExecution() {
		return createNewExecution(numberOfNextExecution.get());
	}
	
	public Execution createNewExecution(int executionNumber) {
		if (numberOfNextExecution.get() > numberOfExecutions)
			return null;
		Execution execution = new Execution(executionNumber, numberOfRounds, localNode);
		executions.put(numberOfNextExecution.get(), execution);
		if (numberOfNextExecution.get() == 1) 
			initExecution = execution;
		numberOfNextExecution.incrementAndGet();
		return execution;
	}
	
	public int getCompletedExecutions() {
		return completedExecutions.get();
	}
	
	public boolean addCompletedExecution() {
		int exec = completedExecutions.incrementAndGet();
		boolean complete = false;
		if (exec == getNumberOfExecutions()) {
			complete = true;
			computedEigenvalues = this.majorityVotingResults();
			completedSession.set(complete);
		}
		return complete;
	}
	
	public double [] getComputedEigenvalues() {
		if (this.hasTerminated()) {
			return computedEigenvalues;
		}
		return null;
	}
	
	public boolean hasTerminated() {
		return completedSession.get();
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
	
	
	/**
	 * Checks whether a new execution should be created
	 * @return true if a new execution should be created
	 */
	public boolean newExecutionExpected() {
		int currRound = initExecution.getCurrentRound();
		//If all the expected executions have been created
		//do not create an additional one
		if (executions.size() >= numberOfExecutions)
			return false;
		return (currRound % roundOffset == 0);
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

	private double [] majorityVotingResults() {
		int sizeOfArray = initExecution.getMatrixAEigenvalues().length;
		double [] results = new double[sizeOfArray];
		
		double [][] completeResults = new double[numberOfExecutions][sizeOfArray];
		
		Collection<Execution> execs = executions.values();
		int i=0;
		for (Execution e : execs) {
			double [] median = e.getMedianEigenvalues();
			for (int j = 0; j < median.length; j++) {
				completeResults[i][j] = median[j];
			}
			i++;
		}
		
		for (int j = 0; j < sizeOfArray; j++) {
			int count = 1;
			double major = completeResults[0][j];
			
			for (i = 1; i < numberOfExecutions; i++) {
				if (major == completeResults[i][j]) {
					count++;
				} else if (count == 0) {
					major = completeResults[i][j];
					count = 1;
				} else {
					count--;
				}
				results[j] = major;
			}
		}
		return results;
	}
	
}
