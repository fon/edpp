package algorithms;

import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jblas.Singular;
import org.jblas.Solve;


//import Jama.Matrix;
//import Jama.SingularValueDecomposition;



public class Algorithms {
	
	public static DoubleMatrix computeSystemMatrixA(double [] impulseResponses, int networkDiameter) {
		return KungsRealizationAlgorithm.computeSystemMatrixA(impulseResponses, networkDiameter);
	}
	
	public static double [][] systemMatrixAToArray(double [] impulseResponses, int networkDiameter) {
		return KungsRealizationAlgorithm.systemMatrixAToArray(impulseResponses, networkDiameter);
	}
	
	public static double [] computeEigenvalues(double [][] matrix) {
		DoubleMatrix m = new DoubleMatrix(matrix);
		return Eigen.eigenvalues(m).real().data;
	}
	
	public static double [] computeEigenvalues(DoubleMatrix matrix) {
		return Eigen.eigenvalues(matrix).real().data;
	}
	
	public static class KungsRealizationAlgorithm {
		
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
//			System.out.println(svd[1].toString());
			
			
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
//			a = (g1.transpose().times(g1)).inverse().times(g1.transpose().times(g2));
			return a;
		}
		
		public static double[][] systemMatrixAToArray(double [] impulseResponses, int networkDiameter) {
			DoubleMatrix m = computeSystemMatrixA(impulseResponses, networkDiameter);
			return m.toArray2();
//			return m.getArrayCopy();
		}
		
		private static DoubleMatrix convertToHankelMatrix(double [] impulseResponsesArray) {
			int dim = impulseResponsesArray.length;
			
			DoubleMatrix m = DoubleMatrix.zeros(dim, dim);
			
//			Matrix m = new Matrix(dim, dim);
			
			//Construct the upper triangular array
			// The first column will contain elements h(1), h(2), ... ,
			// the second h(2), h(3), ... etc 
			for (int i = 0; i < dim; i++) {
				for (int j = 0; j < dim-i; j++){
					m.put(i,j, impulseResponsesArray[j+i]);
//					m.set(i, j, impulseResponsesArray[j+i]);
				}
			}
			return m;
		}

	}
	
}
