package core;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
import comm.MessageBuilder;
import comm.ProtocolMessage.Message;
import comm.TransferableMessage;
import domain.Execution;
import domain.Phase;
import domain.Session;
import domain.network.FakeNode;
import domain.structure.TimedNeighborsTable;

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
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue);
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
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue);
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
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue);
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
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue);
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
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue);
		mht.run();
		
		//The session map should still be empty
		assertEquals(0, sessions.size());
	}
	
	
	@Test
	public void gossipMessageWasRetainedWhenNotInGossipPhase() throws UnknownHostException {
		Message m = MessageBuilder.buildNewMessage(1, 2);
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue);
		mht.run();
		
		//Get the only session
		Session [] createdSessions = sessions.values().toArray(new Session [0]);
		Session testSession = createdSessions[0];
		Execution e = testSession.getInitExecution();
		
		double [] eigenvalues = {1.0};
		Message n1 = MessageBuilder.buildGossipMessage("cmVtb3RlTm9kZU9uZQ==", testSession.getSessionId(), 1, eigenvalues);
		
		tm = new TransferableMessage(n1, address);
		mht = new MessageHandlerTask(tm, sessions, localNode, outQueue);
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
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue);
		mht.run();
		//Nothing must have changed, the message must be ignored
		assertEquals(0, sessions.size());
		assertEquals(0, outQueue.size());
	}

}
