package comm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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

public class MessageReceiverTest {

	static BlockingQueue<TransferableMessage> queue;
	static Thread receiverThread;
	static ExecutorService executor;
	static MessageReceiver mr;
	
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
		mr = new MessageReceiver(queue);
		executor.execute(mr);
	}

	@Test
	public void checkThatMessageIsDelivered() throws UnknownHostException, IOException, InterruptedException {
		Message testMessage = Message.newBuilder()
				.setType(MessageType.NEW)
				.setExecution(100)
				.setRound(200)
				.build();
		
		Socket s = new Socket(InetAddress.getLocalHost(), ProtocolController.PROTOCOL_PORT);
		testMessage.writeTo(s.getOutputStream());
		s.close();
		TransferableMessage receivedMessage = queue.take();
		assertEquals(testMessage, receivedMessage.getMessage());
	}
	
	@AfterClass
	public static void setUpAfterClass() throws Exception {
		mr.stopService();
		executor.shutdown();
	}
}
