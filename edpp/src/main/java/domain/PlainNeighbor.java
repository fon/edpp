package domain;

import java.net.InetAddress;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * Class representing out-neighbors of a node
 * 
 * @author Xenofon Foukas
 * 
 */
public class PlainNeighbor extends Neighbor {

	private AtomicDouble weight;

	/**
	 * Constructor class
	 * 
	 * @param id
	 *            the byte representation of the id of the out-neighbor
	 * @param address
	 *            the InetAddress of the out-neighbor
	 * @param weight
	 *            the transition probability of going from the local node the
	 *            this out-neighbor
	 */
	public PlainNeighbor(byte[] id, InetAddress address, double weight) {
		super(id, address);
		this.weight = new AtomicDouble(weight);
	}

	/**
	 * Constructor class
	 * 
	 * @param id
	 *            the Id of the out-neighbor
	 * @param address
	 *            the InetAddress of the out-neighbor
	 * @param weight
	 *            the transition probability of going from the local node the
	 *            this out-neighbor
	 */
	public PlainNeighbor(Id id, InetAddress address, double weight) {
		super(id, address);
		this.weight = new AtomicDouble(weight);
	}

	/**
	 * Constructor class
	 * 
	 * @param id
	 *            the string representation of the id of the out-neighbor
	 * @param address
	 *            the InetAddress of the out-neighbor
	 * @param weight
	 *            the transition probability of going from the local node the
	 *            this out-neighbor
	 */
	public PlainNeighbor(String stringId, InetAddress address, double weight) {
		super(stringId, address);
		this.setWeight(weight);
	}

	/**
	 * 
	 * @return the transition probability of going from the local node to this
	 *         out-neighbor
	 */
	public double getWeight() {
		return weight.get();
	}

	/**
	 * This method changes the transition probability of going from the local
	 * node to this out-neighbor
	 * 
	 * @param weight
	 *            the new transition probability
	 */
	public void setWeight(double weight) {
		this.weight.set(weight);
	}

}
