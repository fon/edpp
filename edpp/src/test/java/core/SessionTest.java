package core;

import static org.junit.Assert.*;
import network.FakeNode;
import network.Node;

import org.jblas.DoubleMatrix;
import org.junit.Before;
import org.junit.Test;

import util.Phase;

public class SessionTest {

	Node localNode;
	Session session;
	
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
	public void addAllExecutions() {
		session = new Session(localNode, 5, 100);
		for (int i = 1; i <= 5; i++){
			/*
			 * We initially defined 5 executions, so
			 * all must return true
			 */
			assertNotNull(session.createNewExecution());
		}
		/*
		 * The addition of a sixth execution must return
		 * false, since we only defined 5
		 */
		assertNull(session.createNewExecution());
		// Verify that we have 5 executions
		assertEquals(5, session.getCurrentNumberOfExecutions());
	}
	
	@Test
	public void checkIfNewExecutionIsExpected() {
		session = new Session(localNode, 5, 100);
		session.createNewExecution();
		session.getInitExecution().setRound(19);
		assertFalse(session.newExecutionExpected());
		session.getInitExecution().setRound(20);
		assertTrue(session.newExecutionExpected());
		session.getInitExecution().setRound(99);
		assertFalse(session.newExecutionExpected());
	}
	
	@Test
	public void createNewUnnumberedExecution() {
		session = new Session(localNode, 4, 100);
		assertEquals(1,
				session.createNewExecution().getExecutionNumber());
		assertEquals(2,
				session.createNewExecution().getExecutionNumber());
	}
	
	@Test
	public void createNewNumberedExecution() {
		session = new Session(localNode, 4, 100);
		assertEquals(2,
				session.createNewExecution(2).getExecutionNumber());
		assertEquals(3,
				session.createNewExecution(3).getExecutionNumber());
		assertEquals(2, session.getCurrentNumberOfExecutions());
	}
	
	@Test
	public void createMoreThanDefinedExecutions() {
		session = new Session(localNode, 3, 100);
		assertNotNull(session.createNewExecution());
		session.createNewExecution();
		session.createNewExecution();
		assertNull(session.createNewExecution());
	}

	@Test
	public void sessionTerminatesSuccessfully() {
		session = new Session(localNode, 3, impulseResponses.length);
		Execution e1 = session.createNewExecution();
		Execution e2 = session.createNewExecution();
		Execution e3 = session.createNewExecution();
		
		//No execution should be completed
		assertEquals(0, session.getCompletedExecutions());
		assertFalse(session.hasTerminated());
		
		//We make two of the executions the same. The result must be the expected eigenvalues
		for (int i = 2; i <= impulseResponses.length; i++) {
			e1.addValToRound(impulseResponses[i-1], i);
			e2.addValToRound(impulseResponses[i-1], i);
			e3.addValToRound(0.05, i);
		}
		
		for (int i = 2; i <= impulseResponses.length +1; i++) {
			e1.setRound(i);
			e2.setRound(i);
			e3.setRound(i);
		}
		//Must do this, otherwise the first value will always be 0 or 1
		//We need it to be the test impulse response
		e1.setImpulseResponse(1, impulseResponses[0]);
		e2.setImpulseResponse(1, impulseResponses[0]);
		e3.setImpulseResponse(1, impulseResponses[0]);
		
		e1.computeRealizationMatrix();
		e1.setPhase(Phase.GOSSIP);
		e1.computeMedianEigenvalues();
		e1.setPhase(Phase.TERMINATED);
		session.addCompletedExecution();
		e2.computeRealizationMatrix();
		e2.setPhase(Phase.GOSSIP);
		e2.computeMedianEigenvalues();
		e2.setPhase(Phase.TERMINATED);
		session.addCompletedExecution();
		
		e3.computeRealizationMatrix();
		e3.setPhase(Phase.GOSSIP);
		DoubleMatrix dd = new DoubleMatrix(e3.computeMedianEigenvalues());
		dd.print();
		e3.setPhase(Phase.TERMINATED);
		session.addCompletedExecution(); 
		
		assertEquals(3, session.getCompletedExecutions());
		assertArrayEquals(expectedEigenvals, session.getComputedEigenvalues(), 0.05);
		assertTrue(session.hasTerminated());
	}
}
