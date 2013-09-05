package domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * Class responsible for storing all the proposed estimations of the Gossip
 * Round for an Execution and for computing their median
 * 
 * @author Xenofon Foukas
 * 
 */
public class GossipData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1496225759246739361L;

	Map<String, double[]> collectedEigenvalues;

	/**
	 * Constructor class. Creates a GossipData structure without any estimations
	 * stored
	 */
	public GossipData() {
		collectedEigenvalues = new ConcurrentHashMap<String, double[]>();
	}

	/**
	 * This method adds a new proposal to the list of estimations. If the node
	 * with id nodeId has already made a proposal, the new proposed value will
	 * replace the previous
	 * 
	 * @param nodeId
	 *            the string representation of the proposing node's id
	 * @param fs
	 *            a double array containing the eigenvalue estimations of the
	 *            remote node
	 */
	public void setNewProposal(String nodeId, double[] fs) {
		collectedEigenvalues.put(nodeId, fs);
	}

	/**
	 * This method retrieves the proposals of the remote node with id nodeId
	 * 
	 * @param nodeId
	 *            the string representation of the proposing node's id
	 * @return a double array containing the eigenvalues proposed by the node
	 *         with id nodeId
	 */
	public double[] getProposal(String nodeId) {
		return collectedEigenvalues.get(nodeId);
	}

	/**
	 * This method computes the median of all the proposed eigenvalues
	 * 
	 * @return a double array containing the median eigenvalues of all
	 *         eigenvalues proposed. The length of this array will be equal to
	 *         the length of the proposal with the least eigenvalues
	 */
	public double[] computeMedianOfProposedValues() {
		Collection<double[]> values = collectedEigenvalues.values();

		int rows = values.size();
		int columns = Integer.MAX_VALUE;

		// set the length of the result equal to the length of the proposal with
		// the least eigenvalues
		for (double[] vals : values) {
			if (vals.length < columns)
				columns = vals.length;
		}

		// create a double matrix containing all the proposals.
		// each row will contain the eigenvalues proposed by one node in
		// decreasing magnitude order
		double[][] proposals = new double[rows][columns];
		int i = 0;
		for (double[] vals : values) {
			for (int j = 0; j < columns; j++) {
				proposals[i][j] = vals[j];
			}
			i++;
		}
		double[] finalValues = new double[columns];

		// for each column of the matrix get the median, i.e. for column j find
		// the median of all proposed eigenvalues placed jth by a node when
		// ordered
		for (int j = 0; j < proposals[0].length; j++) {
			double items[] = new double[proposals.length];
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
