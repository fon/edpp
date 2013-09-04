package domain.structure;

import domain.Id;

/**
 * Interface defining methods for managing a table storing neighbors of type T
 * 
 * @author Xenofon Foukas
 * 
 * @param <T>
 *            The specific type of neighbor stored in this table. This should be
 *            either PlainNeighbor or TimedNeighbor
 */
public interface NeighborsTable<T> extends Iterable<T> {

	/**
	 * 
	 * @param nodeId
	 *            the Id of the neighbor to be returned
	 * @return returns the neighbor with the given Id if it exists, otherwise
	 *         null
	 */
	public T getNeighbor(Id nodeId);

	/**
	 * 
	 * @param stringId
	 *            the string representation of the Id of the neighbor
	 * @return returns the neighbor with the given Id if it exists, otherwise
	 *         null
	 */
	public T getNeighbor(String stringId);

	/**
	 * Adds a neighbor to the table if it not currently present
	 * 
	 * @param node
	 *            the neighbor to be added to this table
	 * @return true if the neighbor was successfully added, otherwise false
	 */
	public boolean addNeighbor(T node);

	/**
	 * 
	 * @param nodeId
	 *            the Id of the neighbor to be removed
	 * @return true if the neighbor existed and was removed, otherwise false
	 */
	public boolean removeNeighbor(Id nodeId);

	/**
	 * 
	 * @param node
	 *            the neighbor to be removed from this table
	 * @return true if the neighbor existed and was removed, otherwise false
	 */
	public boolean removeNeighbor(T node);

	/**
	 * Converts this table to an array of type T
	 * 
	 * @return an array of type T containing all the neighbors of this table
	 */
	public T[] toArray();

	/**
	 * 
	 * @return the number of neighbors currently stored in the table
	 */
	public int getSize();

}
