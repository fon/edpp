package core;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import network.FakeNode;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import storage.Database;
import storage.FakeDatabase;
import util.Phase;
import util.TimedNeighborsTable;

import comm.MessageBuilder;
import comm.ProtocolMessage.Message;
import comm.TransferableMessage;

public class MessageHandlerTaskTest {

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
	public void newSessionIsProperlyCreated() throws UnknownHostException {
		Message m = MessageBuilder.buildNewMessage(1, 10);
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		assertEquals(1, sessions.size());
		//Get the only session
		Session [] createdSessions = sessions.values().toArray(new Session [0]);
		Session s = createdSessions[0];
		assertTrue(s.isInitiator());
		assertEquals(1, s.getCurrentNumberOfExecutions());
		assertEquals(localNode.getOutNeighbors().size(), outQueue.size());
	}
	
	@Test
	public void newSessionIsCreatedFromUnknownInitMessage() throws UnknownHostException {
		Session s =  new Session(localNode, 1, 2);
		Message m = MessageBuilder.buildInitMessage("remoteNode", s.getSessionId(), 1, 1, 2, 1);
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		assertEquals(1, sessions.size());
		//Get the only session
		Session [] createdSessions = sessions.values().toArray(new Session [0]);
		Session testSession = createdSessions[0];
		assertFalse(testSession.isInitiator());
		assertEquals(1, testSession.getCurrentNumberOfExecutions());
		assertEquals(s.getSessionId(), testSession.getSessionId());
	}
	
	@Test
	public void newExecutionIsCreatedInKnownSession() throws UnknownHostException {
		Session s =  new Session(localNode, 2, 2);
		s.createNewExecution();
		sessions.put(s.getSessionId(), s);
		Message m = MessageBuilder.buildInitMessage("remoteNode", s.getSessionId(), 2, 2, 2, 1);
		
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		//No new session was created
		assertEquals(1, sessions.size());
		//Get the only session
		Session [] createdSessions = sessions.values().toArray(new Session [0]);
		Session testSession = createdSessions[0];
		
		//A new execution was actually created
		assertEquals(2, testSession.getCurrentNumberOfExecutions());
		assertEquals(s.getSessionId(), testSession.getSessionId());
		TimedNeighborsTable tnt = s.getExecution(2).getInNeighbors();
		assertEquals(1, tnt.getSize());
		//remoteNode is remoteNodQ== when converted to base64 encoding
		assertNotNull(tnt.getNeighbor("remoteNodQ=="));
	}
	
	@Test
	public void valueIsAddedToExecutionInINITPhase() throws UnknownHostException {
		Session s = new Session(localNode, 1, 2);
		Execution initExecution = s.createNewExecution();
		//We are interested in the value of the second round that will come with the INIT message
		double initVal = initExecution.getValsOfRound(2);
		System.out.println(initVal);
		sessions.put(s.getSessionId(), s);
		Message m = MessageBuilder.buildInitMessage("remoteNode", s.getSessionId(), 1, 1, 2, 1);
		
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		assertEquals(1, sessions.size());
		//Check the values of the second round
		assertEquals(initVal + 1, initExecution.getValsOfRound(2), 0.0);
	}
	
	@Test
	public void receivedDataExchangeMessageForUnknownSession() throws UnknownHostException {
		Message m = MessageBuilder.buildNextMessage("remoteNode", "unknownSession", 1, 2, 1);
		
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		//The session map should still be empty
		assertEquals(0, sessions.size());
	}
	
	@Test
	public void dataExchangePhaseIsProperlyCompleted() throws UnknownHostException {
		// Initially a new session with one execution and 2 rounds is created
		Message m = MessageBuilder.buildNewMessage(1, 3);
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		//Get the only session
		Session [] createdSessions = sessions.values().toArray(new Session [0]);
		Session testSession = createdSessions[0];
		Execution e = testSession.getInitExecution();
		
		//Add two in-neighbors
		Message n1 = MessageBuilder.buildInitMessage("cmVtb3RlTm9kZU9uZQ==", testSession.getSessionId(), 1, 1, 2, 1);
		Message n2 = MessageBuilder.buildInitMessage("cmVtb3RlTm9kZVR3bw==", testSession.getSessionId(), 1, 1, 2, 0);
		
		tm = new TransferableMessage(n1, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		tm = new TransferableMessage(n2, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		//Terminate INIT phase
		e.setPhase(Phase.DATA_EXCHANGE);
		e.recomputeWeight();
		e.setRound(2);
		e.recomputeWeight();
		e.setRound(3);
		e.recomputeWeight();
		
		n1 = MessageBuilder.buildNextMessage("cmVtb3RlTm9kZU9uZQ==", testSession.getSessionId(), 1, 3, 1);
		n2 = MessageBuilder.buildNextMessage("cmVtb3RlTm9kZVR3bw==", testSession.getSessionId(), 1, 3, 3);

		tm = new TransferableMessage(n1, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		tm = new TransferableMessage(n2, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		//The execution must enter the gossip phase
		assertEquals(2, e.getInNeighbors().getSize());
		assertEquals(1, e.getImpulseResponse(2), 0);
		assertEquals(Phase.GOSSIP, e.getPhase());
		assertEquals(4, e.getImpulseResponse(3), 0);
	}
	
	@Test
	public void gossipPhaseIsProperlyCompleted() throws UnknownHostException {
		Message m = MessageBuilder.buildNewMessage(1, 2);
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		//Get the only session
		Session [] createdSessions = sessions.values().toArray(new Session [0]);
		Session testSession = createdSessions[0];
		Execution e = testSession.getInitExecution();
		
		//Add two in-neighbors
		Message n1 = MessageBuilder.buildInitMessage("cmVtb3RlTm9kZU9uZQ==", testSession.getSessionId(), 1, 1, 2, 1);
		Message n2 = MessageBuilder.buildInitMessage("cmVtb3RlTm9kZVR3bw==", testSession.getSessionId(), 1, 1, 2, 0);
		
		tm = new TransferableMessage(n1, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		tm = new TransferableMessage(n2, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		e.setPhase(Phase.DATA_EXCHANGE);
		e.recomputeWeight();
		e.setRound(2);
		e.recomputeWeight();
		e.setPhase(Phase.GOSSIP);
		e.computeRealizationMatrix(localNode.getDiameter());
		
		double [] eig1 = {1.0};
		double [] eig2 = {0.0};
		
		n1 = MessageBuilder.buildGossipMessage("cmVtb3RlTm9kZU9uZQ==", testSession.getSessionId(), 1, eig1);
		n2 = MessageBuilder.buildGossipMessage("cmVtb3RlTm9kZVR3bw==", testSession.getSessionId(), 1, eig2);
	
		tm = new TransferableMessage(n1, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		tm = new TransferableMessage(n2, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		assertEquals(Phase.TERMINATED, e.getPhase());
	}
	
	@Test
	public void gossipMessageWasRetainedWhenNotInGossipPhase() throws UnknownHostException {
		Message m = MessageBuilder.buildNewMessage(1, 2);
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		//Get the only session
		Session [] createdSessions = sessions.values().toArray(new Session [0]);
		Session testSession = createdSessions[0];
		Execution e = testSession.getInitExecution();
		
		double [] eigenvalues = {1.0};
		Message n1 = MessageBuilder.buildGossipMessage("cmVtb3RlTm9kZU9uZQ==", testSession.getSessionId(), 1, eigenvalues);
		
		tm = new TransferableMessage(n1, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		
		//We are still in the INIT phase
		assertEquals(Phase.INIT, e.getPhase());
		assertEquals(1, e.getCurrentRound());
	}
	
	@Test
	public void malformedMessageIsReceived() throws UnknownHostException {
		Message m = MessageBuilder.buildLivenessMessage();
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		//Nothing must have changed, the message must be ignored
		assertEquals(0, sessions.size());
		assertEquals(0, outQueue.size());
	}

}
