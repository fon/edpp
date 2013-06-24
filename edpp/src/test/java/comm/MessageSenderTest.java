package comm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.BeforeClass;
import org.junit.Test;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;
import core.ProtocolEngine;

public class MessageSenderTest {

	static BlockingQueue<TransferableMessage> queue;
	static Thread senderThread;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		queue = new LinkedBlockingQueue<TransferableMessage>();
		senderThread = new Thread(new MessageSender(queue));
		senderThread.setDaemon(true);
		senderThread.start();
	}

	@Test
	public void checkThatMessageWillBeDelivered() throws IOException, InterruptedException {
		Socket incomingSocket;
		ServerSocket ss = new ServerSocket(ProtocolEngine.PROTOCOL_PORT);
		
		Message testMessage = Message.newBuilder()
				.setType(MessageType.NEW)
				.setExecution(100)
				.setRound(200)
				.build();
		
		TransferableMessage m = new TransferableMessage(testMessage, InetAddress.getLocalHost());
		queue.put(m);
		
		incomingSocket = ss.accept();
		Message pm = Message.parseFrom(
				incomingSocket.getInputStream());
		incomingSocket.close();
		ss.close();
		
		assertEquals(testMessage, pm);
	}

}
