package core;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import comm.OutgoingMessage;

public class MaintenanceTask implements Runnable {
	
	private Map<String, Session> sessions;
	private BlockingQueue<OutgoingMessage> outgoingQueue;

	public MaintenanceTask(Map<String, Session> sessions,
			BlockingQueue<OutgoingMessage> outgoingQueue) {
		this.sessions = sessions;
		this.outgoingQueue = outgoingQueue;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
