package analysis;

/**
 * Class that provides methods for manipulating and performing computations on eigenvalues
 * @author Xenofon Foukas
 *
 */
public class Analyzer {

	/**
	 * Computes the spectral gap from a given array of eigenvalues, where the largest eigenvalue is 1
	 * @param eigenvalues an array of eigenvalues sorted by modulus
	 * @return the spectral gap computed by the given eigenvalues
	 */
	public static double computeSpectralGap(double [] eigenvalues) {
		if (eigenvalues.length<2)
			return -1;
		double secondEigenval = eigenvalues[1];
		return 1 - Math.abs(secondEigenval);
	}
	
	public static double computeSpectralGap2(double [] eigenvalues) {
		if (eigenvalues.length<2)
			return -1;
		double secondEigenval = eigenvalues[1];
		return Math.abs(eigenvalues[0]) - Math.abs(secondEigenval);
	}
	
	/**
	 * Computes the mixing time of the eigenvalues derived from a network-related matrix
	 * @param eigenvalues an array of unsorted eigenvalues
	 * @param error the accepted error
	 * @return the mixing time derived by the given eigenvalues
	 */
	public static double computeMixingTime(double [] eigenvalues, double error) {
		if (eigenvalues.length<2)
			return -1;
		double secondEigenval = eigenvalues[1];
		return Math.log(error) / Math.log(Math.abs(secondEigenval));
	}
	
}
