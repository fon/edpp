package comm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import comm.ProtocolMessage.Message;
import core.ProtocolController;

/**
 * Class that is responsible for receiving incoming protocol messages through
 * TCP connections and forwarding them to the protocol controller
 * 
 * @author Xenofon Foukas
 * 
 */
public class MessageReceiver implements Runnable {

	private BlockingQueue<TransferableMessage> incomingQueue;
	private ServerSocket ss;
	private Socket incomingSocket;

	/**
	 * Class constructor
	 * 
	 * @param incomingQueue
	 *            the queue, where the incoming messages will be placed
	 */
	public MessageReceiver(BlockingQueue<TransferableMessage> incomingQueue) {
		this.incomingQueue = incomingQueue;
	}

	@Override
	public void run() {
		InetAddress address;
		try {
			// Accept up to 100 concurrent TCP connections. This could probably
			// be increased more
			ss = new ServerSocket(ProtocolController.PROTOCOL_PORT, 100);
			while (true) {
				incomingSocket = ss.accept();
				address = incomingSocket.getInetAddress();
				Message pm = Message.parseFrom(incomingSocket.getInputStream());

				// place the message in the incoming queue to be handled by the
				// ProtocolController
				incomingQueue.add(new TransferableMessage(pm, address, true));
				incomingSocket.close();
			}
		} catch (IOException e) {
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
