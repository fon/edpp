package core;

import static org.junit.Assert.*;

import network.FakeNode;
import network.Node;

import org.junit.Before;
import org.junit.Test;

public class SessionTest {

	Node localNode;
	Session session;
	
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

}
