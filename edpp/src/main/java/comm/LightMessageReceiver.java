package comm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.BlockingQueue;

import comm.ProtocolMessage.Message;
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
				incomingQueue.add(new TransferableMessage(m, packet.getAddress(), false));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// TODO Must fix this to locate the exact case of the exception
		}
	}
	
	
}
