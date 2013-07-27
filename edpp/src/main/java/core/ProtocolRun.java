package core;

import java.io.IOException;
import java.net.InetAddress;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import util.SamplingParameters;

import comm.MessageBuilder;
import comm.ProtocolMessage.Message;
import comm.TransferableMessage;

import jdbm.PrimaryTreeMap;
import jdbm.RecordListener;

public class ProtocolRun implements Callable<Session>, RecordListener<Integer, RecordedSession> {

	public static final long TIME_THRESHOLD = 10000;
	
	final Object lock; 
	
	private Logger logger;
	
	private Session s;
	private PrimaryTreeMap<Integer, RecordedSession> db;
	private ProtocolController pc;
	private SamplingParameters sp;
	
	public ProtocolRun(PrimaryTreeMap<Integer, RecordedSession> db, ProtocolController pc,
			SamplingParameters sp) {
		logger = Logger.getLogger(ProtocolRun.class.getName());
		
		lock = new Object();
		s = null;
		this.pc = pc;
		this.sp = sp;
		this.db = db;
		synchronized (this.db) {
			this.db.addRecordListener(this);
		}
	}
	
	@Override
	public Session call() throws Exception{
		
		RecordedSession rs = null;
		int lastRecord;
		
		synchronized (db) {
			try {
				lastRecord = db.lastKey();
			} catch (NoSuchElementException e) {
				lastRecord = 0;
			}
			System.out.println("The size is: "+lastRecord);
			rs = db.find(new Integer(lastRecord));
		}
			
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
		synchronized (db) {
			this.db.removeRecordListener(this);
		}
		return s;
		
	}

	@Override
	public void recordInserted(Integer arg0, RecordedSession arg1) throws IOException {
		logger.info("A new record was inserted to the database");
		synchronized (lock) {
			s = arg1.getRecordedSession();
			lock.notify();
		}
	}

	@Override
	public void recordRemoved(Integer arg0, RecordedSession arg1) throws IOException {
		return;
	}

	@Override
	public void recordUpdated(Integer arg0, RecordedSession arg1, RecordedSession arg2)
			throws IOException {
		return;
	}

}
