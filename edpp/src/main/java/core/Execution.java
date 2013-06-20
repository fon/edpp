package core;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import util.Neighbor;
import util.Phase;
import util.PlainNeighbor;
import util.PlainNeighborsTable;
import util.PlainNeighborsTableSet;
import util.TimedNeighborsTable;
import util.TimedNeighborsTableSet;

import network.Node;

public class Execution {
	
	private int executionNumber;
	private int numOfRounds;
	private int round;
	private Node localNode;
	private PlainNeighborsTable outNeighbors;
	private TimedNeighborsTable inNeighbors;
	private Phase phase;
	private double [] impulseResponse;
	private double nodeVal;
	private Map<Integer, Double> roundVals;
	
	
	public Execution(int executionNumber, int numOfRounds, Node localNode) {
		this.executionNumber = executionNumber;
		this.numOfRounds = numOfRounds;
		this.localNode = localNode;
		createOutTable();
		inNeighbors = new TimedNeighborsTableSet();
		setPhase(Phase.INIT);
		impulseResponse = new double[numOfRounds];
		nodeVal = chooseInitialValue();
		impulseResponse[0] = nodeVal;
		this.setRound(2);
		roundVals = new HashMap<Integer, Double>();
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
		return round;
	}

	/**
	 * Sets the current round of the execution
	 * @param round The new round number
	 */
	public void setRound(int round) {
		this.round = round;
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
	public Phase getPhase() {
		return phase;
	}

	/**
	 * Change the current phase of the execution
	 * @param phase
	 */
	public void setPhase(Phase phase) {
		this.phase = phase;
	}
	
	/**
	 * 
	 * @param round
	 * @return The impulse response of a round
	 */
	public double getImpulseResponse(int round) {
		return impulseResponse[round-1];
	}
	
	/**
	 * 
	 * @return Get the current value that will be
	 * used to compute the value sent to all
	 * out-neighbors
	 */
	public double getCurrentValue() {
		return nodeVal;
	}
	
	/**
	 * 
	 * @param nodeVal Set the current value that will be
	 * used to compute the value sent to all
	 * out-neighbors
	 */
	public void setCurrentValue(double nodeVal) {
		this.nodeVal = nodeVal;
	}

	/**
	 * 
	 * @return An array of all the impulse responses
	 * in the current execution
	 */
	public double [] getImpulseResponses() {
		return impulseResponse;
	}
	
	/**
	 * 
	 * @param round
	 * @param response
	 * @return Set the impulse response of a particular round
	 */
	public boolean setImpulseResponse(int round, double response) {
		if (round <= numOfRounds) {
			impulseResponse[round-1] = response;
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
		if (this.round <= numOfRounds) {
			impulseResponse[round-1] = response;
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return True if the execution has terminated
	 */
	public boolean hasTerminated() {
		return this.round == this.numOfRounds;
	}
	
	private void createOutTable() {
		PlainNeighbor pn;
		Set<Neighbor> on = localNode.getOutNeighbors();
		outNeighbors = new PlainNeighborsTableSet();
		double weight = (double)1/on.size();
		for (Neighbor n : outNeighbors) {
			pn = new PlainNeighbor(n.getId(), n.getAddress(), weight);
			outNeighbors.addNeighbor(pn);
		}
	}
	
	private int chooseInitialValue() {
		int initVal;
		Random rand = new Random();
		initVal = rand.nextInt(2);
		return initVal;
	}

	
}
