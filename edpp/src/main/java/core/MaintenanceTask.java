package core;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import jdbm.PrimaryTreeMap;

import network.Node;

import util.Phase;
import util.PlainNeighbor;
import util.PlainNeighborsTable;
import util.TimedNeighbor;
import util.TimedNeighborsTable;

import comm.MessageBuilder;
import comm.MessageSender;
import comm.ProtocolMessage.SessionEvent;
import comm.TransferableMessage;
import comm.ProtocolMessage.Message;
import comm.ProtocolMessage.Message.MessageType;
import comm.ProtocolMessage.SessionEvent.EventType;

public class MaintenanceTask implements Runnable {
	
	private  Logger logger;
	
	private Map<String, Session> sessions;
	private BlockingQueue<TransferableMessage> outgoingQueue;
	private Node localNode;
	private PrimaryTreeMap<Integer, RecordedSession> db;
	private static List<SessionListener> sessionListeners = new LinkedList<SessionListener>();

	public MaintenanceTask(Map<String, Session> sessions,
			BlockingQueue<TransferableMessage> outgoingQueue,
			Node localNode,
			PrimaryTreeMap<Integer, RecordedSession> db) {
		logger = Logger.getLogger(MaintenanceTask.class.getName());
		this.sessions = sessions;
		this.outgoingQueue = outgoingQueue;
		this.localNode = localNode;
		this.db = db;
	}

	@Override
	public void run() {
		logger.fine("Running maintenance task");
		Execution e;
		TimedNeighborsTable inNeighbors;
		Collection<Session> sessionSet = sessions.values();
		
		Iterator<Session> sessionIter = sessionSet.iterator();
		Session s;
		while (sessionIter.hasNext()) {
			s = sessionIter.next();
			//Get all executions of the session
			logger.info("Checking session "+s.getSessionId());
			for (int i = 1; i <= s.getCurrentNumberOfExecutions(); i++) {
				e = s.getExecution(i);
				if (e != null){
					logger.info("Checking execution "+e.getExecutionNumber());
					//Check if it is in the INIT phase
					if (e.getPhase() == Phase.INIT) {
						//Check if the INIT phase should end
						if (e.remainingInitTime() <= 0) {
							logger.info("Stage is "+e.getPhase()+" and remaining time is "+e.remainingInitTime());
							/*
							 * Enter DATA_EXCHANGE phase
							 * Go to next round and
							 * send message to all out-neighbors
							 */
							e.setPhase(Phase.DATA_EXCHANGE);
							logger.info("Stage is now "+Phase.DATA_EXCHANGE);
							logger.info("The current round is "+e.getCurrentRound());
							sendOutNextMessage(MessageType.NEXT, s, e);
							e.recomputeWeight();
							e.setRound(2);
							logger.info("Recomputed weights and set round to "+e.getCurrentRound());
						}
					} else if (e.getPhase() == Phase.DATA_EXCHANGE) { /* Check for in-neighbors suspected of failure*/
						inNeighbors = e.getInNeighbors();
						boolean endOfRound = true;
						logger.info("Got in neighbors list and now time to check their times");
						synchronized (inNeighbors) {
							Iterator<TimedNeighbor> iter = inNeighbors.iterator();
//							System.out.println("Making check now");
							while (iter.hasNext()) {
								TimedNeighbor neighbor = iter.next();
								logger.info("Checking neighbor with id "+neighbor.getId().toString());
								long remainingTime = neighbor.getTimeToProbe();
								if (remainingTime != TimedNeighbor.INF)
									neighbor.decreaseTime(ProtocolController.TIMEOUT);
								if (neighbor.getTimeToProbe() <= 0) { /* Check whether the node is alive */
									logger.info("Probing node to see if it is alive");
									if (isAlive(neighbor)) {
										logger.info("Node is still alive");
										inNeighbors.renewTimer(neighbor);
										endOfRound = false;
									} else {
										iter.remove();
									}
								} else if (neighbor.getTimeToProbe() != TimedNeighbor.INF) {
									endOfRound = false;
								}
							}
							System.out.println("Finished with check");
						}
						if (endOfRound) {
							logger.info("Round "+e.getCurrentRound()+" has ended");
							// If it is the end of the round check if we have another round
							if (e.hasAnotherRound()) {
								logger.fine("This has another round");
								sendOutNextMessage(MessageType.NEXT, s, e);
								e.recomputeWeight();
								e.getInNeighbors().renewTimers();
								e.setRound(e.getCurrentRound() + 1);
							} else {
								logger.fine("This was the last round");
								e.setPhase(Phase.GOSSIP);
								logger.info("Time to compute the realization matrix");
								e.computeRealizationMatrix(localNode.getDiameter());
								logger.info("Computed the matrix");
								e.getRealizationMatrix().print();
//								e.getRealizationMatrix().print(NumberFormat.FRACTION_FIELD, 5);
								//compute the eigenvalues of the approximation matrix
								//TODO probably should test this for null
								double [] eigenvals = e.getMatrixAEigenvalues();
								Message msg = MessageBuilder.buildGossipMessage(localNode.getLocalId().toString(),
										s.getSessionId(), e.getExecutionNumber(), eigenvals);
								//send GOSSIP message to out-neighbors
								sendGossipMessage(msg, e);
								logger.info("Sent the message to all the required nodes");
							}
						}
					} else if (e.getPhase() == Phase.GOSSIP) {
						inNeighbors = e.getInNeighbors();
						
						boolean endOfRound = true;
						synchronized (inNeighbors) {
							Iterator<TimedNeighbor> iter = inNeighbors.iterator();
							while (iter.hasNext()) {
								TimedNeighbor neighbor = iter.next();
								long remainingTime = neighbor.getTimeToProbe();
								if (remainingTime != TimedNeighbor.INF)
									neighbor.decreaseTime(ProtocolController.TIMEOUT);
								if (neighbor.getTimeToProbe() <= 0) { /* Check whether the node is alive */
									if (isAlive(neighbor)) {
										inNeighbors.renewTimer(neighbor);
										endOfRound = false;
									} else {
										iter.remove();
									}
								} else if (neighbor.getTimeToProbe() != TimedNeighbor.INF) {
									endOfRound = false;
								}
							}
						}
						if (endOfRound) {
							e.computeMedianEigenvalues();
							e.setPhase(Phase.TERMINATED);
							s.addCompletedExecution();
							if (s.hasTerminated()) {
								System.out.println("Update database");
								sessionIter.remove();
								int size;
								synchronized (db) {
									try {
										size = db.lastKey()+1;
									} catch (NoSuchElementException nse) {
										size = 0;
									}
									RecordedSession recSes = new RecordedSession(s);
									db.put(new Integer(size), recSes);
								}
								
								//Notify all listeners
								for (SessionListener sl : sessionListeners) {
									SessionEvent se = MessageBuilder.buildNewSessionEvent(s, localNode, EventType.TERMINAL);
									sl.sessionCompleted(se);
								}
							}
						}
					}
				}
			}
		}
	}
	
	//TODO must add test
	public static void addSessionListener(SessionListener listener) {
		sessionListeners.add(listener);
	}
	
	//TODO must add test
	public static boolean removeSessionListener(SessionListener listener) {
		return sessionListeners.remove(listener);
	}
	
	private boolean isAlive(TimedNeighbor neighbor) {
		Message m = MessageBuilder.buildLivenessMessage();
		return MessageSender.makeLivenessCheck(
				new TransferableMessage(m, neighbor.getAddress()));
	}

	private void sendGossipMessage(Message m, Execution e) {
		InetAddress address;
		PlainNeighborsTable pnt = e.getOutNeighbors();
		
		synchronized (pnt) {
			for (PlainNeighbor n : pnt) {
				address = n.getAddress();
				outgoingQueue.add(new TransferableMessage(m, address));
			}
		}
	}
	
	private void sendOutNextMessage(MessageType type, Session session, Execution execution) {
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
				//round number + 1, because we send the message for a round
				//using the value of the previous round
				outMessage = MessageBuilder.buildNextMessage(nodeId,
						sessionId, execution.getExecutionNumber(), 
						execution.getCurrentRound() + 1, valueToSend);
				outgoingQueue.add(new TransferableMessage(outMessage, address));
			}
		}
	}

}
