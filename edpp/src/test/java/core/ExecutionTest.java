package core;

import static org.junit.Assert.*;

import java.util.ArrayList;


import network.FakeNode;
import network.Node;

import org.junit.Before;
import org.junit.Test;

import util.Id;

public class ExecutionTest {

	Node localNode;
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
	public void executionHasTerminated() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		for (int i = 2; i <= NUM_OF_ROUNDS; i++) {
			execution.setRound(i);
		}
		assertTrue(execution.hasTerminated());
	}
	
	@Test
	public void canAccessCorrectRound() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		execution.setCurrentImpulseResponse(10.0);
		assertEquals(10.0, execution.getImpulseResponse(2), 0);
		
		execution.setImpulseResponse(5, 5.0);
		assertEquals(5.0, execution.getImpulseResponse(5),0);
		
		execution.setCurrentValue(20.0);
		assertEquals(20.0, execution.getCurrentValue(),0);
		
		execution.addValToRound(13.0, 10);
		assertEquals(13.0, execution.getValsOfRound(10),0);
	}

}
