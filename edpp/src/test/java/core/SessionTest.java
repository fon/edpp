package core;

import static org.junit.Assert.*;

import network.FakeNode;
import network.Node;

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
			{0.8216, -0.4873, 0.1192, -0.0065},
			{0.4873, 0.7846, 0.1619, 0.1915},
			{-0.1192, 0.1619, 0.7605, -0.4553},
			{-0.0065, -0.1915, 0.4553, 0.3925}
	};
	
	double [] expectedEigenvals = {
			0.785, 0.785, 0.595,  0.595
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
		
		
		e1.computeRealizationMatrix(4);
		e1.setPhase(Phase.GOSSIP);
		e1.computeMedianEigenvalues();
		e1.setPhase(Phase.TERMINATED);
		session.addCompletedExecution();
		
		e2.computeRealizationMatrix(4);
		e2.setPhase(Phase.GOSSIP);
		e2.computeMedianEigenvalues();
		e2.setPhase(Phase.TERMINATED);
		session.addCompletedExecution();
		
		e3.computeRealizationMatrix(4);
		e3.setPhase(Phase.GOSSIP);
		e3.computeMedianEigenvalues();
		e3.setPhase(Phase.TERMINATED);
		session.addCompletedExecution(); 
		
		assertEquals(3, session.getCompletedExecutions());
		assertArrayEquals(expectedEigenvals, session.getComputedEigenvalues(), 0.05);
		assertTrue(session.hasTerminated());
	}
}
