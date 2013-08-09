package algorithms;

import java.util.Arrays;
import java.util.Collections;
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
	public static DoubleMatrix computeSystemMatrixA(double [] impulseResponses) {
		return KungsRealizationAlgorithm.computeSystemMatrixA(impulseResponses);
	}
	
	/**
	 * Computes the system matrix A, using Kung's realization algorithm
	 * @param impulseResponses an array of the collected impulse responses
	 * @param networkDiameter an approximation of the diameter of the underlying network
	 * @return the realization matrix A of the system in an array form
	 */
	public static double [][] systemMatrixAToArray(double [] impulseResponses) {
		return KungsRealizationAlgorithm.systemMatrixAToArray(impulseResponses);
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
		for (int i = 0; i < modulus.length; i++) {
			modulus[i] = new Double(Math.sqrt(Math.pow(realPart[i],2)+Math.pow(imagPart[i], 2)));
		}
		Arrays.sort(modulus, Collections.reverseOrder());
		double [] vals = new double[modulus.length];
		for(int i=0; i< modulus.length; i++) {
			vals[i] = modulus[i].doubleValue();
		}
		return vals;
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
		for (int i = 0; i < realPart.length; i++) {
			modulus[i] = new Double(Math.sqrt(Math.pow(realPart[i],2)+Math.pow(imagPart[i], 2)));
		}
		Arrays.sort(modulus, Collections.reverseOrder());
		double [] vals = new double[modulus.length];
		for(int i=0; i< modulus.length; i++) {
			vals[i] = modulus[i].doubleValue();
		}
		return vals;
	}
	
	/**
	 * Kung's classic realization algorithm
	 * @author Xenofon Foukas
	 *
	 */
	public static class KungsRealizationAlgorithm {
		
		
		/*public static DoubleMatrix computeSystemMatrixA2(double [] impulseResponses) {
		 
			DoubleMatrix U, S, u, u1, u2, ss, invss, temp;
			
			double [] d = {impulseResponses[0]};
			double [] rest = new double[impulseResponses.length-1];
			for (int i = 0; i<impulseResponses.length-1; i++) {
				rest[i] = impulseResponses[i+1];
			}
			DoubleMatrix m = convertToHankelMatrix2(rest);
			DoubleMatrix [] svd = Singular.fullSVD(m);
			U = svd[0];
			S = svd[1];
			int size = S.length;
			double [] s = S.data;
			double tol = 0.01*S.get(0);
			
			double [] bounds = new double[size+1];
			for(int i=0; i<bounds.length; i++) {
				bounds[i] = 0;
				for(int j=i; j<s.length; j++) {
					bounds[i] += s[j];
				}
			}
			
			int firstProperIndex = 0;
			boolean indexNotFound = true;
			
			while(indexNotFound) {
				if(2*bounds[firstProperIndex]<=tol)
					indexNotFound = false;
				else
					firstProperIndex++;
			}
			u1 = U.getRange(0, m.getRows()-1, 0, firstProperIndex);
			u2 = U.getRange(1, m.getRows(), 0, firstProperIndex);
			if (u1.getColumns() == 0 || u2.getColumns() == 0 || u1.getRows() == 0 || u2.getRows() == 0 )
				return new DoubleMatrix(d);
			ss = org.jblas.MatrixFunctions.sqrt(S.getRange(0, firstProperIndex));
			invss = DoubleMatrix.ones(ss.length);
			invss = invss.divi(ss);
			u = u1.transpose().mmul(u2);
			temp  = invss.mmul(ss.transpose());
			return u.mul(temp);
			
		}*/
		
		
		/**
		 * Computes the system matrix A
		 * @param impulseResponses an array of the collected impulse responses
		 * @return the realization matrix A of the system in a DoubleMatrix form
		 * @see DoubleMatrix
		 */
		public static DoubleMatrix computeSystemMatrixA(double [] impulseResponses) {
			 
			DoubleMatrix H, U, S, u1, O, O1, O2, temp, A;
			
			H = convertToHankelMatrix(impulseResponses);
			DoubleMatrix [] svd = Singular.fullSVD(H);
			U = svd[0];
			S = svd[1];
			double tol = 1e-7;
			
			int lastAcceptedIndex = 0;

			for (int i = 0; i<S.length; i++) {
				if (S.get(i)>tol)
					lastAcceptedIndex++;
			}

			DoubleMatrix sqrt_s = DoubleMatrix.zeros(lastAcceptedIndex);
			
			for (int i = 0; i<lastAcceptedIndex; i++) {
				sqrt_s.put(i, Math.sqrt(S.get(i)));
			}
			
			u1 = U.getRange(0, U.getRows(), 0, lastAcceptedIndex);
			O = u1.mmul(DoubleMatrix.diag(sqrt_s));
			O1 = O.getRange(0,O.getRows()-1,0,O.getColumns());
			O2 = O.getRange(1, O.getRows(), 0, O.getColumns());
			
			temp = Solve.pinv(O1);
			A = temp.mmul(O2);
			
			return A;
		}
		
		/**
		 * Computes the system matrix A
		 * @param impulseResponses an array of the collected impulse responses
		 * @return the realization matrix A of the system in an array form
		 */ 
		public static double[][] systemMatrixAToArray(double [] impulseResponses) {
			DoubleMatrix m = computeSystemMatrixA(impulseResponses);
			return m.toArray2();
		}
		
		/*private static DoubleMatrix convertToHankelMatrix2(double [] impulseResponsesArray) {
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
		}*/
		
		private static DoubleMatrix convertToHankelMatrix(double [] impulseResponsesArray) {
			int dim = impulseResponsesArray.length;
			
			int l = (int)Math.floor((dim+1)/2);

			DoubleMatrix dm = DoubleMatrix.zeros(l, l);

			for (int i = 0; i < l; i++) {
				for (int j = 0; j < l; j++) {
					dm.put(i, j, impulseResponsesArray[i+j]);
				}
			}
			return dm;
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
	
	public static void main(String args []) {
		double [] impulseResponses  = {
				0.00000,   0.52632,   0.49861,   0.50007,   0.50000,   0.50000,  0.5000

		};
		DoubleMatrix dm = Algorithms.KungsRealizationAlgorithm.computeSystemMatrixA(impulseResponses);
		dm.print();
		double [] eigs = Algorithms.computeEigenvaluesModulus(dm);
		for(int i=0; i<eigs.length; i++) {
			System.out.println(eigs[i]);
		}
	}
	
}
