package logic;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This class implements Dijkstra's shortest path algorithm
 *
 * It finds the lowest cost path between:
 * - a start node
 * - an end node
 *
 * The graph uses weighted edges where weights represent
 * terrain risk and movement cost.
 */
public class DijkstraPathfinder {

    /*
     * Stores the final shortest path and total path cost
     */
    public static class PathResult {
        private final List<Node> path;
        private final int totalCost;

        public PathResult(List<Node> path, int totalCost) {
            this.path = path;
            this.totalCost = totalCost;
        }

        public List<Node> getPath() {
            return path;
        }

        public int getTotalCost() {
            return totalCost;
        }
    }

    /*
     * Uses Dijkstra's algorithm to compute the safest / lowest cost path
     *
     * @param graph Graph containing all nodes and edges
     * @param start Starting node
     * @param end Ending node
     * 
     * @return PathResult containing the path and its total cost
     */
    public PathResult findShortestPath(Graph graph, Node start, Node end) {
        Map<Node, Integer> distances = new HashMap<>();
        Map<Node, Node> previous = new HashMap<>();
        MinHeapPriorityQueue queue = new MinHeapPriorityQueue();

        // Initialise all distances to infinity
        for (Node node : graph.getAllNodes()) {
            distances.put(node, Integer.MAX_VALUE);
        }

        // Distance to start node is 0
        distances.put(start, 0);
        queue.insert(start, 0);

        while (!queue.isEmpty()) {
            MinHeapPriorityQueue.Entry entry = queue.extractMin();

            if (entry == null) {
                break;
            }

            Node current = entry.getNode();
            int currentDistance = entry.getPriority();

            // Ignore outdated entries
            if (currentDistance > distances.get(current)) {
                continue;
            }

            // Stop if destination reached
            if (current.equals(end)) {
                break;
            }

            // Explore neighboring nodes
            for (Edge edge : graph.getNeighbors(current)) {
                Node neighbor = edge.getTarget();
                int newDistance = distances.get(current) + edge.getWeight();

                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    previous.put(neighbor, current);
                    queue.insert(neighbor, newDistance);
                }
            }
        }

        List<Node> path = reconstructPath(previous, start, end);
        int totalCost = distances.getOrDefault(end, Integer.MAX_VALUE);

        if (path.isEmpty()) {
            totalCost = Integer.MAX_VALUE;
        }

        return new PathResult(path, totalCost);
    }

    /*
     * Rebuilds the path by tracing backwards
     * from end node to start node using the previous map 
     */
    private List<Node> reconstructPath(Map<Node, Node> previous, Node start, Node end) {
        List<Node> path = new ArrayList<>();
        Node current = end;

        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }

        Collections.reverse(path);

        // Valid path must begin at the start node
        if (!path.isEmpty() && path.get(0).equals(start)) {
            return path;
        }

        return new ArrayList<>();
    }
}