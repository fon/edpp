package io;

import java.util.List;

import evaluation.EvaluationResults;

public interface EvaluationWriter {

	public boolean writeSamplingResults(EvaluationResults er);
	
	public boolean writeSamplingResults(List<EvaluationResults> er);
	
}
