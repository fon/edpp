package core;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;


import network.FakeNode;

import org.junit.Before;
import org.junit.Test;

import util.Id;
import util.TimedNeighbor;

public class ExecutionTest {

	FakeNode localNode;
	Execution execution; 
	static final int NUM_OF_ROUNDS = 100;
	
	@Before
	public void setUp() throws Exception {
		localNode = new FakeNode();
	}

	@Test
	public void initValueIsOnlyOneOrZero() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		ArrayList<Double> expected = new ArrayList<Double>();
		expected.add(0.0);
		expected.add(1.0);
		assertTrue(expected.contains(execution.getImpulseResponse(1)));
	}
	
	@Test 
	public void outTableHasProperWeights() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		Id nodeId = new Id(FakeNode.getNode1());
		double weight = execution.getOutNeighbors().getWeight(nodeId);
		assertEquals(1.0/3, weight, 0);
	}
	
	@Test
	public void checkIfWeightsAreRecomputed() throws UnknownHostException {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		Id nodeId = new Id(FakeNode.getNode1());
		double weight = execution.getOutNeighbors().getWeight(nodeId);
		assertEquals(1.0/3, weight, 0);
		localNode.renewOutNeighbors();
		execution.recomputeWeight();
		weight = execution.getOutNeighbors().getWeight(nodeId);
		assertEquals(1.0/4, weight, 0);
	}
	
	@Test
	public void executionHasTerminated() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		for (int i = 2; i <= NUM_OF_ROUNDS+1; i++) {
			execution.setRound(i);
		}
		assertTrue(execution.hasTerminated());
	}
	
	@Test
	public void canAccessCorrectRound() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		execution.setCurrentImpulseResponse(10.0);
		assertEquals(10.0, execution.getImpulseResponse(1), 0);
		
		execution.setImpulseResponse(5, 5.0);
		assertEquals(5.0, execution.getImpulseResponse(5),0);
		
		execution.setCurrentValue(20.0);
		assertEquals(20.0, execution.getCurrentValue(),0);
		
		execution.addValToRound(13.0, 10);
		assertEquals(13.0, execution.getValsOfRound(10),0);
	}
	
	@Test
	public void addANewInNeighbor() throws UnknownHostException {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		byte [] id = {1,2,3,4,6,7,8,9,0};
		Id i = new Id(id);
		InetAddress address = InetAddress.getLocalHost();
		TimedNeighbor tn = new TimedNeighbor(i, address);
		assertTrue(execution.addInNeighbor(tn));
		assertEquals(tn,execution.getInNeighbors().getNeighbor(i));
	}
	
	@Test
	public void addAnAlreadyExistingNeighbor() throws UnknownHostException {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		byte [] id = {1,2,3,4,6,7,8,9,0};
		Id i = new Id(id);
		InetAddress address = InetAddress.getLocalHost();
		TimedNeighbor tn = new TimedNeighbor(i, address);
		execution.addInNeighbor(tn);
		assertFalse(execution.addInNeighbor(tn));
		assertEquals(1, execution.getInNeighbors().getSize());
	}

	@Test
	public void checkIfRoundIsOver() throws UnknownHostException {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		byte [] id1 = {1,2,3,4,6,7,8,9,0};
		byte [] id2 = {0,9,8,7,6,5,4,3,2,1};
		byte [] id3 = {1,1,2,2,3,3,4,4,5,5};
		
		InetAddress address = InetAddress.getLocalHost();
		TimedNeighbor tn1 = new TimedNeighbor(id1, address);
		TimedNeighbor tn2 = new TimedNeighbor(id2, address);
		TimedNeighbor tn3 = new TimedNeighbor(id3, address);
		
		execution.addInNeighbor(tn1);
		execution.addInNeighbor(tn2);
		execution.addInNeighbor(tn3);
		
		assertFalse(execution.roundIsOver());
		execution.setTimerToInf(tn1.getId().toString());
		assertFalse(execution.roundIsOver());
		execution.setTimerToInf(tn2.getId().toString());
		execution.setTimerToInf(tn3.getId().toString());
		assertTrue(execution.roundIsOver());
	}
	
}
