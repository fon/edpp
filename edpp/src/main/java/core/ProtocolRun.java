package core;

import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import storage.Database;
import comm.MessageBuilder;
import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.SessionEvent;
import comm.TransferableMessage;
import domain.RecordedSession;
import domain.SamplingParameters;
import domain.Session;
import event.SessionListener;

/**
 * Class responsible for initiating a new sampling request by creating a message
 * of type NEW and placing it to the incoming queue. It should be executed in a
 * separate thread
 * 
 * @author Xenofon Foukas
 * 
 */
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

	/**
	 * Class constructor
	 * 
	 * @param db
	 *            the database where the completed Sessions are stored
	 * @param pc
	 *            the ProtocolController object responsible for running the
	 *            protocol tasks
	 * @param sp
	 *            an object of type SamplingParameters with the parameters of
	 *            the new Execution
	 */
	public ProtocolRun(Database db, ProtocolController pc, SamplingParameters sp) {
		logger = Logger.getLogger(ProtocolRun.class.getName());

		lock = new Object();
		s = null;
		this.pc = pc;
		this.sp = sp;
		this.db = db;
		this.db.addSessionListener(this);
	}

	@Override
	public Session call() throws Exception {

		RecordedSession rs = null;
		Session previousSession = null;

		logger.info("Checking for previously stored sampling data...");
		rs = db.getLastRecordedSession();

		// if no previous session was stored make a new request
		if (rs == null) {
			logger.info("No previously stored session data found. Making a new request");
			Message m = MessageBuilder.buildNewMessage(
					sp.getNumberOfExecutions(), sp.getNumberOfRounds());
			TransferableMessage tm = new TransferableMessage(m,
					InetAddress.getLocalHost());
			pc.putMessageToInQueue(tm);
		} else if (System.currentTimeMillis() - rs.getTimestamp() <= CURRENT_THRESHOLD) {
			// if a previous Session was already stored and it is recent do not
			// initiate a new sampling process
			logger.info("A recently stored session exists. Using this");
			s = rs.getRecordedSession();
			return s;
		} else {
			// if a previous outdated Session was found, then a new sampling
			// should be initiated
			logger.info("Found a recorded session, but it is outdated. Will make a new request");
			Message m = MessageBuilder.buildNewMessage(
					sp.getNumberOfExecutions(), sp.getNumberOfRounds());
			TransferableMessage tm = new TransferableMessage(m,
					InetAddress.getLocalHost());
			pc.putMessageToInQueue(tm);
			previousSession = rs.getRecordedSession();
		}

		// Wait to get a session through a record inserted event
		synchronized (lock) {
			lock.wait();
		}
		// Adjust the threshold of the elapsed time among consecutive samplings
		if (previousSession != null)
			logger.info("Adjusting the sampling time interval threshold");
		adjustThreshold(previousSession.getComputedEigenvalues(),
				s.getComputedEigenvalues());
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
		synchronized (lock) {
			logger.info("A new record was inserted to the database for session with id "
					+ rs.getRecordedSession().getSessionId());
			s = rs.getRecordedSession();
			lock.notify();
		}
	}

	/**
	 * This method is responsible for adjusting the interval between to
	 * consecutive samplings by checking whether we have changes in the
	 * estimations from previous samples
	 * 
	 * @param previousEigenvalues
	 *            a double array of the eigenvalues estimated by the previous
	 *            Session
	 * @param newEigenvalues
	 *            a double array of the eigenvalues estimated by the new Session
	 */
	public static void adjustThreshold(double[] previousEigenvalues,
			double[] newEigenvalues) {
		// if no changes were observed to the eigenvalues increase the interval
		// between samplings
		if (!eigenvaluesChanged(previousEigenvalues, newEigenvalues, 0.05)) {
			long increase = (long) (CURRENT_INCREASE_RATE * CURRENT_THRESHOLD);
			if (CURRENT_THRESHOLD + increase <= MAX_TIME_THRESHOLD) {
				CURRENT_THRESHOLD += increase;
			} else {
				CURRENT_THRESHOLD = MAX_TIME_THRESHOLD;
			}
			if (CURRENT_INCREASE_RATE < MAX_RATE) {
				CURRENT_INCREASE_RATE += 0.05;
			}
			CURRENT_DECREASE_RATE = MIN_RATE;
		} else { // If changes were observed set the interval back to the
					// minimum value
			long decrease = (long) (CURRENT_DECREASE_RATE * CURRENT_THRESHOLD);
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

	private static boolean eigenvaluesChanged(double[] previousEigenvalues,
			double[] newEigenvalues, double errorThres) {
		if (previousEigenvalues.length != newEigenvalues.length)
			return true;

		// check if the eigenvalues are different, i.e. if their difference is
		// greater than some threshold
		for (int i = 0; i < previousEigenvalues.length; i++) {
			if (Math.abs(previousEigenvalues[i] - newEigenvalues[i]) > errorThres)
				return true;
		}
		return false;
	}

}
