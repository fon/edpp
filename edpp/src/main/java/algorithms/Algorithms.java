package algorithms;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jblas.Singular;
import org.jblas.Solve;

/**
 * Class that contains all the algorithms related to matrix manipulation.
 * 
 * @author Xenofon Foukas
 * 
 */
public class Algorithms {

	/**
	 * Computes the system matrix A, using Kung's realization algorithm
	 * 
	 * @param impulseResponses
	 *            an array of the collected impulse responses
	 * @param networkDiameter
	 *            an approximation of the diameter of the underlying network
	 * @return the realization matrix A of the system in a DoubleMatrix form
	 * @see DoubleMatrix
	 */
	public static DoubleMatrix computeSystemMatrixA(double[] impulseResponses) {
		return KungsRealizationAlgorithm.computeSystemMatrixA(impulseResponses);
	}

	/**
	 * Computes the system matrix A, using Kung's realization algorithm
	 * 
	 * @param impulseResponses
	 *            an array of the collected impulse responses
	 * @param networkDiameter
	 *            an approximation of the diameter of the underlying network
	 * @return the realization matrix A of the system in an array form
	 */
	public static double[][] systemMatrixAToArray(double[] impulseResponses) {
		return KungsRealizationAlgorithm.systemMatrixAToArray(impulseResponses);
	}

	/**
	 * Computes the eigenvalues of a matrix
	 * 
	 * @param matrix
	 *            the matrix that will be analyzed
	 * @return an array of the computed eigenvalues
	 */
	public static double[] computeEigenvaluesModulus(double[][] matrix) {
		DoubleMatrix m = new DoubleMatrix(matrix);
		ComplexDoubleMatrix cdm = Eigen.eigenvalues(m);
		double[] realPart = cdm.real().data;
		double[] imagPart = cdm.imag().data;
		Double[] modulus = new Double[realPart.length];

		// Compute the moduli of the eigenvalues
		for (int i = 0; i < modulus.length; i++) {
			modulus[i] = new Double(Math.sqrt(Math.pow(realPart[i], 2)
					+ Math.pow(imagPart[i], 2)));
		}

		// Sort the moduli from largest to smallest and copy them to a double
		// array
		Arrays.sort(modulus, Collections.reverseOrder());
		double[] vals = new double[modulus.length];
		for (int i = 0; i < modulus.length; i++) {
			vals[i] = modulus[i].doubleValue();
		}
		return vals;
	}

	/**
	 * 
	 * @param matrix
	 *            the matrix that will be analyzed in a DoubleMatrix form
	 * @return an array of the computed eigenvalues
	 */
	public static double[] computeEigenvaluesModulus(DoubleMatrix matrix) {
		ComplexDoubleMatrix cdm = Eigen.eigenvalues(matrix);
		double[] realPart = cdm.real().data;
		double[] imagPart = cdm.imag().data;
		Double[] modulus = new Double[realPart.length];

		// Compute the moduli of the eigenvalues
		for (int i = 0; i < realPart.length; i++) {
			modulus[i] = new Double(Math.sqrt(Math.pow(realPart[i], 2)
					+ Math.pow(imagPart[i], 2)));
		}

		// Sort the moduli from largest to smallest and copy them to a double
		// array
		Set<Double> mySet = new HashSet<Double>(Arrays.asList(modulus));
		modulus = mySet.toArray(new Double[0]);

		Arrays.sort(modulus, Collections.reverseOrder());

		double[] vals = new double[modulus.length];
		for (int i = 0; i < modulus.length; i++) {
			vals[i] = modulus[i].doubleValue();
		}
		return vals;
	}

	/**
	 * Kung's classic realization algorithm
	 * 
	 * @author Xenofon Foukas
	 * 
	 */
	public static class KungsRealizationAlgorithm {

		/**
		 * Computes the system matrix A
		 * 
		 * @param impulseResponses
		 *            an array of the collected impulse responses
		 * @return the realization matrix A of the system in a DoubleMatrix form
		 * @see DoubleMatrix
		 */
		public static DoubleMatrix computeSystemMatrixA(
				double[] impulseResponses) {

			DoubleMatrix H, U, S, u1, O, O1, O2, temp, A;

			// Create the Hankel matrix of the impulse responses and perform
			// singular value decomposition
			H = convertToHankelMatrix(impulseResponses);
			DoubleMatrix[] svd = Singular.fullSVD(H);
			U = svd[0];
			S = svd[1];
			double tol = 1e-7;

			int size = S.length;
			int lastAcceptedIndex = 0;

			// Compute the order of the realization by comparing the singular
			// values to the set threshold
			for (int i = 0; i < size; i++) {
				if (S.get(i) > tol)
					lastAcceptedIndex++;
			}

			DoubleMatrix sqrt_s = DoubleMatrix.zeros(lastAcceptedIndex);

			// sqrt_s = sqrt(S)
			for (int i = 0; i < lastAcceptedIndex; i++) {
				sqrt_s.put(i, Math.sqrt(S.get(i)));
			}

			// u1 are the p first rows of U, where p is the order we found
			// earlier
			u1 = U.getRange(0, U.getRows(), 0, lastAcceptedIndex);
			if (u1.isEmpty()) {
				return new DoubleMatrix(1).put(0, impulseResponses[0]);
			}

			// O = u1*sqrt_s
			O = u1.mmul(DoubleMatrix.diag(sqrt_s));
			// O1 are the first n-1 rows of O and O2 the last n-1
			O1 = O.getRange(0, O.getRows() - 1, 0, O.getColumns());
			O2 = O.getRange(1, O.getRows(), 0, O.getColumns());

			// compute the pseudo-inverse of O1 and multiply it with O2 if O1
			// has elements
			if (O1.length == 0) {
				A = DoubleMatrix.zeros(1);
				A.put(0, 1);
			} else {
				temp = Solve.pinv(O1);
				A = temp.mmul(O2);
			}

			return A;
		}

		/**
		 * Computes the system matrix A
		 * 
		 * @param impulseResponses
		 *            an array of the collected impulse responses
		 * @return the realization matrix A of the system in an array form
		 */
		public static double[][] systemMatrixAToArray(double[] impulseResponses) {
			DoubleMatrix m = computeSystemMatrixA(impulseResponses);
			return m.toArray2();
		}

		private static DoubleMatrix convertToHankelMatrix(
				double[] impulseResponsesArray) {
			int dim = impulseResponsesArray.length;

			// compute the dimensions of the Hankel matrix. it will have to be a
			// square matrix
			int l = (int) Math.floor((dim + 1) / 2);

			DoubleMatrix dm = DoubleMatrix.zeros(l, l);

			/*
			 * create a Hankel matrix of the form:
			 * 
			 * |a b c d| |b c d e| |c d e f| |d e f g|
			 */
			for (int i = 0; i < l; i++) {
				for (int j = 0; j < l; j++) {
					dm.put(i, j, impulseResponsesArray[i + j]);
				}
			}
			return dm;
		}

	}
}
