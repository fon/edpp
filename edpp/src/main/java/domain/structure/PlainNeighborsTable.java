package domain.structure;

import domain.Id;
import domain.Neighbor;
import domain.PlainNeighbor;

/**
 * Interface defining methods for managing PlainNeighbor objects
 * 
 * @author Xenofon Foukas
 * 
 */
public interface PlainNeighborsTable extends NeighborsTable<PlainNeighbor> {

	/**
	 * 
	 * @param node
	 *            the Neighbor whose transition probability we want to find
	 * @return the transition probability of the node if it exists, otherwise -1
	 */
	public double getWeight(Neighbor node);

	/**
	 * 
	 * @param nodeId
	 *            the id of the Neighbor whose transition probability we want to
	 *            find
	 * @return the transition probability of the node if it exists, otherwise -1
	 */
	public double getWeight(Id nodeId);

	/**
	 * 
	 * @param nodeId
	 *            the string representation of the id of the Neighbor whose
	 *            transition probability we want to find
	 * @return the transition probability of the node if it exists, otherwise -1
	 */
	public double getWeight(String nodeId);

	/**
	 * Updates the transition probability of a Neighbor in the table
	 * 
	 * @param node
	 *            the Neighbor whose transition probability will be updated
	 * @param weight
	 *            the new transition probability
	 * @return true if the transition probability was updated successfully,
	 *         false if the Neighbor did not exist
	 */
	public boolean setWeight(Neighbor node, double weight);

	/**
	 * 
	 * @param nodeId
	 *            the id of the Neighbor whose transition probability will be
	 *            updated
	 * @param weight
	 *            the new transition probability
	 * @return true if the transition probability was updated successfully,
	 *         false if the Neighbor did not exist
	 */
	public boolean setWeight(Id nodeId, double weight);

	/**
	 * 
	 * @param nodeId
	 *            the string representation of the id of the Neighbor whose
	 *            transition probability will be updated
	 * @param weight
	 *            the new transition probability
	 * @return true if the transition probability was updated successfully,
	 *         false if the Neighbor did not exist
	 */
	public boolean setWeight(String nodeId, double weight);
}
