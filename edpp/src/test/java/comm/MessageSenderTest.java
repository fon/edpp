package comm;

import static org.junit.Assert.*;

import java.io.IOException;
//import java.io.PrintWriter;
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
		
		TransferableMessage m = new TransferableMessage(testMessage, InetAddress.getLocalHost(),true);
		queue.put(m);
		
		incomingSocket = ss.accept();
		Message pm = Message.parseFrom(
				incomingSocket.getInputStream());
		incomingSocket.close();
		ss.close();
		
		assertEquals(testMessage, pm);
	}
	
	
	//Must fix this to be for the new UDP protocol
	/*@Test
	public void checkThatNodeIsAlive() throws IOException {
		Thread t = new Thread(new Runnable() {
			Socket incomingSocket;
			ServerSocket ss = new ServerSocket(ProtocolController.PROTOCOL_PORT);
			PrintWriter out = null;
			@Override
			public void run() {
				try {
					incomingSocket = ss.accept();
					Message pm = Message.parseFrom(
							incomingSocket.getInputStream());
					if (pm.getType() == MessageType.LIVENESS_CHECK) {
						out = new PrintWriter(incomingSocket.getOutputStream(), false);
						out.println("alive");
						out.close();
						incomingSocket.close();
						ss.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		t.start();
		Message m = MessageBuilder.buildLivenessMessage();

		assertTrue(MessageSender.makeLivenessCheck(new TransferableMessage(m, InetAddress.getLocalHost())));
	}*/
	
	@AfterClass
	public static void setUpAfterClass() throws Exception {
		executor.shutdown();
	}

}
