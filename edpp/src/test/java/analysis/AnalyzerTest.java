package analysis;

import static org.junit.Assert.*;

import org.junit.Test;

public class AnalyzerTest {

	double [] testEigenvalues = {
			0.2, 0.9, 0.7, 0.3, 0.8
	};
	
	double [] sortedEigenvalues = {
			0.9, 0.8, 0.7, 0.3, 0.2
	};
	
	@Test
	public void eigenValsGetSorted() {
		assertArrayEquals(sortedEigenvalues, Analyzer.sortEigenvalues(testEigenvalues), 0.0);
	}
	
	@Test
	public void spectralGapIsComputedCorrectly() {
		assertEquals(0.2, Analyzer.computeSpectralGap(testEigenvalues), 0.001);
		
	}
	
	@Test
	public void mixingTimeIsComputedCorrectrly() {
		assertEquals(41.275404634, Analyzer.computeMixingTime(testEigenvalues, 0.0001), 0.001);
	}

}
