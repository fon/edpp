package core;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jdbm.PrimaryTreeMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;

import network.Node;

public class ProtocolEngine {

	static final String DBNAME = "sessiondata.db";
	static final String REC_NAME = "sessionRec";
	
	private ExecutorService executor;
	private RecordManager recMan;
	private PrimaryTreeMap<Integer, RecordedSession> db;
	private ProtocolController pc;
	
	public ProtocolEngine(Node localNode) {
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
			try {
				executor.awaitTermination(10, TimeUnit.SECONDS);
				recMan.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				executor.shutdownNow();
			}
	}
	
	public Session requestSessionData() {
		Future<Session> future = executor.submit(new ProtocolRun(db, pc));
		Session s = null;
		
		try {
			s = future.get();
			System.out.println("Session id is: "+s.getSessionId());
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
	
}
