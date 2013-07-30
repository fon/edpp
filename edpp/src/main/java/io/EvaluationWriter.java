package io;

import java.util.List;

import evaluation.EvaluationResults;

public interface EvaluationWriter {

	public void writeSamplingResults(EvaluationResults er);
	
	public void writeSamplingResults(List<EvaluationResults> er);
	
}
