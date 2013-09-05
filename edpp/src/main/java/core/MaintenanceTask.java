package core;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import storage.Database;
import comm.MessageBuilder;
import comm.MessageSender;
import comm.ProtocolMessage.SessionEvent;
import comm.TransferableMessage;
import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;
import comm.ProtocolMessage.SessionEvent.EventType;
import domain.Execution;
import domain.Phase;
import domain.PlainNeighbor;
import domain.RecordedSession;
import domain.Session;
import domain.TimedNeighbor;
import domain.network.Node;
import domain.structure.PlainNeighborsTable;
import domain.structure.TimedNeighborsTable;
import event.SessionListener;

/**
 * This is the scheduled task that runs periodically and checks whether remote
 * nodes have failed and if an Execution should proceed to the next round,
 * terminate etc.
 * 
 * @author Xenofon Foukas
 * 
 */
public class MaintenanceTask implements Runnable {

	private Logger logger;

	private Map<String, Session> sessions;
	private BlockingQueue<TransferableMessage> outgoingQueue;
	private Node localNode;
	private Database db;
	private static List<SessionListener> sessionListeners = new LinkedList<SessionListener>();

	/**
	 * Constructor class
	 * 
	 * @param sessions
	 *            a map containing all the currently active sessions and their
	 *            ids as keys
	 * @param outgoingQueue
	 *            the queue, where the messages intended for remote nodes will
	 *            be placed
	 * @param localNode
	 *            an object of type Node representing the local node
	 * @param db
	 *            a database which will be used for storing completed Sessions
	 */
	public MaintenanceTask(Map<String, Session> sessions,
			BlockingQueue<TransferableMessage> outgoingQueue, Node localNode,
			Database db) {
		logger = Logger.getLogger(MaintenanceTask.class.getName());
		this.sessions = sessions;
		this.outgoingQueue = outgoingQueue;
		this.localNode = localNode;
		this.db = db;
	}

	// TODO MUST break this run() into smaller tasks, because it is too long
	// (use private methods probably)

	@Override
	public void run() {
		logger.fine("Running maintenance task");
		Execution e;
		TimedNeighborsTable inNeighbors;
		Collection<Session> sessionSet = sessions.values();

		Iterator<Session> sessionIter = sessionSet.iterator();
		Session s;
		// iterate over the active Sessions
		while (sessionIter.hasNext()) {
			s = sessionIter.next();
			// Iterate over the Executions of a Session
			for (int i = 1; i <= s.getCurrentNumberOfExecutions(); i++) {
				e = s.getExecution(i);
				if (e != null) {
					// Check if it is in the INIT phase
					if (e.getPhase() == Phase.INIT) {
						// Check if the INIT phase should end, i.e. whether the
						// timer of this phase has expired
						if (e.remainingInitTime() <= 0) {
							logger.info("Stage is " + e.getPhase()
									+ " and remaining time is "
									+ e.remainingInitTime());
							logger.info("The INIT phase of Execution "
									+ e.getExecutionNumber()
									+ " in Session "
									+ s.getSessionId()
									+ " is over. Entering the DATA_EXCHANGE phase");
							/*
							 * Enter DATA_EXCHANGE phase Go to next round and
							 * send message to all out-neighbors
							 */
							e.setPhase(Phase.DATA_EXCHANGE);
							e.setRound(2);

							// check whether a new Execution should be initiated
							// in this round
							checkForNewExecution(s, e);
							logger.info("Recomputed weights and set round to "
									+ e.getCurrentRound() + " in execution "
									+ e.getExecutionNumber() + " of session "
									+ s.getSessionId());

							// if the Execution has another round, send next
							// messages and renew timers of in-neighbors
							if (e.hasAnotherRound()) {
								sendOutNextMessage(MessageType.NEXT, s, e);
								e.getInNeighbors().renewTimers();
								int newRound = e.getCurrentRound() + 1;
								checkForNewExecution(s, e);
								logger.info("Round " + e.getCurrentRound()
										+ " of execution "
										+ e.getExecutionNumber()
										+ " in session " + s.getSessionId()
										+ " is over. Going to round "
										+ newRound);
								e.setRound(newRound);
							} else { // it he Execution does not have another
										// round, switch to GOSSIP phase
								logger.info("Round "
										+ e.getCurrentRound()
										+ " of execution "
										+ e.getExecutionNumber()
										+ " in session "
										+ s.getSessionId()
										+ " is over. This was the final round. "
										+ "Switching to GOSSIP phase");
								e.setPhase(Phase.GOSSIP);
								e.computeRealizationMatrix();
								logger.info("The realization matrix of execution "
										+ e.getExecutionNumber()
										+ " of session "
										+ s.getSessionId()
										+ " was computed");

								// add any gossip messages already received to
								// the gossip data of the Execution
								e.transferPendingGossipMessages();

								// compute the eigenvalues of the approximation
								// matrix
								// TODO probably should test this for null
								double[] eigenvals = e.getMatrixAEigenvalues();
								String eig = "[";
								for (int j = 0; j < eigenvals.length; j++) {
									eig += eigenvals[j];
									eig += ", ";
								}
								eig += "]";

								// Send only the 3 largest eigenvalues. This
								// should probably be implemented to change
								// dynamically by the user
								double[] valsToSend;
								if (eigenvals.length > 3) {
									valsToSend = new double[3];
									for (int j = 0; j < 3; j++) {
										valsToSend[j] = eigenvals[j];
									}
								} else {
									valsToSend = eigenvals;
								}
								logger.info("The computed eigenvalues of execution "
										+ e.getExecutionNumber()
										+ " of session "
										+ s.getSessionId()
										+ " are " + eig);
								Message msg = MessageBuilder
										.buildGossipMessage(localNode
												.getLocalId().toString(), s
												.getSessionId(), e
												.getExecutionNumber(),
												valsToSend);
								// send GOSSIP message to out-neighbors
								sendGossipMessage(msg, e);
							}
						}
					} else if (e.getPhase() == Phase.DATA_EXCHANGE) { /*
																	 * Check for
																	 * in
																	 * -neighbors
																	 * suspected
																	 * of
																	 * failure
																	 */
						e.setProperTimersToInf();
						inNeighbors = e.getInNeighbors();
						boolean endOfRound = true;

						// iterate over the in-neighbors to check whether a node
						// has failed
						synchronized (inNeighbors) {
							Iterator<TimedNeighbor> iter = inNeighbors
									.iterator();

							while (iter.hasNext()) {
								TimedNeighbor neighbor = iter.next();

								long remainingTime = neighbor.getTimeToProbe();
								if (remainingTime != TimedNeighbor.INF)
									neighbor.decreaseTime(ProtocolController.TIMEOUT);
								if (neighbor.getTimeToProbe() <= 0) { /*
																	 * Check
																	 * whether
																	 * the node
																	 * is alive
																	 */
									if (isAlive(neighbor)) {
										// if the node is alive, the value the
										// present node expects is probably
										// lost; request retransmission and
										// renew the timer of the suspected node
										requestPreviousVal(s.getSessionId(),
												e.getExecutionNumber(),
												e.getCurrentRound(),
												neighbor.getAddress());

										inNeighbors.renewTimer(neighbor);
										endOfRound = false;
									} else { // if the node has failed just
												// remove it from the list
										logger.info("Node "
												+ neighbor.getId().toString()
												+ " is no longer alive."
												+ " Removing it from in-neihgbors table of Execution "
												+ e.getExecutionNumber()
												+ " in Session "
												+ s.getSessionId());
										iter.remove();

										// give a hint to the overlay that a
										// node has failed and it should
										// probably remove it
										if (localNode
												.removeOutNeighborNode(neighbor
														.getId().toString())) {
											logger.info("Node "
													+ neighbor.getId()
															.toString()
													+ " is no longer alive."
													+ " Removing it from out-neihgbors table");
										}
									}
								} else if (neighbor.getTimeToProbe() != TimedNeighbor.INF) {
									endOfRound = false;
								}
							}
						}
						if (endOfRound) { // If the round is over, check if it
											// was the last round or not
							e.getInNeighbors().renewTimers();
							// If it is the end of the round check if we have
							// another round
							if (e.hasAnotherRound()) {
								sendOutNextMessage(MessageType.NEXT, s, e);
								int newRound = e.getCurrentRound() + 1;
								logger.info("Round " + e.getCurrentRound()
										+ " of execution "
										+ e.getExecutionNumber()
										+ " in session " + s.getSessionId()
										+ " is over. Going to round "
										+ newRound);
								e.setRound(e.getCurrentRound() + 1);
								checkForNewExecution(s, e);
							} else { // if this was the last round, go to the
										// Gossip Round phase
								logger.info("Round "
										+ e.getCurrentRound()
										+ " of execution "
										+ e.getExecutionNumber()
										+ " in session "
										+ s.getSessionId()
										+ " is over. This was the final round. "
										+ "Switching to GOSSIP phase");
								e.setRound(e.getCurrentRound() + 1);
								e.setPhase(Phase.GOSSIP);

								// Compute the eigenvalue estimations of the
								// current node
								e.computeRealizationMatrix();
								logger.info("The realization matrix of execution "
										+ e.getExecutionNumber()
										+ " of session "
										+ s.getSessionId()
										+ " was computed");
								e.transferPendingGossipMessages();

								// TODO probably should test this for null
								double[] eigenvals = e.getMatrixAEigenvalues();
								String eig = "[";
								for (int j = 0; j < eigenvals.length; j++) {
									eig += eigenvals[j];
									eig += ", ";
								}
								eig += "]";

								// keep only the three largest eigenvalues
								double[] valsToSend;
								if (eigenvals.length > 3) {
									valsToSend = new double[3];
									for (int j = 0; j < 3; j++) {
										valsToSend[j] = eigenvals[j];
									}
								} else {
									valsToSend = eigenvals;
								}
								logger.info("The computed eigenvalues of execution "
										+ e.getExecutionNumber()
										+ " of session "
										+ s.getSessionId()
										+ " are " + eig);
								Message msg = MessageBuilder
										.buildGossipMessage(localNode
												.getLocalId().toString(), s
												.getSessionId(), e
												.getExecutionNumber(),
												valsToSend);
								// send GOSSIP message to out-neighbors
								sendGossipMessage(msg, e);
							}
						}
					} else if (e.getPhase() == Phase.GOSSIP) {
						e.transferPendingGossipMessages();
						inNeighbors = e.getInNeighbors();

						boolean endOfRound = true;
						synchronized (inNeighbors) {
							Iterator<TimedNeighbor> iter = inNeighbors
									.iterator();
							while (iter.hasNext()) {
								TimedNeighbor neighbor = iter.next();
								long remainingTime = neighbor.getTimeToProbe();
								if (remainingTime != TimedNeighbor.INF)
									neighbor.decreaseTime(ProtocolController.TIMEOUT);
								if (neighbor.getTimeToProbe() <= 0) { /*
																	 * Check
																	 * whether
																	 * the node
																	 * is alive
																	 */
									if (isAlive(neighbor)) {
										inNeighbors.renewTimer(neighbor);
										requestPreviousVal(s.getSessionId(),
												e.getExecutionNumber(), -1,
												neighbor.getAddress());
										endOfRound = false;
									} else {
										logger.info("Node "
												+ neighbor.getId().toString()
												+ " is no longer alive."
												+ " Removing it from in-neihgbors table of Execution "
												+ e.getExecutionNumber()
												+ " in Session "
												+ s.getSessionId());
										iter.remove();
										if (localNode
												.removeOutNeighborNode(neighbor
														.getId().toString())) {
											logger.info("Node "
													+ neighbor.getId()
															.toString()
													+ " is no longer alive."
													+ " Removing it from out-neihgbors table");
										}
									}
								} else if (neighbor.getTimeToProbe() != TimedNeighbor.INF) {
									endOfRound = false;
								}
							}
						}
						if (endOfRound) {
							double[] medianEig = e.computeMedianEigenvalues();
							String eig = "[";
							for (int j = 0; j < medianEig.length; j++) {
								eig += medianEig[j];
								eig += ", ";
							}
							eig += "]";
							logger.info("Gossip round of execution "
									+ e.getExecutionNumber()
									+ " of session "
									+ s.getSessionId()
									+ " is now over. The median of all received eigenvalues is "
									+ eig);
							e.setPhase(Phase.TERMINATED);
							s.addCompletedExecution();
							if (s.hasTerminated()) {
								sessionIter.remove();
								RecordedSession recSes = new RecordedSession(s);
								db.addSession(recSes);
								// Notify all listeners
								for (SessionListener sl : sessionListeners) {
									SessionEvent se = MessageBuilder
											.buildNewSessionEvent(s, localNode,
													EventType.TERMINAL);
									sl.sessionCompleted(se);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Adds a SessionListener to the MaintenanceTask
	 * 
	 * @param listener
	 *            the SessionListener to be added
	 */
	public static void addSessionListener(SessionListener listener) {
		sessionListeners.add(listener);
	}

	/**
	 * Removes a SessionListener from the Maintenance Task
	 * 
	 * @param listener
	 *            the SessionListener to be removed
	 * @return true if the listener was removed, otherwise false
	 */
	public static boolean removeSessionListener(SessionListener listener) {
		return sessionListeners.remove(listener);
	}

	private boolean isAlive(TimedNeighbor neighbor) {
		Message m = MessageBuilder.buildLivenessMessage();
		return MessageSender.makeLivenessCheck(new TransferableMessage(m,
				neighbor.getAddress(), false));
	}

	private void sendGossipMessage(Message m, Execution e) {
		InetAddress address;
		PlainNeighborsTable pnt = e.getOutNeighbors();

		// send it to all out-neighbors
		synchronized (pnt) {
			for (PlainNeighbor n : pnt) {
				address = n.getAddress();
				outgoingQueue.add(new TransferableMessage(m, address, false));
			}
		}
	}

	private void checkForNewExecution(Session s, Execution e) {
		// a new Execution can be created only from the initial Execution of the
		// Session and only if the Session has the initiator flag on
		if (s.isInitiator() && e.equals(s.getInitExecution())
				&& s.newExecutionExpected()) {
			Execution newExecution;
			newExecution = s.createNewExecution();

			InetAddress address;
			double valueToSend;
			Message outMessage;
			String sessionId = s.getSessionId();
			String nodeId = localNode.getLocalId().toString();
			PlainNeighborsTable pnt = e.getOutNeighbors();

			// send to all out-neighbors INIT messages for the newly created
			// Execution
			synchronized (pnt) {
				for (PlainNeighbor n : pnt) {
					address = n.getAddress();
					valueToSend = newExecution.getCurrentValue()
							* n.getWeight();
					logger.info("Sending INIT message to node " + n.getId()
							+ " with address " + address + " for execution "
							+ newExecution.getExecutionNumber()
							+ " of session " + sessionId);
					outMessage = MessageBuilder.buildInitMessage(nodeId,
							sessionId, newExecution.getExecutionNumber(),
							s.getNumberOfExecutions(), s.getNumberOfRounds(),
							valueToSend);
					outgoingQueue.add(new TransferableMessage(outMessage,
							address, true));
				}
			}
		}
	}

	private void sendOutNextMessage(MessageType type, Session session,
			Execution execution) {
		InetAddress address;
		double valueToSend;
		Message outMessage;
		String sessionId = session.getSessionId();
		String nodeId = localNode.getLocalId().toString();
		PlainNeighborsTable pnt = execution.getOutNeighbors();

		synchronized (pnt) {
			for (PlainNeighbor n : pnt) {
				address = n.getAddress();
				valueToSend = execution.getCurrentValue() * n.getWeight();
				// round number + 1, because we send the message for a round
				// using the value of the previous round
				int r = execution.getCurrentRound() + 1;
				logger.info("Sending NEXT message to node " + n.getId()
						+ " with address " + address + " for round " + r
						+ " of execution " + execution.getExecutionNumber()
						+ " in session " + sessionId);
				outMessage = MessageBuilder.buildNextMessage(nodeId, sessionId,
						execution.getExecutionNumber(), r, valueToSend);
				outgoingQueue.add(new TransferableMessage(outMessage, address,
						false));
			}
		}
	}

	private void requestPreviousVal(String sessionId, int execNum, int round,
			InetAddress address) {
		String nodeId = localNode.getLocalId().toString();
		Message m = MessageBuilder.requestPreviousValMessage(nodeId, sessionId,
				execNum, round);
		TransferableMessage tm = new TransferableMessage(m, address, false);
		logger.info("Sending REQUEST message to node with address " + address
				+ " for round " + round + " of execution " + execNum
				+ " in session " + sessionId);
		outgoingQueue.add(tm);
	}
}
