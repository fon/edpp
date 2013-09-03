package comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;

import core.ProtocolController;

/**
 * Class that is responsible for forwarding protocol messages from the protocol
 * controller to remote nodes either by using TCP or UDP
 * 
 * @author Xenofon Foukas
 * 
 */
public class MessageSender implements Runnable {

	private BlockingQueue<TransferableMessage> outgoingQueue;
	private Socket s;

	/**
	 * Constructor class
	 * 
	 * @param outgoigQueue
	 *            the queue, where the incoming messages will be placed
	 */
	public MessageSender(BlockingQueue<TransferableMessage> outgoigQueue) {
		this.outgoingQueue = outgoigQueue;
	}

	@Override
	public void run() {
		while (true) {
			TransferableMessage m = null;
			try {
				// Take a message from the queue of messages waiting to be
				// transferred
				m = outgoingQueue.take();
				if (m.getSendReliably()) { // if the message needs to be sent
											// through TCP
					s = new Socket();
					// set a timeout to the socket, because the remote node
					// might take to long to reply
					s.connect(new InetSocketAddress(m.getAddress(),
							ProtocolController.PROTOCOL_PORT), 10000);
					m.getMessage().writeTo(s.getOutputStream());
					s.close();
				} else { // if the message uses the UDP protocol
					// create a datagram socket and send it
					ByteArrayOutputStream output = new ByteArrayOutputStream(
							2048);
					m.getMessage().writeDelimitedTo(output);
					DatagramSocket socket = new DatagramSocket();
					byte[] buf = output.toByteArray();
					DatagramPacket packet = new DatagramPacket(buf, buf.length,
							m.getAddress(), ProtocolController.PROTOCOL_PORT);
					socket.send(packet);
					socket.close();
				}
			} catch (IOException e) {
				// if the message is not sent for any reason (timer expiry, io
				// error etc) put it back to the queue and try to retransmit it
				// later
				try {
					outgoingQueue.put(m);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Checks whether a remote node is alive by sending a LIVENESS_CHECK message
	 * through UDP
	 * 
	 * @param tm
	 *            a liveness check message for a remote node
	 * @return true if the remote node is alive
	 */
	public static boolean makeLivenessCheck(TransferableMessage tm) {
		ByteArrayOutputStream output;
		int numOfTries = 0;
		do {
			try {
				output = new ByteArrayOutputStream(2048);
				tm.getMessage().writeDelimitedTo(output);
				// Create a datagram to send the liveness check message
				DatagramSocket socket = new DatagramSocket();
				byte[] buf = output.toByteArray();
				DatagramPacket packet = new DatagramPacket(buf, buf.length,
						tm.getAddress(), ProtocolController.PROTOCOL_PORT);
				socket.send(packet);
				// Wait for 3sec to receive a reply to the probe. It might be
				// better to reduce this value. Need to check it
				buf = new byte[64];
				packet = new DatagramPacket(buf, buf.length);
				socket.setSoTimeout(3000);
				// if a reply is received the node is alive. no need to check
				// the contents of the message
				socket.receive(packet);
				socket.close();
				return true;
			} catch (SocketTimeoutException ste) {
				numOfTries++;
			} catch (IOException e) {
				// if the message was not sent increase the number of failed
				// attempts
				numOfTries++;
			}
		} while (numOfTries <= 3); // stop trying after 3 failed attempts
		return false;
	}

}
