package util;

import static org.junit.Assert.*;

import org.junit.Test;

import analysis.Analyzer;

public class GossipDataTest {

	double [] proposer1 = {
			0.4, 0.2, 0.5
	};
	
	double [] proposer2 = {
			0.2, 0.3, 0.6
	};
	
	double [] proposer3 = {
			0.5, 0.9, 0.2
	};
	
	double [] expectedMedianEigenvalues = {
		0.6, 0.4, 0.2
	};

	@Test
	public void medianValuesAreComputedProperly() {
		GossipData gd = new GossipData();
		gd.setNewProposal("node1", proposer1);
		gd.setNewProposal("node2", proposer2);
		gd.setNewProposal("node3", proposer3);
		
		assertArrayEquals(expectedMedianEigenvalues, gd.computeMedianOfProposedValues(), 0.0);
	}

	@Test
	public void proposalsAreProperlyRetrieved() {
		GossipData gd = new GossipData();
		gd.setNewProposal("node1", proposer1);
		gd.setNewProposal("node2", proposer2);
		gd.setNewProposal("node3", proposer3);
		
		assertArrayEquals(Analyzer.sortEigenvalues(proposer2), gd.getProposal("node2"), 0.0);
	}
}
