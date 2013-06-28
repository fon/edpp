package core;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import Jama.Matrix;
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

public class Execution {
	
	private final int executionNumber;
	private final int numOfRounds;
	private AtomicInteger round;
	private final Node localNode;
	private PlainNeighborsTable outNeighbors;
	private TimedNeighborsTable inNeighbors;
	private Phase phase;
	private AtomicDoubleArray impulseResponse;
	private AtomicDouble nodeVal;
	private Map<Integer, Double> roundVals;	
	private AtomicLong initTimeout;
	private AtomicLong snapshot;
	private Matrix matrixA;
	private boolean hasComputedMatrix;
	private double [] eigenvalues;
	private GossipData gossip;
	
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
		initTimeout = new AtomicLong(2*ProtocolEngine.TIMEOUT);
		roundVals = new ConcurrentHashMap<Integer, Double>();
		this.hasComputedMatrix = false;
		gossip = new GossipData();
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

	//TODO must add test
	/**
	 * Sets the current round of the execution
	 * @param round The new round number
	 */
	public void setRound(int round) {
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
	
	//TODO add test
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
	
	//TODO add test
	/**
	 * Runs Kung's realization algorithm to compute the approximate matrix A and its eigenvalues
	 * @param networkDiameter the diameter of the network or some approximation
	 * @return The approximation of matrix A, if we had gathered all the required impulse responses, otherwise null
	 */
	public synchronized Matrix computeRealizationMatrix(int networkDiameter) {
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
	
	//TODO add test
	/**
	 * 
	 * @return The approximation matrix, if the computation has already been performed, otherwise null
	 */
	public Matrix getRealizationMatrix() {
		if (hasComputedMatrix) {
			return matrixA;
		} else {
			return null;
		}
	}
	
	//TODO add test
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
	
	//TODO add test
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
	
	//TODO add test
	public void addGossipEigenvalues(String nodeId, double [] array) {
		gossip.setNewProposal(nodeId, array);
	}
	
	//TODO add test
	public double [] getMedianEigenvalues() {
		if (hasComputedMatrix && this.getPhase() == Phase.GOSSIP) {
			return gossip.computeMedianOfProposedValues();
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
	
	//TODO add test
	/**
	 * 
	 * @return true if this is not the last round of execution
	 */
	public boolean hasAnotherRound() {
		return round.get() < this.numOfRounds;
	}
	
	public boolean addInNeighbor(TimedNeighbor tn) {
		if (inNeighbors.getNeighbor(tn.getId()) == null) {
			return inNeighbors.addNeighbor(tn);
		}
		
		return false;
	}
	
	//TODO must add test
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
