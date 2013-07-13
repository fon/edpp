package comm;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

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
	
	
	@Override
	public void run() {
		InetAddress address;
		PrintWriter out = null;
		try {
			ss = new ServerSocket(ProtocolController.PROTOCOL_PORT);
			while (true) {
				incomingSocket = ss.accept();
				address = incomingSocket.getInetAddress();
				Message pm = Message.parseFrom(
						incomingSocket.getInputStream());
				if (pm.getType() == MessageType.LIVENESS_CHECK) {
					out = new PrintWriter(incomingSocket.getOutputStream(), true);
					out.println("alive");
					out.close();
					incomingSocket.close();
				} else {
					incomingQueue.add(new TransferableMessage(pm, address));
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
