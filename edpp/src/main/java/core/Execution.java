package core;

import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jblas.DoubleMatrix;

import algorithms.Algorithms;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.AtomicDoubleArray;

import util.GossipData;
import util.Neighbor;
import util.Phase;
import util.PlainNeighbor;
import util.PlainNeighborsTable;
import util.PlainNeighborsTableSet;
import util.TimedNeighbor;
import util.TimedNeighborsTable;
import util.TimedNeighborsTableSet;

import network.Node;

public class Execution implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6505271319331510315L;
	
	private final int executionNumber;
	private final int numOfRounds;
	private transient AtomicInteger round;
	private transient final Node localNode;
	private transient PlainNeighborsTable outNeighbors;
	private transient TimedNeighborsTable inNeighbors;
	private transient Phase phase;
	private AtomicDoubleArray impulseResponse;
	private transient AtomicDouble nodeVal;
	private transient Map<Integer, Double> roundVals;	
	private transient AtomicLong initTimeout;
	private transient AtomicLong snapshot;
	private DoubleMatrix matrixA;
	private boolean hasComputedMatrix, computedMedian;
	private double [] eigenvalues;
	private double [] medianEigenvalues;
	private transient GossipData gossip;
	
	public Execution(final int executionNumber,final int numOfRounds, final Node localNode) {
		this.executionNumber = executionNumber;
		this.numOfRounds = numOfRounds;
		this.localNode = localNode;
		createOutTable();
		inNeighbors = new TimedNeighborsTableSet();
		setPhase(Phase.INIT);
		impulseResponse = new AtomicDoubleArray(numOfRounds);
		nodeVal = new AtomicDouble(chooseInitialValue());
		impulseResponse.set(0, nodeVal.get());
		round = new AtomicInteger(1);
		snapshot = new AtomicLong(System.nanoTime());
		initTimeout = new AtomicLong(2*ProtocolController.TIMEOUT);
		roundVals = new ConcurrentHashMap<Integer, Double>();
		this.hasComputedMatrix = false;
		gossip = new GossipData();
		computedMedian = false;
	}

	/**
	 * 
	 * @return The number of the current execution
	 */
	public int getExecutionNumber() {
		return executionNumber;
	}

	/**
	 * 
	 * @return Returns the current round of the execution
	 */
	public int getCurrentRound() {
		return round.get();
	}

	/**
	 * Sets the current round of the execution. This also
	 * changes the current computing value to the value of
	 * the proper round
	 * @param round The new round number
	 */
	public void setRound(int round) {
		setImpulseResponse(this.round.get(), nodeVal.get());
		this.round.set(round);
		Double d = roundVals.get(round);
		if (d == null) {
			this.setCurrentValue(0);
		} else {
			this.setCurrentValue(d);
		}
	}
	
	/**
	 * Adds a received value to a specific round
	 * @param val The value to be added
	 * @param round The round to which the value corresponds
	 */
	public void addValToRound(double val, int round) {
		if (roundVals.get(round) == null) {
			roundVals.put(round, val);
		} else {
			double newVal = roundVals.get(round);
			newVal += val;
			roundVals.put(round, newVal);
		}
	}
	
	/**
	 * 
	 * @param round
	 * @return Returns the sum of the currently received values for some round
	 */
	public double getValsOfRound(int round) {
		if (roundVals.get(round) == null) {
			return 0;
		} else {
			return roundVals.get(round);
		}
	}
	
	/**
	 * Computes the remaining time in the current execution's timer, by
	 * subtractiong the time since the last sampling
	 * @return The remaining time  of the execution's timer
	 */
	public synchronized long remainingInitTime() {
		long interval = System.nanoTime() - snapshot.get();
		//set the time for the next sampling
		snapshot = new AtomicLong(System.currentTimeMillis());
		long remTime = TimeUnit.MILLISECONDS.convert(interval, TimeUnit.NANOSECONDS);
		return initTimeout.addAndGet(-remTime);
	}
	
	/**
	 * Runs Kung's realization algorithm to compute the approximate matrix A and its eigenvalues
	 * @param networkDiameter the diameter of the network or some approximation
	 * @return The approximation of matrix A, if we had gathered all the required impulse responses, otherwise null
	 */
	public synchronized DoubleMatrix computeRealizationMatrix(int networkDiameter) {
		if (this.hasAnotherRound() || hasComputedMatrix) {
			return null;
		}
		double [] responses = new double[impulseResponse.length()];
		
		for (int i = 0; i< impulseResponse.length(); i++) {
			responses[i] = impulseResponse.get(i);
		}

		this.matrixA = Algorithms.computeSystemMatrixA(responses, networkDiameter);
		eigenvalues = Algorithms.computeEigenvalues(matrixA);
		gossip.setNewProposal(localNode.getLocalId().toString(), eigenvalues);
		hasComputedMatrix = true;
		return matrixA;
	}
	
	/**
	 * 
	 * @return The approximation matrix, if the computation has already been performed, otherwise null
	 */
	public DoubleMatrix getRealizationMatrix() {
		if (hasComputedMatrix) {
			return matrixA;
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @return A double array with the eigenvalues of the approximation matrix
	 * or null if the approximation matrix has not been computed yet
	 */
	public double [] getMatrixAEigenvalues() {
		if (hasComputedMatrix) {
			return eigenvalues;
		} else {
			return null;
		}
	}
	
	/**
	 * Changes the values of the eigenvalue array. This method should be used if the
	 * approximation matrix has already been computed and we are in the GOSSIP round,
	 * otherwise no change will be made to the eigenvalues array
	 * @param newValues An array with the new values the eigenvalues array should get
	 * @return {@value true} if the values were updated successfully, {@value false} otherwise
	 */
	public boolean setMatrixAEigenvalues(double [] newValues) {
		if (hasComputedMatrix && this.getPhase() == Phase.GOSSIP) {
			eigenvalues = newValues;
			gossip.setNewProposal(localNode.getLocalId().toString(), newValues);
			return true;
		}
		return false;
	}
	
	/**
	 * Addes an array of eigenvalues for a node. If the node has already
	 * proposed some values, they will be overwritten
	 * @param nodeId The Id of the node that proposed eigenvalues
	 * @param array The proposed eigenvalues
	 */
	public void addGossipEigenvalues(String nodeId, double [] array) {
		gossip.setNewProposal(nodeId, array);
	}
	
	/**
	 * Computes the median of the eigenvalues based on the local node and the
	 * proposals if its in-neighbors
	 * @return An array containing the median of the eigenvalues, or null
	 * if the execution has not reached the gossip phase and has not computed the required matrix
	 */
	public double [] computeMedianEigenvalues() {
		if (hasComputedMatrix && this.getPhase() == Phase.GOSSIP) {
			medianEigenvalues = gossip.computeMedianOfProposedValues();
			computedMedian = true;
			return medianEigenvalues;
		}
		return null;
	}
	
	/**
	 * 
	 * @return An array with the final eigenvalues if they have been computed, null otherwise
	 */
	public double [] getMedianEigenvalues() {
		if (computedMedian) {
			return medianEigenvalues;
		}
		return null;
	}
	
	/**
	 * 
	 * @return A table with all the out-neighbors along with
	 * their weights
	 */
	public PlainNeighborsTable getOutNeighbors() {
		return outNeighbors;
	}
	
	/**
	 * 
	 * @return A table with all the in-neighbors along with 
	 * their remaining alive time
	 */
	public TimedNeighborsTable getInNeighbors() {
		return inNeighbors;
	}
	
	/**
	 * 
	 * @return The total number of rounds of this execution
	 */
	public int getNumOfRounds() {
		return numOfRounds;
	}
	
	/**
	 * Gives the phase that the execution is in
	 * @return  An enum with values 
	 *  INIT, DATA_EXCHANGE, GOSSIP depending on the current phase
	 */
	public synchronized Phase getPhase() {
		return phase;
	}

	/**
	 * Change the current phase of the execution
	 * @param phase
	 */
	public synchronized void setPhase(Phase phase) {
		this.phase = phase;
	}
	
	/**
	 * 
	 * @param round
	 * @return The impulse response of a round
	 */
	public double getImpulseResponse(int round) {
		return impulseResponse.get(round-1);
	}
	
	/**
	 * 
	 * @return Get the current value that will be
	 * used to compute the value sent to all
	 * out-neighbors
	 */
	public double getCurrentValue() {
		return nodeVal.get();
	}
	
	/**
	 * 
	 * @param nodeVal Set the current value that will be
	 * used to compute the value sent to all
	 * out-neighbors
	 */
	public void setCurrentValue(double nodeVal) {
		this.nodeVal.set(nodeVal);
	}

	/**
	 * 
	 * @return An array of all the impulse responses
	 * in the current execution
	 */
	public double [] getImpulseResponses() {
		double [] responses = new double[impulseResponse.length()];
		for (int i = 0; i<impulseResponse.length(); i++) {
			responses[i] = impulseResponse.get(i);
		}
		return responses;
	}
	
	/**
	 * 
	 * @param round
	 * @param response
	 * @return Set the impulse response of a particular round
	 */
	public boolean setImpulseResponse(int round, double response) {
		if (round <= numOfRounds) {
			impulseResponse.set(round-1, response);
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param response
	 * @return Set the impulse response of the current round
	 */
	public boolean setCurrentImpulseResponse(double response) {
		if (round.get() <= numOfRounds) {
			impulseResponse.set(round.addAndGet(-1), response);
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return True if the execution has terminated
	 */
	public boolean hasTerminated() {
		return round.get() == this.numOfRounds+1;
	}
	
	/**
	 * 
	 * @return true if this is not the last round of execution
	 */
	public boolean hasAnotherRound() {
		return round.get() < this.numOfRounds;
	}
	
	/**
	 * Adds a new in-neighbor to the in-neighbors list
	 * @param tn The TimedNeighbor to be added to the list
	 * @return true if the neighbor was successfully added, false if the neighbor already existed
	 */
	public boolean addInNeighbor(TimedNeighbor tn) {
		if (inNeighbors.getNeighbor(tn.getId()) == null) {
			return inNeighbors.addNeighbor(tn);
		}
		
		return false;
	}
	
	/**
	 * Recomputes the weights of the out-neighbors
	 * depending on the current out-neighbors list
	 */
	public void recomputeWeight() {
		Set<Neighbor> on = localNode.getOutNeighbors();
		double weight = 1.0/on.size();
		synchronized (outNeighbors) {
			for (PlainNeighbor n : outNeighbors) {
				n.setWeight(weight);
			}
		}
	}
	
	public boolean setTimerToInf(String nodeId) {
		return inNeighbors.setTimerToInf(nodeId);
	}
	
	public void resetTimers() {
		inNeighbors.renewTimers();
	}

	public boolean resetTimer(String nodeId) {
		return inNeighbors.renewTimer(nodeId);
	}

	public boolean roundIsOver() {
		synchronized (inNeighbors) {
			for (TimedNeighbor tn : inNeighbors) {
				if (tn.getTimeToProbe() != TimedNeighbor.INF) {
					return false;
				}
			}
		}
		return true;
	}
	
	private void createOutTable() {
		PlainNeighbor pn;
		Set<Neighbor> on = localNode.getOutNeighbors();
		outNeighbors = new PlainNeighborsTableSet();
		double weight = 1.0/on.size();
		for (Neighbor n : on) {
			pn = new PlainNeighbor(n.getId(), n.getAddress(), weight);
			outNeighbors.addNeighbor(pn);
		}
	}
	
	private static int chooseInitialValue() {
		int initVal;
		Random rand = new Random();
		initVal = rand.nextInt(2);
		return initVal;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null) return false;
		if (!(obj instanceof Execution)) return false;
		Execution e = (Execution)obj;
		
		return this.executionNumber == e.getExecutionNumber();
	}

}
