package core;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import storage.Database;
import storage.FakeDatabase;
import comm.TransferableMessage;
import domain.Execution;
import domain.Phase;
import domain.Session;
import domain.network.FakeNode;

public class MaintenanceTaskTest {

	static BlockingQueue<TransferableMessage> outQueue;
	FakeNode localNode;
	Map<String, Session> sessions;
	Database db;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		outQueue = new LinkedBlockingQueue<TransferableMessage>();
	}

	@Before
	public void setUp() throws Exception {
		db = new FakeDatabase();
		localNode = new FakeNode();
		sessions = new HashMap<String, Session>();
	}
	
	@After
	public void tearDown() throws Exception {
		outQueue.clear();
	}

	@Test
	public void initPhasePassesWhenNoRemainingTimeIsLeft() throws InterruptedException {
		Session s = new Session(localNode, 1, 2);
		Execution e = s.createNewExecution();
		sessions.put(s.getSessionId(), s);
		MaintenanceTask mt = new MaintenanceTask(sessions, outQueue, localNode, db);
		assertTrue(e.remainingInitTime() > 0);
		assertEquals(Phase.INIT, e.getPhase());
		mt.run();
		Thread.sleep(10000);
		while(e.remainingInitTime()>0) {
			System.out.println(e.remainingInitTime());
			mt = new MaintenanceTask(sessions, outQueue, localNode, db);
			mt.run();
		}
		// When the remaining INIT time is negative, the INIT phase is over
		assertTrue(e.remainingInitTime() <= 0);
		mt.run();
		assertNotEquals(Phase.INIT, e.getPhase());
	}
	
}
