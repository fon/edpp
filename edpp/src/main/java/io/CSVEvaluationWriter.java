package io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;

import evaluation.EvaluationResults;

public class CSVEvaluationWriter implements EvaluationWriter {

	static final String EVAL_RESULTS_FILENAME = "evaluation_results.csv";

	private CSVWriter writer;
	private boolean writerIsOpen;
	
	public CSVEvaluationWriter(String filename, boolean append) throws IOException {
		writer = new CSVWriter(new FileWriter(filename, true), ',');
		writerIsOpen = true;
	}
	
	public CSVEvaluationWriter(String filename) throws IOException {
		this(filename, true);
	}
	
	public CSVEvaluationWriter() throws IOException {
		this(EVAL_RESULTS_FILENAME);
	}
	
	@Override
	public boolean writeSamplingResults(EvaluationResults er) {
		if(!writerIsOpen)
			return false;
		String [] result = new String[5];
		result[0] = er.getSessionId();
		result[1] = new Double(er.getExpectedMixingTime()).toString();
		result[2] = new Double(er.getExpectedSpectralGap()).toString();
		result[3] = new Double(er.getMixingTimePercentError(10)).toString();
		result[4] = new Double(er.getMixingTimePercentError(90)).toString();
		
		writer.writeNext(result);
		return true;
	}

	@Override
	public boolean writeSamplingResults(List<EvaluationResults> er) {
		if(!writerIsOpen)
			return false;
		for (EvaluationResults result : er) {
			this.writeSamplingResults(result);
		}
		try {
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	public void closeWriter() {
		try {
			writer.flush();
			writer.close();
			writerIsOpen = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
