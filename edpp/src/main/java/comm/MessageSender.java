package comm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import core.ProtocolController;

public class MessageSender implements Runnable {

	private BlockingQueue<TransferableMessage> outgoingQueue;
	private Socket s;
	
	public MessageSender(BlockingQueue<TransferableMessage> outgoigQueue) {
		this.outgoingQueue = outgoigQueue;
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				TransferableMessage m = outgoingQueue.take();
				s = new Socket(m.getAddress(), ProtocolController.PROTOCOL_PORT);
				m.getMessage().writeTo(s.getOutputStream());
				s.close();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static boolean makeLivenessCheck(TransferableMessage tm) {
		BufferedReader in = null;
		try {
			Socket s = new Socket(tm.getAddress(), ProtocolController.PROTOCOL_PORT);
			tm.getMessage().writeTo(s.getOutputStream());
			in = new BufferedReader(new InputStreamReader(
                    s.getInputStream()));
			String reply = in.readLine();
			if (reply != null) 
				return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}
			
}

