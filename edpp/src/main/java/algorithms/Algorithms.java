package algorithms;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import Jama.SingularValueDecomposition;



public class Algorithms {
	
	public static Matrix computeSystemMatrixA(double [] impulseResponses, int networkDiameter) {
		return KungsRealizationAlgorithm.computeSystemMatrixA(impulseResponses, networkDiameter);
	}
	
	public static double [][] systemMatrixAToArray(double [] impulseResponses, int networkDiameter) {
		return KungsRealizationAlgorithm.systemMatrixAToArray(impulseResponses, networkDiameter);
	}
	
	public static double [] computeEigenValues(double [][] matrix) {
		Matrix m = new Matrix(matrix);
		EigenvalueDecomposition ed = new EigenvalueDecomposition(m);
		return ed.getRealEigenvalues();
	}
	
	public static class KungsRealizationAlgorithm {
		
		public static Matrix computeSystemMatrixA(double [] impulseResponses, int networkDiameter) {
			Matrix m, a, u1, sqrts1, g, g1, g2;
			double [] s;
			int dim = impulseResponses.length;
			
			m = convertToHankelMatrix(impulseResponses);
			SingularValueDecomposition svd = new SingularValueDecomposition(m);
			
			if (networkDiameter > dim)
				networkDiameter = dim;
			// Get the first p columns of matrices U and V, where
			// p is the defined order (the network diameter)
			u1 = svd.getU().getMatrix(0, dim - 1, 0, networkDiameter-1);
			s = svd.getSingularValues();
			
			// The roots of the first p singular values will be
			//stored in the diagonal of this matrix
			sqrts1 = new Matrix(networkDiameter, networkDiameter);
			
			//Compute the square root of the singular values submatrix
			for (int i = 0; i < networkDiameter; i++) {
				sqrts1.set(i, i, Math.sqrt(s[i]));
			}
			
			g = u1.times(sqrts1);
			
			//G1 are the first 2n-2 rows of g
			//and G2 the last 2n-2 rows
			g1 = g.getMatrix(0, dim-2, 0, g.getColumnDimension() - 1);
			g2 = g.getMatrix(1 , g.getRowDimension() - 1, 0, g.getColumnDimension() - 1);
			
			//A = [G1^T G1]^-1 G1^T G2
			a = (g1.transpose().times(g1)).inverse().times(g1.transpose().times(g2));
			return a;
		}
		
		public static double[][] systemMatrixAToArray(double [] impulseResponses, int networkDiameter) {
			Matrix m = computeSystemMatrixA(impulseResponses, networkDiameter);
			return m.getArrayCopy();
		}
		
		private static Matrix convertToHankelMatrix(double [] impulseResponsesArray) {
			int dim = impulseResponsesArray.length;
			Matrix m = new Matrix(dim, dim);
			
			//Construct the upper triangular array
			// The first column will contain elements h(1), h(2), ... ,
			// the second h(2), h(3), ... etc 
			for (int i = 0; i < dim; i++) {
				for (int j = 0; j < dim-i; j++){
					m.set(i, j, impulseResponsesArray[j+i]);
				}
			}
			return m;
		}

	}
	
}
