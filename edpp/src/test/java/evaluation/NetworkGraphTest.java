package evaluation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class NetworkGraphTest {

	NetworkGraph ng;
	
	String [] nodeIds = {"1", "2", "3", "4", "5"};
	
	int [][] expectedAdjacencyMatrix = 
		{ {0, 0, 1, 0, 1},
			{1, 0, 1, 0, 1},
			{0, 1, 0, 1, 0},
			{1, 1, 0, 0, 0},
			{0, 1, 1, 1, 0}
	};
	
	double [][] expectedWeightsMatrix = 
		{ 	{0, 0, 0.33, 0, 0.5},
			{0.5, 0, 0.33, 0, 0.5},
			{0, 0.33, 0, 0.5, 0},
			{0.5, 0.33, 0, 0, 0},
			{0, 0.33, 0.33, 0.5, 0}
	};
	
	@Before
	public void setUp() throws Exception {
		ng = new NetworkGraph();
		for (int i = 0; i < nodeIds.length; i++) {
			ng.addNode(nodeIds[i]);
		}
		ng.addLinkWithWeight("1", "2", 0.5);
		ng.addLinkWithWeight("1", "4", 0.5);
		ng.addLinkWithWeight("2", "3", 0.33);
		ng.addLinkWithWeight("2", "4", 0.33);
		ng.addLinkWithWeight("2", "5", 0.33);
		ng.addLinkWithWeight("3", "1", 0.33);
		ng.addLinkWithWeight("3", "2", 0.33);
		ng.addLinkWithWeight("3", "5", 0.33);
		ng.addLinkWithWeight("4", "3", 0.5);
		ng.addLinkWithWeight("4", "5", 0.5);
		ng.addLinkWithWeight("5", "1", 0.5);
		ng.addLinkWithWeight("5", "2", 0.5);
	}

	@Test
	public void adjacencyMatrixIsCreatedProperly() {
		int [][] computedAdjacencyMatrix = ng.getAdjacencyMatrix();
		for(int i = 0; i < nodeIds.length; i++) {
			assertArrayEquals(computedAdjacencyMatrix[i], expectedAdjacencyMatrix[i]);
		}
	}
	
	@Test
	public void weightsMatrixIsCreatedProperly() {
		double [][] computedWeightsMatrix = ng.getMatrixOfWeights();
		for (int i = 0; i < nodeIds.length; i++) {
			assertArrayEquals(computedWeightsMatrix[i], expectedWeightsMatrix[i], 0);
		}
	}

}
