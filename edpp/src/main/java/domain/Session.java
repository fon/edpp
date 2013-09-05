package domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import domain.network.Node;

/**
 * Class storing all the information regarding a full protocol run. More
 * specifically this class is responsible for storing all the Executions running
 * for a single sampling request and for computing the median of the estimations
 * made by all the Executions
 * 
 * @author Xenofon Foukas
 * 
 */
public class Session implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9215101229263020198L;

	private final UUID sessionId;
	private Map<Integer, Execution> executions;
	private final boolean initiator;
	private AtomicInteger numberOfNextExecution;
	private final int numberOfRounds;
	private final int numberOfExecutions;
	private final transient Node localNode;
	private Execution initExecution;
	private int roundOffset;
	private AtomicInteger completedExecutions;
	private AtomicBoolean completedSession;
	private double[] computedEigenvalues;

	/**
	 * Constructor class. The id of this Session will be randomly generated
	 * using UUID type 4 method of generation. This Session will also have the
	 * initiator flag set to false. Should be used only when the sampling
	 * request was made in another node
	 * 
	 * @param localNode
	 *            the local underlying network node
	 * @param numberOfExecutions
	 *            the number of Executions that should be executed in the
	 *            context of this Session
	 * @param numberOfRounds
	 *            the total number of rounds each Execution in the context of
	 *            this Session should have
	 */
	public Session(final Node localNode, final int numberOfExecutions,
			final int numberOfRounds) {
		sessionId = UUID.randomUUID();
		this.initiator = false;
		numberOfNextExecution = new AtomicInteger(1);
		completedExecutions = new AtomicInteger(0);
		this.numberOfExecutions = numberOfExecutions;
		this.numberOfRounds = numberOfRounds;
		this.localNode = localNode;
		executions = new ConcurrentHashMap<Integer, Execution>();
		initExecution = null;
		roundOffset = numberOfRounds / numberOfExecutions;
		completedSession = new AtomicBoolean(false);
	}

	/**
	 * Constructor class. The id of this Session will be randomly generated
	 * using UUID type 4 method of generation.
	 * 
	 * @param localNode
	 *            the local underlying network node
	 * @param numberOfExecutions
	 *            the number of Executions that should be executed in the
	 *            context of this Session
	 * @param numberOfRounds
	 *            the total number of rounds each Execution in the context of
	 *            this Session should have
	 * @param initiator
	 *            flag that should be set to true if the sampling request was
	 *            made to local node. If the request is initiated through an
	 *            INIT message it should be set to false
	 */
	public Session(final Node localNode, final int numberOfExecutions,
			final int numberOfRounds, final boolean initiator) {
		sessionId = UUID.randomUUID();
		this.initiator = initiator;
		numberOfNextExecution = new AtomicInteger(1);
		this.numberOfExecutions = numberOfExecutions;
		completedExecutions = new AtomicInteger(0);
		this.numberOfRounds = numberOfRounds;
		this.localNode = localNode;
		executions = new ConcurrentHashMap<Integer, Execution>();
		initExecution = null;
		roundOffset = numberOfRounds / numberOfExecutions;
		completedSession = new AtomicBoolean(false);

	}

	/**
	 * Constructor class. This Session will also have the initiator flag set to
	 * false. Should be used only when the sampling request was made in another
	 * node
	 * 
	 * @param localNode
	 *            the local underlying network node
	 * @param sessionId
	 *            the id that the newly created Session should have
	 * @param numberOfExecutions
	 *            the number of Executions that should be executed in the
	 *            context of this Session
	 * @param numberOfRounds
	 *            the total number of rounds each Execution in the context of
	 *            this Session should have
	 */
	public Session(final Node localNode, final String sessionId,
			final int numberOfExecutions, final int numberOfRounds) {
		this.sessionId = UUID.fromString(sessionId);
		this.initiator = false;
		numberOfNextExecution = new AtomicInteger(1);
		this.numberOfExecutions = numberOfExecutions;
		completedExecutions = new AtomicInteger(0);
		this.numberOfRounds = numberOfRounds;
		this.localNode = localNode;
		executions = new ConcurrentHashMap<Integer, Execution>();
		initExecution = null;
		roundOffset = numberOfRounds / numberOfExecutions;
		completedSession = new AtomicBoolean(false);

	}

	/**
	 * Constructor class
	 * 
	 * @param localNode
	 *            the local underlying network node
	 * @param sessionId
	 *            the id that the newly created Session should have
	 * @param numberOfExecutions
	 *            the number of Executions that should be executed in the
	 *            context of this Session
	 * @param numberOfRounds
	 *            the total number of rounds each Execution in the context of
	 *            this Session should have
	 * @param initiator
	 *            flag that should be set to true if the sampling request was
	 *            made to local node. If the request is initiated through an
	 *            INIT message it should be set to false
	 */
	public Session(final Node localNode, final String sessionId,
			final int numberOfExecutions, final int numberOfRounds,
			final boolean initiator) {
		this.sessionId = UUID.fromString(sessionId);
		this.initiator = initiator;
		numberOfNextExecution = new AtomicInteger(1);
		this.numberOfExecutions = numberOfExecutions;
		completedExecutions = new AtomicInteger(0);
		this.numberOfRounds = numberOfRounds;
		this.localNode = localNode;
		executions = new ConcurrentHashMap<Integer, Execution>();
		initExecution = null;
		roundOffset = numberOfRounds / numberOfExecutions;
		completedSession = new AtomicBoolean(false);

	}

	/**
	 * 
	 * @return the String representation of this Session's id
	 */
	public String getSessionId() {
		return sessionId.toString();
	}

	/**
	 * 
	 * @return the total number of rounds each Execution of this Session has
	 */
	public int getNumberOfRounds() {
		return numberOfRounds;
	}

	/**
	 * 
	 * @return the number of Executions this Session will contain in total once
	 *         it terminates
	 */
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
	 * @return The initial Execution of the session or null if no execution has
	 *         been created yet
	 */
	public Execution getInitExecution() {
		return initExecution;
	}

	/**
	 * This method creates a new Execution in the context of this Session, which
	 * has its execution number e incremented by one compared to the currently
	 * last stored Execution. If all the Executions that should be created for
	 * this Session have already been created, then a new Execution will not be
	 * created
	 * 
	 * @return an object of type Execution if a new Execution is created,
	 *         otherwise null
	 */
	public Execution createNewExecution() {
		return createNewExecution(numberOfNextExecution.get());
	}

	/**
	 * This method creates a new Execution in the context of this Session. If
	 * all the Executions that should be created for this Session have already
	 * been created, then a new Execution will not be created
	 * 
	 * @param executionNumber
	 *            the execution number e the new Execution should have
	 * @return an object of type Execution if a new Execution is created,
	 *         otherwise null
	 */
	public Execution createNewExecution(int executionNumber) {
		// if we already have as many Executions as we need, then do not create
		// a new one
		if (numberOfNextExecution.get() > numberOfExecutions)
			return null;
		Execution execution = new Execution(executionNumber, numberOfRounds,
				localNode);
		executions.put(numberOfNextExecution.get(), execution);
		// if this is the initial Execution of the Session keep it stored
		// separately. It will be required for checking whether a new Execution
		// should be created later
		if (numberOfNextExecution.get() == 1)
			initExecution = execution;
		// compute the number of execution a new Execution should have if
		// createNewExecution is called again
		numberOfNextExecution.incrementAndGet();
		return execution;
	}

	/**
	 * 
	 * @return the number of Executions which have terminated
	 */
	public int getCompletedExecutions() {
		return completedExecutions.get();
	}

	/**
	 * Increases the number of completed Executions by 1 and checks whether the
	 * Session has terminated
	 * 
	 * @return true if the Session has terminated, otherwise false
	 */
	public boolean addCompletedExecution() {
		int exec = completedExecutions.incrementAndGet();
		boolean complete = false;
		// if the number of completed Executions is equal to the total number of
		// Executions for this Session, then the Session should terminate
		if (exec == getNumberOfExecutions()) {
			complete = true;
			// compute the final estimations of this Session by getting the
			// median of all the Execution's proposals
			computedEigenvalues = this.medianOfEstimations();
			// mark this Session as terminated
			completedSession.set(complete);
		}
		return complete;
	}

	/**
	 * 
	 * @return the estimated eigenvalues of the whole Session
	 */
	public double[] getComputedEigenvalues() {
		if (this.hasTerminated()) {
			return computedEigenvalues;
		}
		return null;
	}

	/**
	 * 
	 * @return true if the Session has terminated, otherwise false
	 */
	public boolean hasTerminated() {
		return completedSession.get();
	}

	/**
	 * 
	 * @param executionNumber
	 *            the execution number e of the Execution we are interested in
	 * @return The execution that corresponds to executionNumber
	 */
	public Execution getExecution(int executionNumber) {
		return executions.get(executionNumber);
	}

	/**
	 * 
	 * @return true if the local node initiated the Session, otherwise false
	 */
	public boolean isInitiator() {
		return initiator;
	}

	/**
	 * Checks whether a new execution should be created
	 * 
	 * @return true if a new execution should be created, otherwise false
	 */
	public boolean newExecutionExpected() {
		int currRound = initExecution.getCurrentRound();
		// If all the expected executions have been created
		// do not create an additional one
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
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Session))
			return false;
		Session s = (Session) obj;

		return this.sessionId.equals(s.getSessionId());
	}

	@Override
	public int hashCode() {
		return sessionId.hashCode();
	}

	private double[] medianOfEstimations() {
		int sizeOfArray = initExecution.getMatrixAEigenvalues().length;
		Collection<Execution> execs = executions.values();

		// set the length of the result equal to the length of the proposal with
		// the least eigenvalues
		for (Execution e : execs) {
			if (e.getMedianEigenvalues().length < sizeOfArray)
				sizeOfArray = e.getMedianEigenvalues().length;
		}

		// create a square matrix containing all the proposals.
		// each row will contain the eigenvalues proposed by one node in
		// decreasing magnitude order
		double[][] completeResults = new double[numberOfExecutions][sizeOfArray];

		int i = 0;
		for (Execution e : execs) {
			double[] median = e.getMedianEigenvalues();
			for (int j = 0; j < sizeOfArray; j++) {
				completeResults[i][j] = median[j];
			}
			i++;
		}

		double[] finalValues = new double[sizeOfArray];

		// for each column of the matrix get the median, i.e. for column j find
		// the median of all proposed eigenvalues placed jth by a node when
		// ordered
		for (int j = 0; j < completeResults[0].length; j++) {
			double items[] = new double[completeResults.length];

			for (i = 0; i < completeResults.length; i++) {
				items[i] = Math.abs(completeResults[i][j]);
			}
			finalValues[j] = findMedian(items);
		}
		return finalValues;
	}

	private double findMedian(double[] items) {
		DescriptiveStatistics stats = new DescriptiveStatistics(items);
		return stats.getPercentile(50);
	}
}
