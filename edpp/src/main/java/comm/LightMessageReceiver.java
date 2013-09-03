package comm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;
import core.ProtocolController;

/**
 * 
 * Class for receiving protocol messages through UDP. An object of this class
 * should be created in a thread running as a daemon service
 * 
 * @author Xenofon Foukas
 * 
 */
public class LightMessageReceiver implements Runnable {

	private BlockingQueue<TransferableMessage> incomingQueue;
	private DatagramSocket ss;

	/**
	 * Constructor class
	 * 
	 * @param incomingQueue
	 *            BlockingQueue in which any message received should be placed
	 *            and which communicates with the ProtocolController
	 */
	public LightMessageReceiver(BlockingQueue<TransferableMessage> incomingQueue) {
		this.incomingQueue = incomingQueue;
	}

	@Override
	public void run() {
		try {
			ss = new DatagramSocket(ProtocolController.PROTOCOL_PORT);
			// Listen to the PROTOCOL_PORT for any incoming datagrams
			while (true) {
				byte[] buf = new byte[2048];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				ss.receive(packet);
				ByteArrayInputStream input = new ByteArrayInputStream(buf);
				Message m = Message.parseDelimitedFrom(input);

				// If the message is a LIVENESS_CHECK just reply with an empty
				// datagram to prove liveness
				if (m.getType() == MessageType.LIVENESS_CHECK) {
					byte[] rep = new byte[64];
					DatagramPacket pack = new DatagramPacket(rep, rep.length,
							packet.getAddress(), packet.getPort());
					ss.send(pack);
				} else { // Otherwise just put it in the blocking queue to
							// forward it to the ProtocolController
					incomingQueue.add(new TransferableMessage(m, packet
							.getAddress(), false));
				}
			}
		} catch (IOException e) {
			// TODO Must fix this to locate the exact case of the exception
		}
	}

}
