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

public class ProtocolEngine {

	static final String DBNAME = "sessiondata.db";
	static final String REC_NAME = "sessionRec";
	
	private Logger logger;
	
	private ExecutorService executor;
	private Database sessionDB;
	private ProtocolController pc;
	
	public ProtocolEngine(Node localNode) {
		
		logger = Logger.getLogger(ProtocolEngine.class.getName());
		
		sessionDB = new KeyValueDatabase(DBNAME, REC_NAME);

		pc = new ProtocolController(localNode, sessionDB);
		Thread controllerThread = new Thread(pc);
		controllerThread.setDaemon(true);
		controllerThread.start();
		
		
		executor = Executors.newCachedThreadPool();
		
	}
	
	public void terminate() {
			logger.info("Terminating the sampling engine gracefully");
			sessionDB.closeDatabase();
			executor.shutdownNow();
	}
	
	public Session requestSessionData(SamplingParameters sp) {
		logger.info("Submitting a request for a new protocol run");
		ProtocolRun pr = new ProtocolRun(sessionDB, pc, sp);
		Future<Session> future = executor.submit(pr);
		Session s = null;
		
		try {
			logger.info("Awaiting for sampling data...");
			s = future.get();
			logger.info("Received sampling data for session "+s.getSessionId());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
	}
	
	public void addSessionListener(SessionListener listener) {
		MessageHandlerTask.addSessionListener(listener);
		MaintenanceTask.addSessionListener(listener);
	}
	
	public void removeSessionListener(SessionListener listener) {
		MessageHandlerTask.removeSessionListener(listener);
		MaintenanceTask.removeSessionListener(listener);
	}
}
