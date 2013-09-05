package domain.network;

import java.util.Set;

import domain.Id;
import domain.Neighbor;

/**
 * Interface for providing an abstract representation of the underlying network
 * to the top layer of the protocol
 * 
 * @author Xenofon Foukas
 * 
 */
public interface Node {

	/**
	 * This method is responsible for providing the higher protocol layer with a
	 * set containing the local node's out-neighbors
	 * 
	 * @return A set containing the local node's out-neighbors
	 */
	public Set<Neighbor> getOutNeighbors();

	/**
	 * This method returns the Id of the local node
	 * 
	 * @return the Id of the local node
	 */
	public Id getLocalId();

	/**
	 * Optional method that provides a hint to the underlying network for a
	 * failed node which should be removed. The network can choose to accept or
	 * ignore this hint
	 * 
	 * @param id
	 *            The id of the failed node in a String format
	 * @return true if the network actually removes the node, otherwise false
	 */
	public boolean removeOutNeighborNode(String id);
}
