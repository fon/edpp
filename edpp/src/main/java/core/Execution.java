package core;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.AtomicDoubleArray;

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
		round = new AtomicInteger(2);
		
		//TODO concurrent hashmap and atomic double
		roundVals = new ConcurrentHashMap<Integer, Double>();
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
	 * Sets the current round of the execution
	 * @param round The new round number
	 */
	public void setRound(int round) {
		this.round.set(round);
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
		return round.get() == this.numOfRounds;
	}
	
	//TODO must add test
	public boolean addInNeighbor(TimedNeighbor tn) {
		if (inNeighbors.getNeighbor(tn.getId()) == null) {
			return inNeighbors.addNeighbor(tn);
		}
		
		return false;
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

	
}
