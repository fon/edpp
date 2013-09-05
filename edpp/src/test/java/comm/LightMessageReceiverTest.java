package comm;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

public class LightMessageReceiverTest {

	static BlockingQueue<TransferableMessage> queue;
	static Thread receiverThread;
	static ExecutorService executor;
	static LightMessageReceiver mr;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		queue = new LinkedBlockingQueue<TransferableMessage>();
		executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});
		mr = new LightMessageReceiver(queue);
		executor.execute(mr);
	}

	@Test
	public void checkThatMessageIsDelivered() throws UnknownHostException, IOException, InterruptedException {
		Message testMessage = Message.newBuilder()
				.setType(MessageType.NEW)
				.setExecution(100)
				.setRound(200)
				.build();
		ByteArrayOutputStream output = new ByteArrayOutputStream(
				2048);
		testMessage.writeDelimitedTo(output);
		DatagramSocket socket = new DatagramSocket();
		byte[] buf = output.toByteArray();
		DatagramPacket packet = new DatagramPacket(buf, buf.length,
				InetAddress.getLocalHost(), ProtocolController.PROTOCOL_PORT);
		socket.send(packet);
		socket.close();
		TransferableMessage receivedMessage = queue.take();
		assertEquals(testMessage, receivedMessage.getMessage());
		mr.stopService();
	}
	
	@AfterClass
	public static void setUpAfterClass() throws Exception {
//		mr.stopService();
		executor.shutdown();
	}

}
