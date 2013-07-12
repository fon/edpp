package comm;

import static org.junit.Assert.*;

import java.util.List;


import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;

public class MessageBuilderTest {

	@Test
	public void newMessageIsBuiltProperly() {
		Message m = MessageBuilder.buildNewMessage(3, 4);
		
		//These values should be set on the message
		assertEquals(MessageType.NEW, m.getType());
		assertEquals(3, m.getTotalNumberOfExecutions());
		assertEquals(4, m.getRound());
		
		//The rest of the message contents should
		//be initialized to 0
		assertEquals(0.0, m.getVal(), 0.0);
		assertEquals("", m.getNodeId());
		assertEquals(0, m.getEigenvalsCount());
		assertEquals("",m.getSession());
	}
	
	@Test
	public void initMessageIsBuiltProperly() {
		Message m = MessageBuilder.buildInitMessage("testNode", "testSession", 3, 1, 4, 10.0);
		
		//These values should be set on the message
		assertEquals(MessageType.INIT, m.getType());
		assertEquals(3, m.getExecution());
		assertEquals(4, m.getRound());
		assertEquals(10.0, m.getVal(), 0.0);
		assertEquals("testSession",m.getSession());
		assertEquals("testNode", m.getNodeId());
		assertEquals(1, m.getTotalNumberOfExecutions());
		
		//The rest of the message contents should
		//be initialized to 0
		assertEquals(0, m.getEigenvalsCount());
	}
	
	@Test
	public void nextMessageIsBuiltProperly() {
		Message m = MessageBuilder.buildNextMessage("testNode", "testSession", 3, 4, 10.0);
		
		//These values should be set on the message
		assertEquals(MessageType.NEXT, m.getType());
		assertEquals(3, m.getExecution());
		assertEquals(4, m.getRound());
		assertEquals(10.0, m.getVal(), 0.0);
		assertEquals("testSession",m.getSession());
		assertEquals("testNode", m.getNodeId());
		
		//The rest of the message contents should
		//be initialized to 0
		assertEquals(0, m.getEigenvalsCount());
	}
	
	@Test
	public void gossipMessageIsBuiltProperly() {
		double [] eigenvals = {10.0, 11};
		Message m = MessageBuilder.buildGossipMessage("testNode", "testSession", 3, eigenvals);
		
		//These values should be set on the message
		assertEquals(MessageType.GOSSIP, m.getType());
		assertEquals(3, m.getExecution());
		assertEquals("testSession",m.getSession());
		assertEquals("testNode", m.getNodeId());
		assertEquals(2, m.getEigenvalsCount());
		
		List<Double> eig = m.getEigenvalsList();
		double [] e = ArrayUtils.toPrimitive(eig.toArray(new Double[eig.size()]));
		assertArrayEquals(eigenvals, e, 0.0);
		
		//The rest of the message contents should
		//be initialized to 0
		assertEquals(0, m.getRound());
		assertEquals(0.0, m.getVal(), 0.0);
	}

}
