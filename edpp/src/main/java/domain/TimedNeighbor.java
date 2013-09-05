package domain;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class representing in-neighbors of a node
 * 
 * @author Xenofon Foukas
 * 
 */
public class TimedNeighbor extends Neighbor {

	private volatile AtomicLong remainingTime;

	/**
	 * The INF value set when an in-neighbor has sent a message is actually the
	 * MAX_VALUE of long
	 */
	public static final long INF = Long.MAX_VALUE;
	/**
	 * The default timer used for in-neighbors liveness checks
	 */
	public static final long DEFAULT_TIMER = 5000;

	/**
	 * Constructor class. The remaining time for this node to require a liveness
	 * check is set to DEFAULT_TIMER
	 * 
	 * @param id
	 *            the byte representation of the id of the in-neighbor
	 * @param address
	 *            the InetAddress of the in-neighbor
	 */
	public TimedNeighbor(byte[] id, InetAddress address) {
		super(id, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}

	/**
	 * Constructor class. The remaining time for this node to require a liveness
	 * check is set to DEFAULT_TIMER
	 * 
	 * @param id
	 *            the Id of the in-neighbor
	 * @param address
	 *            the InetAddress of the in-neighbor
	 */
	public TimedNeighbor(Id id, InetAddress address) {
		super(id, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}

	/**
	 * Constructor class. The remaining time for this node to require a liveness
	 * check is set to DEFAULT_TIMER
	 * 
	 * @param stringId
	 *            the string representation of the id of the in-neighbor
	 * @param address
	 *            the InetAddress of the in-neighbor
	 */
	public TimedNeighbor(String stringId, InetAddress address) {
		super(stringId, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}

	/**
	 * Constructor class
	 * 
	 * @param id
	 *            the byte representation of the id of the in-neighbor
	 * @param address
	 *            the InetAddress of the in-neighbor
	 * @param remainingTime
	 *            the time before the node will have to be checked for liveness
	 */
	public TimedNeighbor(byte[] id, InetAddress address, long remainingTime) {
		super(id, address);
		this.remainingTime = new AtomicLong(remainingTime);
	}

	/**
	 * Constructor class
	 * 
	 * @param id
	 *            the Id of the in-neighbor
	 * @param address
	 *            the InetAddress of the in-neighbor
	 * @param remainingTime
	 *            the time before the node will have to be checked for liveness
	 */
	public TimedNeighbor(Id id, InetAddress address, long remainingTime) {
		super(id, address);
		this.remainingTime = new AtomicLong(remainingTime);
	}

	/**
	 * Constructor class
	 * 
	 * @param stringId
	 *            the string representation of the id of the in-neighbor
	 * @param address
	 *            the InetAddress of the in-neighbor
	 * @param remainingTime
	 *            the time before the node will have to be checked for liveness
	 */
	public TimedNeighbor(String stringId, InetAddress address,
			long remainingTime) {
		super(stringId, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}

	/**
	 * 
	 * @return The time remaining until the node needs to be probed for liveness
	 */
	public long getTimeToProbe() {
		return remainingTime.get();
	}

	/**
	 * Set the value for time remaining before a liveness check
	 * 
	 * @param time
	 *            The new remaining time before a liveness check
	 */
	public void setRemainingTime(long time) {
		this.remainingTime.set(time);
	}

	/**
	 * Decreases the time remaining before a liveness check
	 * 
	 * @param time
	 *            Decrease remaining time by a time parameter. The remaining
	 *            time is never negative
	 */
	public void decreaseTime(long time) {
		if (remainingTime.get() - time < 0)
			remainingTime.set(0);
		else
			remainingTime.addAndGet(-time);
	}

	/**
	 * Increases the time remaining before a liveness check
	 * 
	 * @param time
	 *            Increase remaining time by time
	 */
	public void increaseTime(long time) {
		remainingTime.addAndGet(time);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		TimedNeighbor n = (TimedNeighbor) obj;

		return this.id.equals(n.getId());
	}

}