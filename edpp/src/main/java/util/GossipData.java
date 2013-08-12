package util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

public class GossipData implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1496225759246739361L;
	
	Map<String, double[]> collectedEigenvalues;
	
	public GossipData() {
		collectedEigenvalues = new ConcurrentHashMap<String, double[]>();
	}
	
	public void setNewProposal(String nodeId, double[] fs) {
		collectedEigenvalues.put(nodeId, fs);
	}
	
	public double[] getProposal(String nodeId) {
		return collectedEigenvalues.get(nodeId);
	}
	
	public double[] computeMedianOfProposedValues() {
		Collection<double []> values = collectedEigenvalues.values();
		
		int rows = values.size();
		int columns = Integer.MAX_VALUE;
		
		for (double [] vals : values) {
			if (vals.length < columns)
				columns = vals.length;
		}
		
		double [][] proposals = new double[rows][columns];
		int i=0;
		for (double [] vals : values) {
			for (int j = 0; j< columns; j++) {
				proposals[i][j] = vals[j];
			}
			i++;
		}
		double [] finalValues = new double[columns]; 
		
		for (int j = 0; j < proposals[0].length; j++) {
			double items [] = new double[proposals.length];
			for (i = 0; i < proposals.length; i++) {
				items[i] = Math.abs(proposals[i][j]);
			}
			finalValues[j] = findMedian(items);
		}
		return finalValues;
	}
	
	private double findMedian(double[] items) {
		DescriptiveStatistics stats = new DescriptiveStatistics(items);
		return stats.getPercentile(50);
	}
	
}
