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
	
	public void setNewProposal(String nodeId, double [] eigenvals) {
		collectedEigenvalues.put(nodeId, eigenvals);
	}
	
	public double [] getProposal(String nodeId) {
		return collectedEigenvalues.get(nodeId);
	}
	
	public double [] computeMedianOfProposedValues() {
		Collection<double []> values = collectedEigenvalues.values();
		double [][] proposals = values.toArray(new double[values.size()][]);
		double [] finalValues = new double[proposals[0].length]; 
		
		for (int j = 0; j < proposals[0].length; j++) {
			double items [] = new double[proposals.length];
			for (int i = 0; i < proposals.length; i++) {
				items[i] = proposals[i][j];
			}
			finalValues[j] = findMedian(items);
		}
		return finalValues;
	}
	
	private double findMedian(double [] items) {
		DescriptiveStatistics stats = new DescriptiveStatistics(items);
		return stats.getPercentile(50);
	}
	
}
