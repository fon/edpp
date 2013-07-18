package analysis;

import java.util.Arrays;
import java.util.Collections;

public class Analyzer {

	public static double computeSpectralGap(double [] eigenvalues) {
		double [] sortedEigenvals = sortEigenvalues(eigenvalues);
		if (sortedEigenvals.length<2)
			return -1;
		double secondEigenval = sortedEigenvals[1];
		return 1 - Math.abs(secondEigenval);
	}
	
	public static double computeMixingTime(double [] eigenvalues, double error) {
		double [] sortedEigenvals = sortEigenvalues(eigenvalues);
		if (sortedEigenvals.length<2)
			return -1;
		double secondEigenval = sortedEigenvals[1];
		return Math.log(error) / Math.log(Math.abs(secondEigenval));
	}
	
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
