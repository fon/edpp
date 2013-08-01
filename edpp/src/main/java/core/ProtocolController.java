package core;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import storage.Database;

import comm.MessageReceiver;
import comm.MessageSender;
import comm.TransferableMessage;

import network.Node;

public class ProtocolController implements Runnable {
	
	public static final int PROTOCOL_PORT = 11990;
	public static final long TIMEOUT = 1000;
	
	private static final int NTHREADS = 1;
	

	private Node localNode;
	private BlockingQueue<TransferableMessage> incomingQueue;
	private BlockingQueue<TransferableMessage> outgoingQueue;
	private MessageReceiver receiver;
	private MessageSender sender;
	private ExecutorService executor;
	private ExecutorService daemonExecutor;
	private ScheduledExecutorService scheduledExecutor;
	private Map<String, Session> sessions;
	private Database db;
	
	public ProtocolController(Node localNode,
			Database db) {
		this.localNode = localNode;
		this.db = db;
		
		incomingQueue = new LinkedBlockingQueue<TransferableMessage>();
		receiver = new MessageReceiver(incomingQueue);
		
		outgoingQueue = new LinkedBlockingQueue<TransferableMessage>();
		sender = new MessageSender(outgoingQueue);
		
		executor = Executors.newFixedThreadPool(NTHREADS);
		scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		daemonExecutor = Executors.newFixedThreadPool(4,new ThreadFactory() {
		    public Thread newThread(Runnable r) {
		        Thread t=new Thread(r);
		        t.setDaemon(true);
		        return t;
		    }
		});
		sessions = new ConcurrentHashMap<String, Session>();
	}

	@Override
	public void run() {
		TransferableMessage incomingMessage;
		
		//Set up and run the basic threads
		daemonExecutor.execute(receiver);
		daemonExecutor.execute(sender);
		
		//Set the threads as daemons, so that the vm will exit
		//if only those threads remain running
		
		// Schedule thread maintenance
		scheduledExecutor.scheduleWithFixedDelay(new MaintenanceTask(sessions, outgoingQueue, localNode, db), 
				TIMEOUT, 100, TimeUnit.MILLISECONDS);
		
		while (true) {
			try {
				incomingMessage = 
						incomingQueue.take();
				//ReceivedMessage
				executor.execute(new MessageHandlerTask(incomingMessage, sessions, 
						localNode, outgoingQueue, db));					
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public void putMessageToInQueue(TransferableMessage tm) {
		incomingQueue.add(tm);
	}
	
}
