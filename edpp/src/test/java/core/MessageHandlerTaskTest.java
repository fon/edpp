package core;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import network.FakeNode;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import storage.Database;
import storage.FakeDatabase;

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

	@Test
	public void newSessionIsProperlyCreated() throws UnknownHostException {
		Message m = MessageBuilder.buildNewMessage(1, 10);
		InetAddress address = InetAddress.getByName("192.168.0.1");
		TransferableMessage tm = new TransferableMessage(m, address);
		
		MessageHandlerTask mht = new MessageHandlerTask(tm, sessions, localNode, outQueue, db);
		mht.run();
		assertEquals(1, sessions.size());
		Session [] createdSessions = sessions.values().toArray(new Session [0]);
		Session s = createdSessions[0];
		assertTrue(s.isInitiator());
		assertEquals(1, s.getCurrentNumberOfExecutions());
		assertEquals(localNode.getOutNeighbors().size(), outQueue.size());
	}
	
	//TODO must add tests for INIT, NEXT and GOSSIP messages
	
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
