package evaluation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jgrapht.ext.MatrixExporter;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableDirectedWeightedGraph;

/**
 * Class representing the network topology in a graph. This class can be used to
 * construct the adjacency matrix of the network or any matrix related to that
 * 
 * @author Xenofon Foukas
 * 
 */
public class NetworkGraph {

	private ListenableDirectedWeightedGraph<String, DefaultWeightedEdge> graph;

	/**
	 * Constructor class. Creates a new empty directed graph with weighted edges
	 */
	public NetworkGraph() {
		graph = new ListenableDirectedWeightedGraph<String, DefaultWeightedEdge>(
				DefaultWeightedEdge.class);
	}

	/**
	 * This method adds a new node to the graph. If the node already exists, no
	 * action is taken
	 * 
	 * @param nodeId
	 *            the string representation of the node's id
	 * @return true if the node was added to the graph, otherwise false
	 */
	public boolean addNode(String nodeId) {
		return graph.addVertex(nodeId);
	}

	/**
	 * Adds a new edge to the graph. If the edge already exists, no action is
	 * taken
	 * 
	 * @param sourceNode
	 *            the id representation of the source node's id
	 * @param destNode
	 *            the id representation of the destination node's id
	 */
	public void addLink(String sourceNode, String destNode) {
		graph.addEdge(sourceNode, destNode);
	}

	/**
	 * Adds a new weighted edge to the graph. If the edge already exists, its
	 * current weight is updated
	 * 
	 * @param sourceNode
	 *            the id representation of the source node's id
	 * @param destNode
	 *            the id representation of the destination node's id
	 * @param weight
	 *            the weight of this edge
	 */
	public void addLinkWithWeight(String sourceNode, String destNode,
			double weight) {
		DefaultWeightedEdge edge = graph.addEdge(sourceNode, destNode);
		graph.setEdgeWeight(edge, weight);
	}

	/**
	 * Computes the adjacency matrix of this graph
	 * 
	 * @return an adjacency matrix of type double[][], where element i,j is 1 if
	 *         there is a link from node i to node j, otherwise 0
	 */
	public int[][] getAdjacencyMatrix() {
		int[][] adjacencyMatrix;
		Map<String, Integer> nodeToIndex = new HashMap<String, Integer>();

		Set<String> vertices = graph.vertexSet();
		int numOfVertices = vertices.size();
		adjacencyMatrix = new int[numOfVertices][numOfVertices];

		int i = 0;
		for (String node : vertices) {
			nodeToIndex.put(node, i);
			i++;
		}

		for (String source : nodeToIndex.keySet()) {
			// if Aij is 1, there is an edge from j to i
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

	/**
	 * This method creates a matrix of weights for all the edges in the graph
	 * 
	 * @return a matrix of weights of type double [][], where element i,j
	 *         contains the weight of the edge from node i to node j
	 */
	public double[][] getMatrixOfWeights() {
		double[][] weightedMatrix;

		Map<String, Integer> nodeToIndex = new HashMap<String, Integer>();

		Set<String> vertices = graph.vertexSet();
		int numOfVertices = vertices.size();
		weightedMatrix = new double[numOfVertices][numOfVertices];

		int i = 0;
		for (String node : vertices) {
			nodeToIndex.put(node, i);
			i++;
		}

		for (String source : nodeToIndex.keySet()) {
			// if Aij is 1, there is an edge from j to i
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

	/**
	 * Writes the adjacency matrix of the graph to a file named adjacency.mat in
	 * the current directory
	 */
	public void exportAdjacencyMatrix() {
		MatrixExporter<String, DefaultWeightedEdge> exporter = new MatrixExporter<String, DefaultWeightedEdge>();
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter("adjacency.mat")));
			exporter.exportAdjacencyMatrix(out, graph);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @return this method returns the created graph of type
	 *         ListenableDirectedWeightedGraph
	 */
	public ListenableDirectedWeightedGraph<String, DefaultWeightedEdge> getGraph() {
		return this.graph;
	}

}
