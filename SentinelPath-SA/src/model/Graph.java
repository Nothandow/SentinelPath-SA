package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Represents the weighted graph used in the project
 *
 * The graph is stored as an adjacency list:
 * Each node maps to a list of outgoing edges
 */
public class Graph {

    private final Map<Node, List<Edge>> adjacencyList = new HashMap<>();

    /*
     * Adds a node to the graph if it does not already exist
     */
    public void addNode(Node node) {
        adjacencyList.putIfAbsent(node, new ArrayList<>());
    }

    /*
     * Adds a directed edge from source to target
     */
    public void addDirectedEdge(Node source, Node target, int weight) {
        adjacencyList.putIfAbsent(source, new ArrayList<>());
        adjacencyList.get(source).add(new Edge(source, target, weight));
    }

    /*
     * Adds an undirected edge between two nodes
     * by inserting two directed edges
     */
    public void addUndirectedEdge(Node a, Node b, int weightAB, int weightBA) {
        addDirectedEdge(a, b, weightAB);
        addDirectedEdge(b, a, weightBA);
    }

    /*
     * Returns all outgoing edges from the given node
     */
    public List<Edge> getNeighbors(Node node) {
        return adjacencyList.getOrDefault(node, new ArrayList<>());
    }

    /*
     * Returns all nodes currently in the graph
     */
    public Set<Node> getAllNodes() {
        return adjacencyList.keySet();
    }

    /*
     * Removes a directed edge from source to target
     */
    public void removeDirectedEdge(Node source, Node target) {
        List<Edge> edges = adjacencyList.get(source);

        if (edges != null) {
            edges.removeIf(edge -> edge.getTarget().equals(target));
        }
    }

    /*
     * Removes both directions of an edge between two nodes
     */
    public void removeUndirectedEdge(Node a, Node b) {
        removeDirectedEdge(a, b);
        removeDirectedEdge(b, a);
    }

    /*
     * Clears all outgoing edges from a node
     */
    public void clearEdges(Node node) {
        adjacencyList.put(node, new ArrayList<>());
    }
}