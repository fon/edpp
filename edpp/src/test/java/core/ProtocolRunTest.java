package core;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProtocolRunTest {

	double [] previousEigenvalues = {
			0.8, 0.6, 0.4, 0.2
	};
	
	double [] newEigenvaluesV1 = {
			0.842, 0.623, 0.4, 0.234
	};
	
	double [] newEigenvaluesV2 = {
			0.9, 0.8, 0.4, 0.33
	};
	
	@Test
	public void thresholdIsAdjustedProperly() {
		long initThreshold = ProtocolRun.CURRENT_THRESHOLD;
		double initIncreaseRate = ProtocolRun.CURRENT_INCREASE_RATE;
		double initDecreaseRate = ProtocolRun.CURRENT_DECREASE_RATE;
		
		ProtocolRun.adjustThreshold(previousEigenvalues, previousEigenvalues);
		long expectedThresholdIncrease = initThreshold + (long)(initThreshold*initIncreaseRate);
		double expectedIncreaseRate = initDecreaseRate+0.05;
		assertEquals(expectedThresholdIncrease, ProtocolRun.CURRENT_THRESHOLD);
		assertEquals(expectedIncreaseRate, ProtocolRun.CURRENT_INCREASE_RATE, 0.0);
		assertEquals(ProtocolRun.MIN_RATE, ProtocolRun.CURRENT_DECREASE_RATE, 0.0);
		
		ProtocolRun.adjustThreshold(previousEigenvalues, newEigenvaluesV1);
		expectedThresholdIncrease = expectedThresholdIncrease + (long)(expectedThresholdIncrease*expectedIncreaseRate);
		expectedIncreaseRate = expectedIncreaseRate + 0.05;
		assertEquals(expectedThresholdIncrease, ProtocolRun.CURRENT_THRESHOLD);
		assertEquals(expectedIncreaseRate, ProtocolRun.CURRENT_INCREASE_RATE, 0.0);
		assertEquals(ProtocolRun.MIN_RATE, ProtocolRun.CURRENT_DECREASE_RATE, 0.0);
		
		ProtocolRun.adjustThreshold(previousEigenvalues, newEigenvaluesV2);
		expectedThresholdIncrease = expectedThresholdIncrease - (long)(expectedThresholdIncrease*initDecreaseRate);
		expectedIncreaseRate = ProtocolRun.MIN_RATE;
		double expectedDecreaseRate = initDecreaseRate + 0.05;
		assertEquals(expectedThresholdIncrease, ProtocolRun.CURRENT_THRESHOLD);
		assertEquals(expectedIncreaseRate, ProtocolRun.CURRENT_INCREASE_RATE, 0.0);
		assertEquals(expectedDecreaseRate, ProtocolRun.CURRENT_DECREASE_RATE, 0.0);
	}
	
	@Test
	public void thresholdDoesNotDecreaseUnderTheMinimumValue() {
		ProtocolRun.CURRENT_THRESHOLD = ProtocolRun.MIN_TIME_THRESHOLD;
		ProtocolRun.adjustThreshold(previousEigenvalues, newEigenvaluesV2);
		assertEquals(ProtocolRun.MIN_TIME_THRESHOLD, ProtocolRun.CURRENT_THRESHOLD);
	}
	
	@Test
	public void thresholdDoesNotIncreaseOverTheMaximumValue() {
		ProtocolRun.CURRENT_THRESHOLD = ProtocolRun.MAX_TIME_THRESHOLD;
		ProtocolRun.adjustThreshold(previousEigenvalues, newEigenvaluesV1);
		assertEquals(ProtocolRun.MAX_TIME_THRESHOLD, ProtocolRun.CURRENT_THRESHOLD);
	}
	
	@Test
	public void increaseRateDoesNotIncreaseOverTheMaximumValue() {
		ProtocolRun.CURRENT_INCREASE_RATE = ProtocolRun.MAX_RATE;
		ProtocolRun.adjustThreshold(previousEigenvalues, newEigenvaluesV1);
		assertEquals(ProtocolRun.MAX_RATE, ProtocolRun.CURRENT_INCREASE_RATE, 0.0);
	}
	
	@Test
	public void decreaseRateDoesNotIncreaseOverTheMaximumValue() {
		ProtocolRun.CURRENT_DECREASE_RATE = ProtocolRun.MAX_RATE;
		ProtocolRun.adjustThreshold(previousEigenvalues, newEigenvaluesV2);
		assertEquals(ProtocolRun.MAX_RATE, ProtocolRun.CURRENT_DECREASE_RATE, 0.0);
	}

}
