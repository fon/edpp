package core;

import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import storage.Database;
import util.SamplingParameters;

import comm.MessageBuilder;
import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.SessionEvent;
import comm.TransferableMessage;

public class ProtocolRun implements Callable<Session>, SessionListener {

	public static final long TIME_THRESHOLD = 10000;
	
	final Object lock; 
	
	private Logger logger;
	
	private Session s;
	private Database db;
	private ProtocolController pc;
	private SamplingParameters sp;
	
	public ProtocolRun(Database db, ProtocolController pc,
			SamplingParameters sp) {
		logger = Logger.getLogger(ProtocolRun.class.getName());
		
		lock = new Object();
		s = null;
		this.pc = pc;
		this.sp = sp;
		this.db = db;
		this.db.addSessionListener(this);
	}
	
	@Override
	public Session call() throws Exception{
		
		RecordedSession rs = null;
		
		rs = db.getLastRecordedSession();
			
		if(rs == null) {
			logger.info("No previous recorded session data found\nMaking a new request");
			//make a new request
			Message m = MessageBuilder.buildNewMessage(sp.getNumberOfExecutions(), sp.getNumberOfRounds());
			TransferableMessage tm = new TransferableMessage(m, InetAddress.getLocalHost());
			pc.putMessageToInQueue(tm);
		} else if (System.currentTimeMillis() - rs.getTimestamp() <= TIME_THRESHOLD) {
			logger.info("A recent session exists. No new request will be made");
			s = rs.getRecordedSession();
			return s;
		} else {
			logger.info("Found a recorded session, but it is outdated. Will make a new request");
			Message m = MessageBuilder.buildNewMessage(sp.getNumberOfExecutions(), sp.getNumberOfRounds());
			TransferableMessage tm = new TransferableMessage(m, InetAddress.getLocalHost());
			pc.putMessageToInQueue(tm);
		}
		
		//Wait to get a session through a record inserted event
		synchronized (lock) {
			lock.wait();
		}
		this.db.removeSessionListener(this);
		return s;
		
	}

	@Override
	public void sessionInitiated(SessionEvent e) {
		return;
	}

	@Override
	public void sessionCompleted(SessionEvent e) {
		return;
	}

	@Override
	public void sessionStored(RecordedSession rs) {
		logger.info("A new record was inserted to the database");
		synchronized (lock) {
			s = rs.getRecordedSession();
			lock.notify();
		}
	}

}
