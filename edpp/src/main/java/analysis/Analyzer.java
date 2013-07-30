package analysis;

import java.util.Arrays;
import java.util.Collections;

/**
 * Class that provides methods for manipulating and performing computations on eigenvalues
 * @author Xenofon Foukas
 *
 */
public class Analyzer {

	/**
	 * Computes the spectral gap from a given array of eigenvalues, where the largest eigenvalue is 1
	 * @param eigenvalues an array of unsorted eigenvalues
	 * @return the spectral gap computed by the given eigenvalues
	 */
	public static double computeSpectralGap(double [] eigenvalues) {
		double [] sortedEigenvals = sortEigenvalues(eigenvalues);
		if (sortedEigenvals.length<2)
			return -1;
		double secondEigenval = sortedEigenvals[1];
		return 1 - Math.abs(secondEigenval);
	}
	
	/**
	 * Computes the mixing time of the eigenvalues derived from a network-related matrix
	 * @param eigenvalues an array of unsorted eigenvalues
	 * @param error the accepted error
	 * @return the mixing time derived by the given eigenvalues
	 */
	public static double computeMixingTime(double [] eigenvalues, double error) {
		double [] sortedEigenvals = sortEigenvalues(eigenvalues);
		if (sortedEigenvals.length<2)
			return -1;
		double secondEigenval = sortedEigenvals[1];
		return Math.log(error) / Math.log(Math.abs(secondEigenval));
	}
	
	/**
	 * Sorts the given eigenvalues in descending order
	 * @param eigenvalues an array of eigenvalues
	 * @return an array with sorted eigenvalues
	 */
	public static double [] sortEigenvalues(double [] eigenvalues) {
		Double [] vals = new Double[eigenvalues.length];
		for (int i = 0; i < eigenvalues.length; i++) {
			vals[i] = new Double(eigenvalues[i]);
		}
		Arrays.sort(vals, Collections.reverseOrder());
		double [] sortedEigenvals = new double[vals.length];
		for (int i = 0; i < vals.length; i++) {
			sortedEigenvals[i] = vals[i];
		}
		return sortedEigenvals;
	}
	
}
