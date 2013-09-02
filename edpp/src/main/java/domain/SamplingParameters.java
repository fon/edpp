package domain;

public class SamplingParameters {

	private int numberOfExecutions;
	private int numberOfRounds;
	
	public SamplingParameters(int numberOfExecutions, int numberOfRounds) {
		this.setNumberOfExecutions(numberOfExecutions);
		this.setNumberOfRounds(numberOfRounds);
	}

	public int getNumberOfExecutions() {
		return numberOfExecutions;
	}

	public void setNumberOfExecutions(int numberOfExecutions) {
		this.numberOfExecutions = numberOfExecutions;
	}

	public int getNumberOfRounds() {
		return numberOfRounds;
	}

	public void setNumberOfRounds(int numberOfRounds) {
		this.numberOfRounds = numberOfRounds;
	}
	
	
}
