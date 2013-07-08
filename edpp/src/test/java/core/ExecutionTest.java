package core;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;


import network.FakeNode;

import org.jblas.DoubleMatrix;
import org.junit.Before;
import org.junit.Test;

import util.Id;
import util.Phase;
import util.TimedNeighbor;

public class ExecutionTest {

	FakeNode localNode;
	Execution execution; 
	static final int NUM_OF_ROUNDS = 100;
	
	double [] impulseResponses  = {
			 0.0122, 0.0068, 0.0686, 0.1061, 0.0763,
			 -0.0071, -0.0985, -0.1537, -0.1534, -0.1074,
			 -0.0426, 0.0131, 0.0439, 0.0488, 0.0373, 0.0217,
			 0.0103, 0.0055, 0.0050, 0.0051, 0.0033, -0.0007
	};
	
	double [][] expectedMatrixA = { 
			{0.8216, -0.4873, -0.1192, 0.0065},
			{0.4873, 0.7846, -0.1619, -0.1915},
			{0.1192, -0.1619, 0.7605, -0.4553},
			{0.0065, 0.1915, 0.4553, 0.3925}
	};
	
	double [] expectedEigenvals = {
			 0.595,  0.595, 0.785, 0.785
	};
	
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
	public void setNewRoundProperly() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		
		execution.addValToRound(10, 1);
		execution.addValToRound(20, 3);
		execution.addValToRound(30, 1);
		
		execution.setRound(1);
		assertEquals(40.0, execution.getCurrentValue(),0);
		execution.setRound(2);
		assertEquals(0.0, execution.getCurrentValue(),0);
		execution.setRound(3);
		assertEquals(20.0, execution.getCurrentValue(), 0);
	}
	
	@Test
	public void checkRemainingTime() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue(execution.remainingInitTime() <= 2*ProtocolController.TIMEOUT - 10);
	}
	
	@Test
	public void computeRealizationMatrixProperly() {
		execution = new Execution(1, impulseResponses.length, localNode);

		//The matrix should not be computed yet
		assertNull(execution.computeRealizationMatrix(4));
		
		for (int i = 2; i <= impulseResponses.length; i++) {
			execution.addValToRound(impulseResponses[i-1], i);
		}
		
		for (int i = 2; i <= impulseResponses.length +1; i++) {
			execution.setRound(i);
		}
		//Must do this, otherwise the first value will always be 0 or 1
		//We need it to be the test impulse response
		execution.setImpulseResponse(1, impulseResponses[0]);
		assertFalse(execution.hasAnotherRound());
		
		DoubleMatrix m = execution.computeRealizationMatrix(4);
		
		for (int i=0; i < m.getRows(); i++) {
			for (int j=0; j < m.getColumns(); j++) {
				assertEquals(expectedMatrixA[i][j], m.get(i, j), 0.05);
			}
		}
	}
	
	@Test
	public void returnRealizationMatrixOnlyOnceComputed() {
		execution = new Execution(1, impulseResponses.length, localNode);

		//The matrix should not be computed yet
		assertNull(execution.getRealizationMatrix());
		
		for (int i = 2; i <= impulseResponses.length; i++) {
			execution.addValToRound(impulseResponses[i-1], i);
		}
		
		for (int i = 2; i <= impulseResponses.length +1; i++) {
			execution.setRound(i);
		}
		//Must do this, otherwise the first value will always be 0 or 1
		//We need it to be the test impulse response
		execution.setImpulseResponse(1, impulseResponses[0]);
		assertFalse(execution.hasAnotherRound());
		
		execution.computeRealizationMatrix(4);
		
		DoubleMatrix m = execution.getRealizationMatrix();
		
		for (int i=0; i < m.getRows(); i++) {
			for (int j=0; j < m.getColumns(); j++) {
				assertEquals(expectedMatrixA[i][j], m.get(i, j), 0.05);
			}
		}
	}
	
	@Test
	public void returnEigenvaluesOnceMatrixIsComputed() {
		execution = new Execution(1, impulseResponses.length, localNode);

		//The matrix should not be computed yet
		assertNull(execution.getMatrixAEigenvalues());
		
		for (int i = 2; i <= impulseResponses.length; i++) {
			execution.addValToRound(impulseResponses[i-1], i);
		}
		
		for (int i = 2; i <= impulseResponses.length +1; i++) {
			execution.setRound(i);
		}
		//Must do this, otherwise the first value will always be 0 or 1
		//We need it to be the test impulse response
		execution.setImpulseResponse(1, impulseResponses[0]);
		
		execution.computeRealizationMatrix(4);
		
		double [] eigenvalues = execution.getMatrixAEigenvalues();
		assertArrayEquals(expectedEigenvals, eigenvalues, 0.05);
		
	}
	
	@Test
	public void hasAdditionalRounds() {
		execution = new Execution(1, NUM_OF_ROUNDS, localNode);
		assertTrue(execution.hasAnotherRound());
		for (int i = 2; i <= NUM_OF_ROUNDS; i++) {
			execution.setRound(i);
		}
		assertFalse(execution.hasAnotherRound());
	}
	
	@Test
	public void medianEigenvaluesAreComputedCorrectly() {
		
		double [] newEigenvalues = {1.0, 1.0, 1.0, 1.0};
		byte [] i1 = {1,2,3,3,2,1};
		byte [] i2 = {3,2,1,1,2,3};
		
		Id id1 = new Id(i1);
		Id id2 = new Id(i2);
		
		execution = new Execution(1, impulseResponses.length, localNode);

		//The matrix should not be computed yet
		assertNull(execution.getMatrixAEigenvalues());
		assertFalse(execution.setMatrixAEigenvalues(newEigenvalues));
		for (int i = 2; i <= impulseResponses.length; i++) {
			execution.addValToRound(impulseResponses[i-1], i);
		}
		
		for (int i = 2; i <= impulseResponses.length +1; i++) {
			execution.setRound(i);
		}
		//Must do this, otherwise the first value will always be 0 or 1
		//We need it to be the test impulse response
		execution.setImpulseResponse(1, impulseResponses[0]);
		
		//Computation should be null because we are not in the gossip phase
		assertNull(execution.computeMedianEigenvalues());
		
		execution.computeRealizationMatrix(4);
		execution.setPhase(Phase.GOSSIP);
		execution.addGossipEigenvalues(id1.toString(), newEigenvalues);
		execution.addGossipEigenvalues(id2.toString(), newEigenvalues);
		
		//The eigenvalues should be the new ones, since we have 2 times these
		//and only one time the expected
		double [] median = execution.computeMedianEigenvalues();
		assertArrayEquals(newEigenvalues, median, 0.05);
		
		//Now the second remote nodes has the expected eigenvalues,
		//thus the median should be the expected one
		execution.addGossipEigenvalues(id2.toString(), expectedEigenvals);
		median = execution.computeMedianEigenvalues();
		assertArrayEquals(expectedEigenvals, median, 0.05);
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
