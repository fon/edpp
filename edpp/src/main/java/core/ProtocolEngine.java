package core;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	
	public ProtocolEngine(Node localNode) {
		Thread controllerThread = new Thread(new ProtocolController(localNode, db));
		controllerThread.setDaemon(true);
		controllerThread.start();
		
		try {
			recMan = RecordManagerFactory.createRecordManager(DBNAME);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		db = recMan.treeMap(REC_NAME);
		
		executor = Executors.newCachedThreadPool();
		
	}
	
	public Session requestSessionData() {
		Future<Session> future = executor.submit(new ProtocolRun(db));
		Session s = null;
		
		try {
			s = future.get();
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
