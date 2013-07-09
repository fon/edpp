package comm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;
import core.ProtocolController;

public class MessageSenderTest {

	static BlockingQueue<TransferableMessage> queue;
	static Thread senderThread;
	static ExecutorService executor;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		queue = new LinkedBlockingQueue<TransferableMessage>();
		executor = Executors.newFixedThreadPool(4,new ThreadFactory() {
		    public Thread newThread(Runnable r) {
		        Thread t=new Thread(r);
		        t.setDaemon(true);
		        return t;
		    }
		});
		executor.execute(new MessageSender(queue));
	}

	@Test
	public void checkThatMessageWillBeDelivered() throws IOException, InterruptedException {
		Socket incomingSocket;
		ServerSocket ss = new ServerSocket(ProtocolController.PROTOCOL_PORT);
		
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
	
	@AfterClass
	public static void setUpAfterClass() throws Exception {
		executor.shutdown();
	}

}
