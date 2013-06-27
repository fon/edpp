package core;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import comm.MessageReceiver;
import comm.MessageSender;
import comm.TransferableMessage;

import network.Node;

public class ProtocolEngine implements Runnable {
	
	public static final int PROTOCOL_PORT = 11990;
	public static final long TIMEOUT = 1000;
	
	private static final int NTHREADS = 5;
	

	private Node localNode;
	private BlockingQueue<TransferableMessage> incomingQueue;
	private BlockingQueue<TransferableMessage> outgoingQueue;
	private MessageReceiver receiver;
	private MessageSender sender;
	private ExecutorService executor;
	private ScheduledExecutorService scheduledExecutor;
	private Map<String, Session> sessions;
	
	public ProtocolEngine(Node localNode) {
		this.localNode = localNode;
		
		incomingQueue = new LinkedBlockingQueue<TransferableMessage>();
		receiver = new MessageReceiver(incomingQueue);
		
		outgoingQueue = new LinkedBlockingQueue<TransferableMessage>();
		sender = new MessageSender(outgoingQueue);
		
		executor = Executors.newFixedThreadPool(NTHREADS);
		scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		
		sessions = new ConcurrentHashMap<String, Session>();
	}

	@Override
	public void run() {
		TransferableMessage incomingMessage;
		
		//Set up and run the basic threads
		Thread receivingThread = new Thread(receiver);
		Thread sendingThread = new Thread(sender);
		
		//Set the threads as daemons, so that the vm will exit
		//if only those threads remain running
		receivingThread.setDaemon(true);
		sendingThread.setDaemon(true);
		
		receivingThread.start();
		sendingThread.start();
		
		// Schedule thread maintenance
		scheduledExecutor.scheduleWithFixedDelay(new MaintenanceTask(sessions, outgoingQueue, localNode), 
				TIMEOUT, TIMEOUT, TimeUnit.MILLISECONDS);
		
		while (true) {
			try {
				incomingMessage = 
						incomingQueue.take();
				//ReceivedMessage
				executor.execute(new MessageHandlerTask(incomingMessage, sessions, 
						localNode, outgoingQueue));					
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
}
