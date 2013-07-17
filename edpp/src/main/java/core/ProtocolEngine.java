package core;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import util.SamplingParameters;

import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;

import network.Node;

public class ProtocolEngine {

	static final String DBNAME = "sessiondata.db";
	static final String REC_NAME = "sessionRec";
	
	private Logger logger;
	
	private ExecutorService executor;
	private RecordManager recMan;
	private PrimaryTreeMap<Integer, RecordedSession> db;
	private ProtocolController pc;
	
	public ProtocolEngine(Node localNode) {
		
		logger = Logger.getLogger(ProtocolEngine.class.getName());
		
		try {
			recMan = RecordManagerFactory.createRecordManager(DBNAME);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		db = recMan.treeMap(REC_NAME);
		pc = new ProtocolController(localNode, db);
		Thread controllerThread = new Thread(pc);
		controllerThread.setDaemon(true);
		controllerThread.start();
		
		
		executor = Executors.newCachedThreadPool();
		
	}
	
	public void terminate() {
			logger.info("Terminating the sampling engine gracefully");
			try {
				recMan.commit();
				recMan.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			executor.shutdownNow();
	}
	
	public Session requestSessionData(SamplingParameters sp) {
		logger.info("Submitting a request for a new protocol run");
		Future<Session> future = executor.submit(new ProtocolRun(db, pc, sp));
		Session s = null;
		
		try {
			logger.info("Awaiting for sampling data...");
			s = future.get();
			logger.info("Received sampling data for session "+s.getSessionId());
			recMan.commit();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
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
