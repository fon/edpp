package algorithms;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

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
		
		Set<Double> mySet = new HashSet<Double>(Arrays.asList(modulus));
		modulus = mySet.toArray(new Double[0]);
		
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
			
			int size = S.length;
			int lastAcceptedIndex = 0;

			/*double totalVal = 0;
			
			for (int i=0; i < size; i++) {
				totalVal += Math.pow(S.get(i),2);
			}
			
			totalVal = Math.sqrt(totalVal);
			
			double [] bounds = new double[size+1];
			for(int i=0; i<bounds.length; i++) {
				bounds[i] = 0;
				for(int j=i; j<S.length; j++) {
					bounds[i] += Math.pow(S.get(j),2);
				}
				bounds[i] = Math.sqrt(bounds[i]);
				System.out.println(bounds[i]/totalVal);
			}*/
			
			/*boolean indexNotFound = true;
			
			while(indexNotFound) {
				if(2*bounds[lastAcceptedIndex]/totalVal<=tol)
					indexNotFound = false;
				else
					lastAcceptedIndex++;
			}*/
			
			for (int i = 0; i<size; i++) {
				if (S.get(i)>tol)
					lastAcceptedIndex++;
			}
			
			
			DoubleMatrix sqrt_s = DoubleMatrix.zeros(lastAcceptedIndex);
			
			for (int i = 0; i<lastAcceptedIndex; i++) {
				sqrt_s.put(i, Math.sqrt(S.get(i)));
			}
			
			u1 = U.getRange(0, U.getRows(), 0, lastAcceptedIndex);
			if(u1.isEmpty()) {
				return new DoubleMatrix(1).put(0, impulseResponses[0]);
			}
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
//				0.0,0.5465053763440858,0.517245391446988,0.5169518111281476,0.5183325742772139,0.5184833028677822,0.5185760612297974,0.5185973780971941,0.5186050107057781,0.5181426622182144,0.5181121435920955,0.5180936464752279,0.5180899977102099
//				0.0,0.3549059139784946,0.34804229986848195,0.34798522359094686,0.34729160682276317,0.3471463217180659,0.3470878296214105,0.3470719750388306,0.3470665466531915,0.3470649540357114,0.34706444060502034
//				0.0,0.7917827468230693,0.7664622414160284,0.7667700239217252,0.7661160037925737,0.7654998184626965,0.7648835607533185
//				1.0,0.6977761485826001,0.704109122162704,0.7034933196999357,0.7034894758099823,0.7035739159323432,0.7037055619694843,0.7038361432473123,0.7039682232662862
//				0.0,0.6281463831867057,0.6110005029124851,0.6113779308862637,0.6114494692046073,0.6114717056786131,0.6114786879564587,0.6114806666391641,0.6114812726593157,0.611481451095891,0.6114815048560767
//				1.0,0.4426686217008797,0.4778143501637318,0.6069023195115905,0.6054517003971708,0.5372656429364207,0.5661866540862691,0.499045929278503,0.5238494020911878,0.5198348750591154,0.5236344152286981,0.49273824904287405,0.4810126449423332
//				0.0,0.6634347507331377,0.6421097659501075,0.6428562480508309,0.6431541526573631,0.6434883367418877,0.6438216671362165,0.6441557844259062,0.6444900651803684,0.6448245311691924,0.6447921653675527,0.6441083762490238,0.6444655298559601
//				0.0,0.5958883186705767,0.6493802584170507,0.6179724091201197,0.6193031260937503,0.6103939019687847,0.5894434113275091,0.570584456812339,0.5618022120077546,0.5542485718211899,0.5484666896120297,0.5429707783505362,0.5233439545608144,0.4994983568496602,0.5071373948406068,0.5013923521689972,0.4872493000870609,0.47956499639575684,0.48381864334025654,0.4787017428356928,0.4735461005258328,0.4688137645864388,0.44679572206353657,0.4427734700145006,0.4378784700849641,0.43350863212154744,0.4288056199789515,0.4163211086020731,0.4121476721354593,0.40802658838496947,0.4039360654872832,0.39988689963462265,0.3958781956240593,0.39163551154950055,0.38772937342295233,0.37458303763368167,0.3753230193134608,0.3527390219379083,0.35935054544701023,0.35556660475139384,0.3520152274651115,0.3484870545951739,0.35072369264393194,0.34068871104704074,0.3374975578191201,0.33464980204605144,0.32970409806662926,0.3319173564164396,0.3141837140718906,0.31728178024492054
				0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0
		};
		DoubleMatrix dm = Algorithms.KungsRealizationAlgorithm.computeSystemMatrixA(impulseResponses);
		dm.print();
		double [] eigs = Algorithms.computeEigenvaluesModulus(dm);
		for(int i=0; i<eigs.length; i++) {
			System.out.println(eigs[i]);
		}
	}
	
}
