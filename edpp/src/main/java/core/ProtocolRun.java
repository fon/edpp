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
	
	public static final long MIN_TIME_THRESHOLD = 100;
	public static final long MAX_TIME_THRESHOLD = 60000;
	
	public static final double MIN_RATE = 0.05;
	public static final double MAX_RATE = 1;

	public static long CURRENT_THRESHOLD = MIN_TIME_THRESHOLD;
	public static double CURRENT_INCREASE_RATE = MIN_RATE;
	public static double CURRENT_DECREASE_RATE = MIN_RATE;
	
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
		Session previousSession = null;
		
		rs = db.getLastRecordedSession();
			
		if(rs == null) {
			logger.info("No previous recorded session data found\nMaking a new request");
			//make a new request
			Message m = MessageBuilder.buildNewMessage(sp.getNumberOfExecutions(), sp.getNumberOfRounds());
			TransferableMessage tm = new TransferableMessage(m, InetAddress.getLocalHost());
			pc.putMessageToInQueue(tm);
		} else if (System.currentTimeMillis() - rs.getTimestamp() <= CURRENT_THRESHOLD) {
			logger.info("A recent session exists. No new request will be made");
			s = rs.getRecordedSession();
			return s;
		} else {
			logger.info("Found a recorded session, but it is outdated. Will make a new request");
			Message m = MessageBuilder.buildNewMessage(sp.getNumberOfExecutions(), sp.getNumberOfRounds());
			TransferableMessage tm = new TransferableMessage(m, InetAddress.getLocalHost());
			pc.putMessageToInQueue(tm);
			previousSession = rs.getRecordedSession();
		}
		
		//Wait to get a session through a record inserted event
		synchronized (lock) {
			lock.wait();
		}
		this.db.removeSessionListener(this);
		// Adjust the threshold
		if (previousSession != null)
			adjustThreshold(previousSession.getComputedEigenvalues(), s.getComputedEigenvalues());
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
	
	public static void adjustThreshold(double [] previousEigenvalues, double [] newEigenvalues) {
		if (!eigenvaluesChanged(previousEigenvalues, newEigenvalues, 0.05)) {  //No changes were made
			long increase = (long)(CURRENT_INCREASE_RATE*CURRENT_THRESHOLD);
			if (CURRENT_THRESHOLD + increase <= MAX_TIME_THRESHOLD) {
				CURRENT_THRESHOLD += increase;
			}
			else {
				CURRENT_THRESHOLD = MAX_TIME_THRESHOLD;
			}
			if (CURRENT_INCREASE_RATE < MAX_RATE) {
				CURRENT_INCREASE_RATE += 0.05;
			}
			CURRENT_DECREASE_RATE = MIN_RATE;
		} else { //If changes were observed
			long decrease = (long)(CURRENT_DECREASE_RATE*CURRENT_THRESHOLD);
			if (CURRENT_THRESHOLD - decrease >= MIN_TIME_THRESHOLD) {
				CURRENT_THRESHOLD -= decrease;
			} else {
				CURRENT_THRESHOLD = MIN_TIME_THRESHOLD;
			}
			if (CURRENT_DECREASE_RATE < MAX_RATE) {
				CURRENT_DECREASE_RATE += 0.05;
			}
			CURRENT_INCREASE_RATE = MIN_RATE;
		}
	}
	
	private static boolean eigenvaluesChanged(double [] previousEigenvalues, double [] newEigenvalues,
			double errorThres) {
		if (previousEigenvalues.length != newEigenvalues.length)
			return true;
		
		for (int i = 0; i < previousEigenvalues.length; i++) {
			if (Math.abs(previousEigenvalues[i] - newEigenvalues[i]) > errorThres)
				return true;
		}
		return false;
	}

}
