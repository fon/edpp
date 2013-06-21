package comm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.BeforeClass;
import org.junit.Test;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;
import core.ProtocolEngine;

public class MessageReceiverTest {

	static BlockingQueue<Message> queue;
	static Thread receiverThread;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		queue = new LinkedBlockingQueue<Message>();
		receiverThread = new Thread(new MessageReceiver(queue));
		receiverThread.setDaemon(true);
		receiverThread.start();
	}

	@Test
	public void checkThatMessageIsDelivered() throws UnknownHostException, IOException, InterruptedException {
		Message testMessage = Message.newBuilder()
				.setType(MessageType.NEW)
				.setExecution(100)
				.setRound(200)
				.build();
		
		Socket s = new Socket(InetAddress.getLocalHost(), ProtocolEngine.PROTOCOL_PORT);
		testMessage.writeTo(s.getOutputStream());
		s.close();
		Message receivedMessage = queue.take();
		assertEquals(testMessage, receivedMessage);
	}
	
}
