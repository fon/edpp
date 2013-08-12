package comm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;

import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;
import core.ProtocolController;

public class LightMessageReceiver implements Runnable {

	private BlockingQueue<TransferableMessage> incomingQueue;
	private DatagramSocket ss;
	
	public LightMessageReceiver(BlockingQueue<TransferableMessage> incomingQueue) {
		this.incomingQueue = incomingQueue;
	}

	@Override
	public void run() {
		try {
			ss = new DatagramSocket(ProtocolController.PROTOCOL_PORT);
			while (true) {
				byte[] buf = new byte[2048];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				ss.receive(packet);
				ByteArrayInputStream input = new ByteArrayInputStream(buf);
				Message m = Message.parseDelimitedFrom(input);
				if (m.getType() == MessageType.LIVENESS_CHECK) {
					byte [] rep = new byte[64];
					DatagramPacket pack = new  DatagramPacket(rep, rep.length, 
							packet.getAddress(), packet.getPort());
					ss.send(pack);
				} else {
					incomingQueue.add(new TransferableMessage(m, packet.getAddress(), false));
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// TODO Must fix this to locate the exact case of the exception
		}
	}
	
	
}
