package domain;

import java.net.InetAddress;

/**
 * Class representing a neighbor of some node in the network
 * 
 * @author Xenofon Foukas
 * 
 */
public class Neighbor {

	protected final Id id;
	protected InetAddress address;

	/**
	 * Constructor class
	 * 
	 * @param id
	 *            the byte representation of the id of the neighbor
	 * @param address
	 *            the InetAddress of the host where the neighbor is located
	 */
	public Neighbor(byte[] id, InetAddress address) {
		Id i = new Id(id);
		this.id = i;
		this.address = address;
	}

	/**
	 * Constructor class
	 * 
	 * @param id
	 *            the Id of the neighbor
	 * @param address
	 *            the InetAddress of the host where the neighbor is located
	 */
	public Neighbor(Id id, InetAddress address) {
		this.id = new Id(id);
		this.address = address;
	}

	/**
	 * Constructor class
	 * 
	 * @param stringId
	 *            the string representation of the Id of the neighbor
	 * @param address
	 *            the InetAddress of the host where the neighbor is located
	 */
	public Neighbor(String stringId, InetAddress address) {
		id = new Id(stringId);
		this.address = address;
	}

	/**
	 * 
	 * @return the Id of the neighbor
	 */
	public Id getId() {
		return id;
	}

	/**
	 * 
	 * @return The IP address of the neighbor
	 */
	public synchronized InetAddress getAddress() {
		return address;
	}

	/**
	 * 
	 * @param address
	 *            The IP address of the neighbor
	 */
	public synchronized void setAddress(InetAddress address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return id.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		Neighbor n = (Neighbor) obj;

		return this.id.equals(n.getId());
	}

	@Override
	public int hashCode() {
		return this.id.toString().hashCode();
	}

}
