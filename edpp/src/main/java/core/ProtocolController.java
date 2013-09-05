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
import java.util.logging.Logger;

import storage.Database;
import comm.LightMessageReceiver;
import comm.MessageReceiver;
import comm.MessageSender;
import comm.ProtocolMessage.Message.MessageType;
import comm.TransferableMessage;
import domain.Session;
import domain.network.Node;

/**
 * This class is responsible for handling all the tasks related to the protocol,
 * i.e. message handling and maintenance tasks. It should run as a daemon
 * 
 * @author Xenofon Foukas
 * 
 */
public class ProtocolController implements Runnable {

	public static final int PROTOCOL_PORT = 11990;

	public static final long TIMEOUT = 1000;

	private static final int NTHREADS = 1;

	private Node localNode;
	private BlockingQueue<TransferableMessage> incomingQueue;
	private BlockingQueue<TransferableMessage> outgoingQueue;
	private MessageReceiver receiver;
	private MessageSender sender;
	private LightMessageReceiver lightReceiver;
	private ExecutorService executor;
	private ExecutorService initExecutor;
	private ExecutorService daemonExecutor;
	private ScheduledExecutorService scheduledExecutor;
	private Map<String, Session> sessions;
	private Database db;

	private Logger logger;

	/**
	 * Class constructor
	 * 
	 * @param localNode
	 *            the abstract Node representing the local network node
	 * @param db
	 *            the Database used for storing and retrieving completed
	 *            Sessions
	 */
	public ProtocolController(Node localNode, Database db) {

		logger = Logger.getLogger(ProtocolController.class.getName());
		this.localNode = localNode;
		this.db = db;

		// initialize the blocking queues for incoming and outgoing messages
		incomingQueue = new LinkedBlockingQueue<TransferableMessage>();
		receiver = new MessageReceiver(incomingQueue);
		lightReceiver = new LightMessageReceiver(incomingQueue);

		outgoingQueue = new LinkedBlockingQueue<TransferableMessage>();
		sender = new MessageSender(outgoingQueue);

		// set the number of threads in the pool of threads for message handling
		// tasks
		executor = Executors.newFixedThreadPool(NTHREADS);
		initExecutor = Executors.newFixedThreadPool(1);
		scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		daemonExecutor = Executors.newFixedThreadPool(4, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});
		sessions = new ConcurrentHashMap<String, Session>();
	}

	@Override
	public void run() {
		TransferableMessage incomingMessage;

		// Set up and run the basic threads
		logger.info("Initiating the receiver thread");
		daemonExecutor.execute(receiver);
		logger.info("Initiating the sender thread");
		daemonExecutor.execute(sender);
		daemonExecutor.execute(lightReceiver);

		// Set the threads as daemons, so that the vm will exit
		// if only those threads remain running

		// Schedule thread maintenance
		logger.info("Initiating the maintenance task scheduler");
		scheduledExecutor.scheduleWithFixedDelay(new MaintenanceTask(sessions,
				outgoingQueue, localNode, db), TIMEOUT, TIMEOUT,
				TimeUnit.MILLISECONDS);

		while (true) {
			try {
				incomingMessage = incomingQueue.take();
				// if an INIT message arrives to the incoming queue you should
				// handle it sequentially and not concurrently with other INIT
				// messages. the reason is that creation of Executions cannot be
				// parallelized
				if (incomingMessage.getMessage().getType() == MessageType.INIT) {
					initExecutor
							.execute(new MessageHandlerTask(incomingMessage,
									sessions, localNode, outgoingQueue));
				} else {
					// if the message is of any type other than INIT, take a
					// thread from the pool and initiate a new
					// MessageHandlerTask
					executor.execute(new MessageHandlerTask(incomingMessage,
							sessions, localNode, outgoingQueue));
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	public void putMessageToInQueue(TransferableMessage tm) {
		incomingQueue.add(tm);
	}

}
