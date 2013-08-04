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
			
			{0.7833, -0.4703,  -0.2217,   0.0037,  -0.0097,  -0.0106,    0.0040,   -0.0033,   -0.0018,    0.0016,   -0.0011,    0.0011,   -0.0008,    0.0005,    0.0002,    0.0005,   -0.0003},
		    {0.4703,    0.8372,   -0.1683,   -0.0821,   -0.0193,   -0.0122,    0.0071,   -0.0041,   -0.0030,    0.0021,   -0.0018,    0.0014,   -0.0012,    0.0007,    0.0003,    0.0007,   -0.0004},
		    {0.2217,  -0.1683,    0.6095,  -0.4209,   -0.0578,   -0.0227,    0.0199,   -0.0083,   -0.0082,    0.0045,   -0.0048,    0.0029,   -0.0032,   0.0015,    0.0008,    0.0015,   -0.0011},
		    {0.0037,    0.0821,    0.4209,    0.5081,   -0.1901,   -0.1894,    0.0720,   -0.0579,   -0.0325,    0.0290,   -0.0198,    0.0186,   -0.0138,    0.0094,    0.0040,    0.0095,   -0.0046},
		   {-0.0097,    0.0193,    0.0578,   -0.1901,    0.8467,   -0.3095,    0.0239,   -0.0696,   -0.0157,    0.0325,   -0.0108,    0.0204,   -0.0079,    0.0101,    0.0030,    0.0104,   -0.0026},
		    {0.0106,   -0.0122,   -0.0227,    0.1894,    0.3095,    0.4952,    0.5272,   -0.0494,   -0.1508,    0.0324,   -0.0818,    0.0223,   -0.0532,    0.0123,    0.0113,    0.0109,   -0.0175},
		    {0.0040,   -0.0071,   -0.0199,    0.0720,    0.0239,   -0.5272,    0.3380,    0.5828,    0.0468,   -0.1809,    0.0333,   -0.1035,    0.0253,   -0.0488,   -0.0123,   -0.0505,    0.0081},
		    {0.0033,   -0.0041,   -0.0083,    0.0579,    0.0696,   -0.0494,   -0.5828,    0.1882,   -0.6222,    0.0348,   -0.2039,    0.0230,   -0.1147,    0.0134,    0.0213,    0.0095,   -0.0349},
		   {-0.0018,    0.0030,    0.0082,   -0.0325,   -0.0157,    0.1508,    0.0468,    0.6222,    0.0255,    0.6298,   -0.0270,    0.2039,   -0.0214,    0.0809,    0.0166,    0.0824,   -0.0060},
		   {-0.0016,    0.0021,    0.0045,   -0.0290,   -0.0325,    0.0324,    0.1809,    0.0348,   -0.6298,   -0.1343,    0.6324,   -0.0127,    0.1936,   -0.0092,   -0.0301,   -0.0028,    0.0479},
		   {-0.0011,    0.0018,    0.0048,   -0.0198,   -0.0108,    0.0818,    0.0333,    0.2039,   -0.0270,   -0.6324,   -0.2957,    0.5774,   -0.0170,    0.1287,    0.0228,    0.1238,   -0.0034},
		   {-0.0011,    0.0014,    0.0029,   -0.0186,   -0.0204,    0.0223,    0.1035,    0.0230,   -0.2039,   -0.0127,   -0.5774,   -0.4435,    0.5708,   -0.0096,   -0.0545,    0.0024,    0.0722},
		   {-0.0008,    0.0012,    0.0032,   -0.0138,   -0.0079,    0.0532,    0.0253,   0.1147,   -0.0214,   -0.1936,   -0.0170,  -0.5708,   -0.5934,    0.3473,    0.0514,    0.2601,    0.0000},
		   {-0.0005,    0.0007,    0.0015,   -0.0094,   -0.0101,    0.0123,    0.0488,    0.0134,   -0.0809,   -0.0092,   -0.1287,   -0.0096,   -0.3473,   -0.7086,   -0.5512,    0.0341,    0.1104},
		    {0.0002,   -0.0003,   -0.0008,    0.0040,    0.0030,   -0.0113,   -0.0123,   -0.0213,    0.0166,    0.0301,    0.0228,    0.0545,    0.0514,    0.5512,   -0.7962,   -0.2162,   -0.0203},
		   {-0.0005,    0.0007,    0.0015,   -0.0095,   -0.0104,    0.0109,    0.0505,    0.0095,   -0.0824,   -0.0028,   -0.1238,    0.0024,   -0.2601,    0.0341,    0.2162,   -0.8847,    0.1962},
		   {-0.0003,    0.0004,    0.0011,   -0.0046,   -0.0026,    0.0175,    0.0081,    0.0349,   -0.0060,   -0.0479,   -0.0034,   -0.0722,    0.0000,   -0.1104,   -0.0203,   -0.1962,   -0.9476}
	};
	
	double [] expectedEigenvals = {
			0.80167, 0.80167, 0.75358, 0.75358, 0.73876, 0.73876, 0.72609, 0.72609, 0.70228, 0.70228, 0.66033, 0.66033, 0.61693, 0.61693, 0.61253, 0.40914, 0.38664
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
		
		execution.addValToRound(10, 2);
		execution.addValToRound(20, 3);
		execution.addValToRound(30, 2);
		
		execution.setRound(2);
		assertEquals(40.0, execution.getCurrentValue(),0);
		execution.setRound(4);
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
		assertTrue(execution.remainingInitTime() <= 10*ProtocolController.TIMEOUT - 10);
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
		
		double [] newEigenvalues = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
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
