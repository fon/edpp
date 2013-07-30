package analysis;

import static org.junit.Assert.*;

import org.junit.Test;

public class AnalyzerTest {

	double [] testEigenvalues = {
			0.9, 0.8, 0.7, 0.3, 0.2
	};
	
	double [] testEigenvalues2 = {
			1, -0.8, 0.7, 0.3, 0.2
	};
	
	double [] sortedEigenvalues = {
			0.9, 0.8, 0.7, 0.3, 0.2
	};
	
	double [] sortedEigenvalues2 = {
			0.9, -0.8, 0.7, 0.3, 0.2
	};
	
	@Test
	public void spectralGapIsComputedCorrectly() {
		assertEquals(0.1, Analyzer.computeSpectralGap(testEigenvalues), 0.001);
		assertEquals(0.2, Analyzer.computeSpectralGap(testEigenvalues2), 0.001);
	}
	
	@Test
	public void mixingTimeIsComputedCorrectrly() {
		assertEquals(41.275404634, Analyzer.computeMixingTime(testEigenvalues, 0.0001), 0.001);
	}

}
