package domain;

/**
 * Container class for the parameters required for a new sampling request
 * 
 * @author Xenofon Foukas
 * 
 */
public class SamplingParameters {

	private int numberOfExecutions;
	private int numberOfRounds;

	/**
	 * Constructor class
	 * 
	 * @param numberOfExecutions
	 *            number of Executions contained in the Session of the new
	 *            sampling request
	 * @param numberOfRounds
	 *            number of rounds for each Execution of the new sampling
	 *            request
	 */
	public SamplingParameters(int numberOfExecutions, int numberOfRounds) {
		this.setNumberOfExecutions(numberOfExecutions);
		this.setNumberOfRounds(numberOfRounds);
	}

	/**
	 * 
	 * @return the number of Executions defined for the Session of the new
	 *         sampling request
	 */
	public int getNumberOfExecutions() {
		return numberOfExecutions;
	}

	/**
	 * Set a new number of Executions for the Session of the new sampling
	 * request
	 * 
	 * @param numberOfExecutions
	 *            the updated number of Executions for the new Session
	 */
	public void setNumberOfExecutions(int numberOfExecutions) {
		this.numberOfExecutions = numberOfExecutions;
	}

	/**
	 * 
	 * @return the number of rounds defined for each Execution of the new
	 *         sampling request
	 */
	public int getNumberOfRounds() {
		return numberOfRounds;
	}

	/**
	 * Set a new number of rounds for each Execution of the new sampling request
	 * 
	 * @param numberOfRounds
	 *            the updated number of rounds for each Execution
	 */
	public void setNumberOfRounds(int numberOfRounds) {
		this.numberOfRounds = numberOfRounds;
	}

}
