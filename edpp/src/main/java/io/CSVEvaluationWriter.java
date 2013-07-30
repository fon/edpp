package io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

import evaluation.EvaluationResults;

public class CSVEvaluationWriter implements EvaluationWriter {

	static final String EVAL_RESULTS_FILENAME = "evaluation_results.csv";

	private CSVWriter writer;
	
	public CSVEvaluationWriter(String filename, boolean append) throws IOException {
		writer = new CSVWriter(new FileWriter(filename, true), ',');
	}
	
	public CSVEvaluationWriter(String filename) throws IOException {
		this(filename, true);
	}
	
	public CSVEvaluationWriter() throws IOException {
		this(EVAL_RESULTS_FILENAME);
	}
	
	@Override
	public void writeSamplingResults(EvaluationResults er) {
		String [] result = new String[5];
		result[0] = er.getSessionId();
		result[1] = new Double(er.getExpectedMixingTime()).toString();
		result[2] = new Double(er.getExpectedSpectralGap()).toString();
		result[3] = new Double(er.getMixingTimePercentError(10)).toString();
		result[4] = new Double(er.getMixingTimePercentError(90)).toString();
		
		writer.writeNext(result);
	}

	@Override
	public void writeSamplingResults(List<EvaluationResults> er) {
		for (EvaluationResults result : er) {
			this.writeSamplingResults(result);
		}
	}

}
