package core;

import java.util.Set;

import util.Neighbor;
import util.PlainNeighbor;
import util.PlainNeighborsTable;
import util.PlainNeighborsTableSet;
import util.TimedNeighborsTable;
import util.TimedNeighborsTableSet;

import network.Node;

public class Execution {
	
	public enum Phase { INIT, DATA_EXCHANGE, GOSSIP};

	private int executionNumber;
	private int numOfRounds;
	private int round;
	private Node localNode;
	private PlainNeighborsTable outNeighbors;
	private TimedNeighborsTable inNeighbors;
	private Phase phase;
	
	
	public Execution(int executionNumber, int numOfRounds, Node localNode) {
		this.setExecutionNumber(executionNumber);
		this.setNumOfRounds(numOfRounds);
		this.setRound(1);
		this.localNode = localNode;
		createOutTable();
		inNeighbors = new TimedNeighborsTableSet();
		setPhase(Phase.INIT);
	}

	public int getExecutionNumber() {
		return executionNumber;
	}

	public void setExecutionNumber(int executionNumber) {
		this.executionNumber = executionNumber;
	}

	public int getRound() {
		return round;
	}

	public void setRound(int round) {
		this.round = round;
	}
	
	public PlainNeighborsTable getOutNeighbors() {
		return outNeighbors;
	}
	
	public TimedNeighborsTable getInNeighbors() {
		return inNeighbors;
	}
	
	public int getNumOfRounds() {
		return numOfRounds;
	}

	public void setNumOfRounds(int numOfRounds) {
		this.numOfRounds = numOfRounds;
	}
	
	private void createOutTable() {
		PlainNeighbor pn;
		Set<Neighbor> on = localNode.getOutNeighbors();
		outNeighbors = new PlainNeighborsTableSet();
		int size = localNode.getNetworkSize();
		double weight = (double)size/on.size();
		for (Neighbor n : outNeighbors) {
			pn = new PlainNeighbor(n.getId(), n.getAddress(), weight);
			outNeighbors.addNeighbor(pn);
		}
	}

	public Phase getPhase() {
		return phase;
	}

	public void setPhase(Phase phase) {
		this.phase = phase;
	}
	
}
