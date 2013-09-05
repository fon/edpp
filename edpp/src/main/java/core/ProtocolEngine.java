package core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import domain.SamplingParameters;
import domain.Session;
import domain.network.Node;
import event.SessionListener;
import storage.Database;
import storage.KeyValueDatabase;

/**
 * This class is the initation class of the protocol. An object of this class
 * should be created when the protocol is initialized. It is responsible for
 * spawning all the other basic threads of the protocol
 * 
 * @author Xenofon Foukas
 * 
 */
public class ProtocolEngine {

	static final String DBNAME = "sessiondata.db";
	static final String REC_NAME = "sessionRec";

	private Logger logger;

	private ExecutorService executor;
	private Database sessionDB;
	private ProtocolController pc;

	/**
	 * Class constructor
	 * 
	 * @param localNode
	 *            the abstract Node representation of the local node
	 */
	public ProtocolEngine(Node localNode) {

		logger = Logger.getLogger(ProtocolEngine.class.getName());

		sessionDB = new KeyValueDatabase(DBNAME, REC_NAME);

		// initiate the ProtocolController and set it as a daemon service
		pc = new ProtocolController(localNode, sessionDB);
		Thread controllerThread = new Thread(pc);
		controllerThread.setDaemon(true);
		controllerThread.start();

		executor = Executors.newCachedThreadPool();

	}

	/**
	 * Method responsible for terminating the protocol engine gracefully. It
	 * closes the database used and terminates the daemon threads
	 */
	public void terminate() {
		logger.info("Terminating the sampling engine gracefully");
		sessionDB.closeDatabase();
		executor.shutdownNow();
	}

	/**
	 * This method is responsible for initiating a new ProtocolRun for making a
	 * new sampling request
	 * 
	 * @param sp
	 *            the SamplingParameters that should be used for the new
	 *            sampling request
	 * @return a Session providing some estimation of the network's spectral
	 *         properties
	 */
	public Session requestSessionData(SamplingParameters sp) {
		logger.info("Submitting a request for a new protocol run");
		ProtocolRun pr = new ProtocolRun(sessionDB, pc, sp);
		// run a ProtocolRun in a new thread
		Future<Session> future = executor.submit(pr);
		Session s = null;

		// block waiting to receive the sampling data
		try {
			logger.info("Awaiting for sampling data...");
			s = future.get();
			logger.info("Received sampling data for session "
					+ s.getSessionId());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return s;
	}

	/**
	 * This method adds a SessionListener to the MessageHandlerTask and
	 * MaintenanceTask
	 * 
	 * @param listener
	 *            the SessionListener to be added
	 */
	public void addSessionListener(SessionListener listener) {
		MessageHandlerTask.addSessionListener(listener);
		MaintenanceTask.addSessionListener(listener);
	}

	/**
	 * This method removes a SessionListener from the MessageHandlerTask and the
	 * MaintenanceTask
	 * 
	 * @param listener
	 *            the SessionListener to be removed
	 */
	public void removeSessionListener(SessionListener listener) {
		MessageHandlerTask.removeSessionListener(listener);
		MaintenanceTask.removeSessionListener(listener);
	}
}
