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
			assertTrue(session.createNewExecution(i));
		}
		/*
		 * The addition of a sixth execution must return
		 * false, since we only defined 5
		 */
		assertFalse(session.createNewExecution(6));
		// Verify that we have 5 executions
		assertEquals(5, session.getCurrentNumberOfExecutions());
	}

}
