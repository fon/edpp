package comm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import core.ProtocolController;

/**
 * Class that is responsible for forwarding protocol messages from the protocol controller
 * to remote nodes
 * @author Xenofon Foukas
 *
 */
public class MessageSender implements Runnable {

	private BlockingQueue<TransferableMessage> outgoingQueue;
	private Socket s;
	
	/**
	 * Constructor class
	 * @param outgoigQueue the queue, where the incoming messages will be placed
	 */
	public MessageSender(BlockingQueue<TransferableMessage> outgoigQueue) {
		this.outgoingQueue = outgoigQueue;
	}
	
	@Override
	public void run() {
		while (true) {
				TransferableMessage m=null;
				try {
					m = outgoingQueue.take();
					s = new Socket();
					s.connect(new InetSocketAddress(m.getAddress(), ProtocolController.PROTOCOL_PORT),10000);
					m.getMessage().writeTo(s.getOutputStream());
					s.close();
				} catch (IOException e) {
					//Place it again on queue to try retransmission later
					try {
						outgoingQueue.put(m);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	/**
	 * Checks whether a remote node is alive
	 * @param tm a liveness check message for a remote node
	 * @return true if the remote node is alive
	 */
	public static boolean makeLivenessCheck(TransferableMessage tm) {
		BufferedReader in = null;
		int numOfTries = 0;
		do {
//			System.out.println("Making liveness check to node "+tm.getAddress());
			try {
				Socket s = new Socket();
				s.connect(new InetSocketAddress(tm.getAddress(), ProtocolController.PROTOCOL_PORT), 0);
				tm.getMessage().writeTo(s.getOutputStream());
				s.shutdownOutput();
				in = new BufferedReader(new InputStreamReader(
						s.getInputStream()));
				in.readLine();
				in.close();
				s.close();
				return true;
			} catch (IOException e) {
				numOfTries++;
			}
		} while (numOfTries <= 3);
		return false;
	}
			
}

