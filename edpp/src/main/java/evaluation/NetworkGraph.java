package evaluation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

public class NetworkGraph {

	private SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>  graph;
	
	public NetworkGraph() {
		graph = new SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
	}
	
	public boolean addNode(String nodeId) {
		return graph.addVertex(nodeId);
	}
	
	public void addLink(String sourceNode, String destNode) {
		graph.addEdge(sourceNode, destNode);
	}
	
	public void addLinkWithWeight(String sourceNode, String destNode, double weight) {
		DefaultWeightedEdge edge = graph.addEdge(sourceNode, destNode);
		graph.setEdgeWeight(edge, weight);
	}
	
	public int [][] getAdjacencyMatrix() {
		int [][] adjacencyMatrix;
		Map<String, Integer> nodeToIndex = new HashMap<String, Integer>();
		
		Set<String> vertices = graph.vertexSet();
		int numOfVertices = vertices.size();
		adjacencyMatrix = new int [numOfVertices][numOfVertices];
		
		int i = 0;
		for (String node : vertices) {
			nodeToIndex.put(node, i);
			i++;
		}
		
		for (String source : nodeToIndex.keySet()) {
			//if Aij is 1, there is an edge from j to i
			Set<DefaultWeightedEdge> edges = graph.outgoingEdgesOf(source);
			for (DefaultWeightedEdge edge : edges) {
				String target = graph.getEdgeTarget(edge);
				int j = nodeToIndex.get(source);
				i = nodeToIndex.get(target);
				adjacencyMatrix[i][j] = 1;
			}
		}
		return adjacencyMatrix;
	}
	
	public double [][] getMatrixOfWeights() {
		double [][] weightedMatrix;
		
		Map<String, Integer> nodeToIndex = new HashMap<String, Integer>();
		
		Set<String> vertices = graph.vertexSet();
		int numOfVertices = vertices.size();
		weightedMatrix = new double [numOfVertices][numOfVertices];
		
		int i = 0;
		for (String node : vertices) {
			nodeToIndex.put(node, i);
			i++;
		}
		
		for (String source : nodeToIndex.keySet()) {
			//if Aij is 1, there is an edge from j to i
			Set<DefaultWeightedEdge> edges = graph.outgoingEdgesOf(source);
			for (DefaultWeightedEdge edge : edges) {
				String target = graph.getEdgeTarget(edge);
				int j = nodeToIndex.get(source);
				i = nodeToIndex.get(target);
				weightedMatrix[i][j] = graph.getEdgeWeight(edge);
			}
		}
		return weightedMatrix;
	}
	
}
