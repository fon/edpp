package algorithms;

import static org.junit.Assert.*;

import org.junit.Test;

public class AlgorithmsTest {

	double [] impulseResponses  = {
			 0.0122, 0.0068, 0.0686, 0.1061, 0.0763,
			 -0.0071, -0.0985, -0.1537, -0.1534, -0.1074,
			 -0.0426, 0.0131, 0.0439, 0.0488, 0.0373, 0.0217,
			 0.0103, 0.0055, 0.0050, 0.0051, 0.0033, -0.0007
	};
	
	double [][] expectedMatrixA = { 
			{0.8216, -0.4873, 0.1192, -0.0065},
			{0.4873, 0.7846, 0.1619, 0.1915},
			{-0.1192, 0.1619, 0.7605, -0.4553},
			{-0.0065, -0.1915, 0.4553, 0.3925}
	};

	@Test
	public void computesSystemMatrixACorrectly() {
		double [][] results;
		results = Algorithms.systemMatrixAToArray(impulseResponses, 4);
		int dim = expectedMatrixA.length;
		for (int i = 0; i < dim; i++) {
			assertArrayEquals(expectedMatrixA[i], results[i], 0.05);
		}
		
	}

}
