package evaluation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

public class EvaluationResults {

	private double expectedSpectralGap;
	private double expectedMixingTime;
	
	private List<Double> computedSpectralGaps;
	private List<Double> computedMixingTimes;
	
	public EvaluationResults() {
		this.setExpectedSpectralGap(0);
		this.setExpectedMixingTime(0);
		
		computedSpectralGaps = new ArrayList<Double>();
		computedMixingTimes = new ArrayList<Double>();
	}
	
	public EvaluationResults(double expectedSpectralGap, double expectedMixingTime) {
		this();
		this.expectedMixingTime = expectedMixingTime;
		this.expectedSpectralGap = expectedSpectralGap;
	}
	
	public void addComputedValues(double spectralGap, double mixningTime) {
		this.computedSpectralGaps.add(spectralGap);
		this.computedMixingTimes.add(mixningTime);
	}
	
	public void addComputedSpectralGap(double spectralGap) {
		this.computedSpectralGaps.add(spectralGap);
	}
	
	public void addComputedMixningTime(double mixingTime) {
		this.computedMixingTimes.add(mixingTime);
	}

	public double getExpectedSpectralGap() {
		return expectedSpectralGap;
	}

	public void setExpectedSpectralGap(double expectedSpectralGap) {
		this.expectedSpectralGap = expectedSpectralGap;
	}

	public double getExpectedMixingTime() {
		return expectedMixingTime;
	}

	public void setExpectedMixingTime(double expectedMixingTime) {
		this.expectedMixingTime = expectedMixingTime;
	}
	
	public double getSpectralGapPercentError(int percentile) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (double gap : computedSpectralGaps) {
			stats.addValue(Math.abs((gap - expectedSpectralGap)/expectedSpectralGap)*100);
		}
		return stats.getPercentile(percentile);
	}
	
	public double getMixingTimePercentError(int percentile) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (double mtime : computedMixingTimes) {
			stats.addValue(Math.abs((mtime - expectedMixingTime)/expectedMixingTime)*100);
		}
		return stats.getPercentile(percentile);
	}
	
}
