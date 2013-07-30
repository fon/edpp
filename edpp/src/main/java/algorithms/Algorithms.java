package algorithms;

import java.util.Arrays;
import java.util.Comparator;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jblas.Singular;
import org.jblas.Solve;




/**
 * Class that contains all the algorithms related to matrix manipulation.
 * @author Xenofon Foukas
 *
 */
public class Algorithms {
	
	/**
	 * Computes the system matrix A, using Kung's realization algorithm
	 * @param impulseResponses an array of the collected impulse responses
	 * @param networkDiameter an approximation of the diameter of the underlying network
	 * @return the realization matrix A of the system in a DoubleMatrix form
	 * @see DoubleMatrix
	 */
	public static DoubleMatrix computeSystemMatrixA(double [] impulseResponses, int networkDiameter) {
		return KungsRealizationAlgorithm.computeSystemMatrixA(impulseResponses, networkDiameter);
	}
	
	/**
	 * Computes the system matrix A, using Kung's realization algorithm
	 * @param impulseResponses an array of the collected impulse responses
	 * @param networkDiameter an approximation of the diameter of the underlying network
	 * @return the realization matrix A of the system in an array form
	 */
	public static double [][] systemMatrixAToArray(double [] impulseResponses, int networkDiameter) {
		return KungsRealizationAlgorithm.systemMatrixAToArray(impulseResponses, networkDiameter);
	}
	
	/**
	 * Computes the eigenvalues of a matrix
	 * @param matrix the matrix that will be analyzed
	 * @return an array of the computed eigenvalues
	 */
	public static double [] computeEigenvaluesModulus(double [][] matrix) {
		DoubleMatrix m = new DoubleMatrix(matrix);
		ComplexDoubleMatrix cdm = Eigen.eigenvalues(m);
		double [] realPart = cdm.real().data;
		double [] imagPart = cdm.imag().data;
		Double [] modulus = new Double[realPart.length];
		double [] eigenvalues = new double[realPart.length];
		for (int i = 0; i < realPart.length; i++) {
			modulus[i] = Math.sqrt(Math.pow(realPart[i],2)+Math.pow(imagPart[i], 2));
		}
		Algorithms a = new Algorithms();
		ArrayIndexComparator comparator = a.new ArrayIndexComparator(modulus);
		Integer[] indexes = comparator.createIndexArray();
		Arrays.sort(indexes, comparator);
		for (int i = 0; i < indexes.length; i++) {
			eigenvalues[i] = realPart[indexes[i]];
		}
		return eigenvalues;
	}
	
	/**
	 * 
	 * @param matrix the matrix that will be analyzed in a DoubleMatrix form
	 * @return an array of the computed eigenvalues
	 */
	public static double [] computeEigenvaluesModulus(DoubleMatrix matrix) {
		ComplexDoubleMatrix cdm = Eigen.eigenvalues(matrix);
		double [] realPart = cdm.real().data;
		double [] imagPart = cdm.imag().data;
		Double [] modulus = new Double[realPart.length];
		double [] eigenvalues = new double[realPart.length];
		for (int i = 0; i < realPart.length; i++) {
			modulus[i] = Math.sqrt(Math.pow(realPart[i],2)+Math.pow(imagPart[i], 2));
		}
		Algorithms a = new Algorithms();
		ArrayIndexComparator comparator = a.new ArrayIndexComparator(modulus);
		Integer[] indexes = comparator.createIndexArray();
		Arrays.sort(indexes, comparator);
		for (int i = 0; i < indexes.length; i++) {
			eigenvalues[i] = realPart[indexes[i]];
		}
		return eigenvalues;
	}
	
	/**
	 * Kung's classic realization algorithm
	 * @author Xenofon Foukas
	 *
	 */
	public static class KungsRealizationAlgorithm {
		
		/**
		 * Computes the system matrix A
		 * @param impulseResponses an array of the collected impulse responses
		 * @param networkDiameter an approximation of the diameter of the underlying network
		 * @return the realization matrix A of the system in a DoubleMatrix form
		 * @see DoubleMatrix
		 */
		public static DoubleMatrix computeSystemMatrixA(double [] impulseResponses, int networkDiameter) {
			DoubleMatrix m, temp, a, u1, sqrts1, g, g1, g2;
			double [] s;
			int dim = impulseResponses.length;
			
			m = convertToHankelMatrix(impulseResponses);

			DoubleMatrix [] svd = Singular.fullSVD(m);
			
			if (networkDiameter > dim)
				networkDiameter = dim;
			// Get the first p columns of matrices U and V, where
			// p is the defined order (the network diameter)
			u1 = svd[0].getRange(0, dim, 0, networkDiameter);
			s = svd[1].toArray();
			
			
			// The roots of the first p singular values will be
			//stored in the diagonal of this matrix
			sqrts1 = DoubleMatrix.zeros(networkDiameter, networkDiameter);
			
			//Compute the square root of the singular values submatrix
			for (int i = 0; i < networkDiameter; i++) {
				sqrts1.put(i, i, Math.sqrt(s[i]));
			}
			
			g = u1.mmul(sqrts1);
			
			//G1 are the first 2n-2 rows of g
			//and G2 the last 2n-2 rows
			g1 = g.getRange(0, dim-1, 0, g.getColumns());
			g2 = g.getRange(1, g.getRows(), 0, g.getColumns());
			//A = [G1^T G1]^-1 G1^T G2
			temp = (g1.transpose().mmul(g1));
			a = Solve.pinv(temp).mmul(g1.transpose().mmul(g2));
			return a;
		}
		
		/**
		 * Computes the system matrix A
		 * @param impulseResponses an array of the collected impulse responses
		 * @param networkDiameter an approximation of the diameter of the underlying network
		 * @return the realization matrix A of the system in an array form
		 */ 
		public static double[][] systemMatrixAToArray(double [] impulseResponses, int networkDiameter) {
			DoubleMatrix m = computeSystemMatrixA(impulseResponses, networkDiameter);
			return m.toArray2();
		}
		
		private static DoubleMatrix convertToHankelMatrix(double [] impulseResponsesArray) {
			int dim = impulseResponsesArray.length;
			
			DoubleMatrix m = DoubleMatrix.zeros(dim, dim);
			
			//Construct the upper triangular array
			// The first column will contain elements h(1), h(2), ... ,
			// the second h(2), h(3), ... etc 
			for (int i = 0; i < dim; i++) {
				for (int j = 0; j < dim-i; j++){
					m.put(i,j, impulseResponsesArray[j+i]);
				}
			}
			return m;
		}

	}

	public class ArrayIndexComparator implements Comparator<Integer> {
		
	    private final Double[] array;

	    public ArrayIndexComparator(Double[] array)
	    {
	        this.array = array;
	    }

	    public Integer[] createIndexArray()
	    {
	        Integer[] indexes = new Integer[array.length];
	        for (int i = 0; i < array.length; i++)
	        {
	            indexes[i] = i; // Autoboxing
	        }
	        return indexes;
	    }

	    @Override
	    public int compare(Integer index1, Integer index2)
	    {
	         // Autounbox from Integer to int to use as array indexes
	        return array[index2].compareTo(array[index1]);
	    }
	}
	
}
