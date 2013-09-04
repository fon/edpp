package domain.structure;

import domain.Id;
import domain.TimedNeighbor;

/**
 * Interface defining methods for managing TimedNeighbor objects
 * 
 * @author Xenofon Foukas
 * 
 */
public interface TimedNeighborsTable extends NeighborsTable<TimedNeighbor> {

	/**
	 * 
	 * @return an array of TimedNeighbor objects having expired timers
	 */
	public TimedNeighbor[] getExpiredNeighbors();

	/**
	 * 
	 * @return an array of TimedNeighbor objects with timers that are still
	 *         valid
	 */
	public TimedNeighbor[] getValidNeighbors();

	/**
	 * Updates the time in the timer of the TimedNeighbor object
	 * 
	 * @param nodeId
	 *            the Id of the TimedNeighbor
	 * @param time
	 *            the new time of the nodes timer
	 * @return true if the timer was updated successfully, false if the
	 *         TimedNeighbor was not found
	 */
	public boolean setNeighborTimer(Id nodeId, long time);

	/**
	 * Updates the time in the timer of the TimedNeighbor object using a default
	 * value
	 * 
	 * @param node
	 *            the TimedNeighbor whose timer will be updated
	 * @return true if the timer was updated successfully, false if the
	 *         TimedNeighbor was not found
	 */
	public boolean renewTimer(TimedNeighbor node);

	/**
	 * Updates the time in the timer of the TimedNeighbor object using a default
	 * value
	 * 
	 * @param nodeId
	 *            the Id of the TimedNeighbor whose timer will be updated
	 * @return true if the timer was updated successfully, false if the
	 *         TimedNeighbor was not found
	 */
	public boolean renewTimer(Id nodeId);

	/**
	 * Updates the time in the timer of the TimedNeighbor object using a default
	 * value
	 * 
	 * @param nodeId
	 *            the string representation of the Id of the TimedNeighbor whose
	 *            timer will be updated
	 * @return true if the timer was updated successfully, false if the
	 *         TimedNeighbor was not found
	 */
	public boolean renewTimer(String nodeId);

	/**
	 * Method for defining the default value for timer renewals
	 * 
	 * @param time
	 *            the new default time for timer renewals
	 */
	public void setDefaultTimeValue(long time);

	/**
	 * Renews the timers of all the TimedNeighbor objects in the table
	 */
	public void renewTimers();

	/**
	 * Sets the timers of a TimedNeighbor object of the table to INF
	 * 
	 * @param nodeId
	 *            the the string representation of the Id of the TimedNeighbor
	 *            whose timer will be set to INF
	 * @return true if the timer was set to INF, false if the node was not found
	 *         in the table
	 */
	public boolean setTimerToInf(String nodeId);
}
