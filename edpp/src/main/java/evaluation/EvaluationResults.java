package evaluation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * This class is responsible for storing all the estimations provided by the
 * nodes participating in the evaluation as well as the expected values for the
 * mixing time and spectral gap
 * 
 * @author Xenofon Foukas
 * 
 */
public class EvaluationResults {

	private double expectedSpectralGap;
	private double expectedMixingTime;

	private List<Double> computedSpectralGaps;
	private List<Double> computedMixingTimes;

	private String sessionId;

	/**
	 * Constructor class
	 * 
	 * @param sessionId
	 *            the string representation of the Session over which the
	 *            evaluation is performed
	 */
	public EvaluationResults(String sessionId) {
		this.setExpectedSpectralGap(0);
		this.setExpectedMixingTime(0);
		this.setSessionId(sessionId);

		computedSpectralGaps = new ArrayList<Double>();
		computedMixingTimes = new ArrayList<Double>();
	}

	/**
	 * Constructor class
	 * 
	 * @param sessionId
	 *            the string representation of the Session over which the
	 *            evaluation is performed
	 * @param expectedSpectralGap
	 *            the expected spectral gap of the network once the evaluation
	 *            terminates
	 * @param expectedMixingTime
	 *            the expected mixing time of the network once the evaluation
	 *            terminates
	 */
	public EvaluationResults(String sessionId, double expectedSpectralGap,
			double expectedMixingTime) {
		this(sessionId);
		this.expectedMixingTime = expectedMixingTime;
		this.expectedSpectralGap = expectedSpectralGap;
	}

	/**
	 * This method adds a new estimation provided by some node in the network to
	 * the set of currently proposed values
	 * 
	 * @param spectralGap
	 *            the estimation of some node for the spectral gap of the
	 *            network
	 * @param mixningTime
	 *            the estimation of some node for the mixing time of the network
	 */
	public void addComputedValues(double spectralGap, double mixningTime) {
		this.computedSpectralGaps.add(spectralGap);
		this.computedMixingTimes.add(mixningTime);
	}

	/**
	 * This method adds a new estimation of the spectral gap provided by some
	 * node in the network to the set of the currently proposed values
	 * 
	 * @param spectralGap
	 *            the estimation of some node for the spectral gap of the
	 *            network
	 */
	public void addComputedSpectralGap(double spectralGap) {
		this.computedSpectralGaps.add(spectralGap);
	}

	/**
	 * This method adds a new estimation of the mixing time provided by some
	 * node in the network to the set of the currently proposed values
	 * 
	 * @param mixingTime
	 *            the estimation of some node for the mixing time of the network
	 */
	public void addComputedMixningTime(double mixingTime) {
		this.computedMixingTimes.add(mixingTime);
	}

	/**
	 * This method returns the expected spectral gap of the network
	 * 
	 * @return The expected spectral gap
	 */
	public double getExpectedSpectralGap() {
		return expectedSpectralGap;
	}

	/**
	 * Setter method for the expected spectral gap
	 * 
	 * @param expectedSpectralGap
	 *            the new value of the expected spectral gap
	 */
	public void setExpectedSpectralGap(double expectedSpectralGap) {
		this.expectedSpectralGap = expectedSpectralGap;
	}

	/**
	 * This method returns the expected mixing time of the network
	 * 
	 * @return The expected mixing time
	 */
	public double getExpectedMixingTime() {
		return expectedMixingTime;
	}

	/**
	 * Setter method for the expected mixing time
	 * 
	 * @param expectedMixingTime
	 *            the new value of the expected mixing time
	 */
	public void setExpectedMixingTime(double expectedMixingTime) {
		this.expectedMixingTime = expectedMixingTime;
	}

	/**
	 * This method returns the requested percentile of the proposed spectral gap
	 * estimations
	 * 
	 * @param percentile
	 *            the percentile of estimations to be returned
	 * @return the spectral gap percent error of the requested percentile
	 */
	public double getSpectralGapPercentError(int percentile) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (double gap : computedSpectralGaps) {
			stats.addValue(Math.abs((gap - expectedSpectralGap)
					/ expectedSpectralGap) * 100);
		}
		return stats.getPercentile(percentile);
	}

	/**
	 * This method returns the requested percentile of the proposed mixing time
	 * estimations
	 * 
	 * @param percentile
	 *            the percentile of the estimations to be returned
	 * @return the mixing time percent error of the requested percentile
	 */
	public double getMixingTimePercentError(int percentile) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (double mtime : computedMixingTimes) {
			stats.addValue(Math.abs((mtime - expectedMixingTime)
					/ expectedMixingTime) * 100);
		}
		return stats.getPercentile(percentile);
	}

	/**
	 * 
	 * @return the number of estimations currently stored
	 */
	public int getSampleSize() {
		return computedSpectralGaps.size();
	}

	/**
	 * 
	 * @return the id of the Session that is being evaluated
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * This method sets the id of the Session currently under evaluation
	 * 
	 * @param sessionId
	 *            the id of the evaluated Session
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

}
