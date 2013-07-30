package evaluation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class EvaluationResultsTest {

	double [] spectralGaps = { 0.9, 0.5, 0.7, 0.1, 0.3, 1, 0.2, 0.4, 0.6, 0.8};
	double [] mixingTimes = { 0.9, 0.5, 0.7, 0.1, 0.3, 1, 0.2, 0.4, 0.6, 0.8};
	EvaluationResults eval;
	
	@Before
	public void setUp() throws Exception {
		eval = new EvaluationResults("testId", 0.5, 0.1);
		for (int i = 0; i < spectralGaps.length; i++) {
			eval.addComputedMixningTime(mixingTimes[i]);
			eval.addComputedSpectralGap(spectralGaps[i]);
		}
	}

	@Test
	public void checkSpectralGapPercentError() {
		assertEquals(50, eval.getSpectralGapPercentError(50),0);
	}

	@Test
	public void checkMixingTimePercentError() {
		assertEquals(900, eval.getMixingTimePercentError(100),0);
	}
}
