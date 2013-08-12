package comm;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;
import core.ProtocolController;

/**
 * Class that is responsible for receiving incoming protocol messages and forwarding
 * them to the protocol controller
 * @author Xenofon Foukas
 *
 */
public class MessageReceiver implements Runnable {

	private BlockingQueue<TransferableMessage> incomingQueue;
	private ServerSocket ss;
	private Socket incomingSocket;
	
	/**
	 * Class constructor
	 * @param incomingQueue the queue, where the incoming messages will be placed
	 */
	public MessageReceiver(BlockingQueue<TransferableMessage> incomingQueue) {
		this.incomingQueue = incomingQueue;
	}
	
	
	class LivenessListener implements Runnable {

		Socket inSocket;
		
		public LivenessListener(Socket inSocket) {
			this.inSocket = inSocket;
		}
		
		@Override
		public void run() {
			try {
				PrintWriter out = new PrintWriter(inSocket.getOutputStream(), true);
				out.println("alive");
				out.close();
				inSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public void run() {
		InetAddress address;
		ExecutorService executor = Executors.newFixedThreadPool(10);
		try {
			ss = new ServerSocket(ProtocolController.PROTOCOL_PORT, 100);
			while (true) {
				incomingSocket = ss.accept();
				address = incomingSocket.getInetAddress();
				Message pm = Message.parseFrom(
						incomingSocket.getInputStream());
				if (pm.getType() == MessageType.LIVENESS_CHECK) {
					executor.execute(new LivenessListener(incomingSocket));
				} else {
					incomingQueue.add(new TransferableMessage(pm, address));
					incomingSocket.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// TODO Must fix this to locate the exact case of the exception
		}

	}
	
	/**
	 * Stops the message receiving service, by closing the server socket
	 */
	public void stopService() {
		try {
			ss.close();
		} catch (IOException e) {

		}
	}

}
