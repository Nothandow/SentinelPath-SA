package logic;

import model.Edge;
import model.Graph;
import model.Node;

import java.util.*;

/*
 *
 * A* improves on Dijkstra by using a heuristic function h(n) to guide
 * the search toward the target, reducing the number of nodes explored.
 *
 * f(n) = g(n) + h(n)
 *   g(n) = actual cost from source to n       (same as Dijkstra dist[])
 *   h(n) = estimated remaining cost to target (Euclidean grid distance)
 */
public class AStarPathfinder {

    /*
     * Stores the result of an A* run:
     * the optimal path, its total cost, and nodes explored count
     */
    public static class PathResult {
        private final List<Node> path;
        private final int        totalCost;
        private final int        nodesExplored;

        public PathResult(List<Node> path, int totalCost, int nodesExplored) {
            this.path          = path;
            this.totalCost     = totalCost;
            this.nodesExplored = nodesExplored;
        }

        public List<Node> getPath()         { return path; }
        public int        getTotalCost()    { return totalCost; }
        public int        getNodesExplored(){ return nodesExplored; }
    }

    /*
     * Finds the lowest-cost path from start to end using A*.
     *
     * @param graph  The weighted graph (adjacency list)
     * @param start  Source node
     * @param end    Target node
     * @return PathResult with path, cost, and nodes explored
     */
    public PathResult findPath(Graph graph, Node start, Node end) {

        Map<Node, Integer> g        = new HashMap<>();
        Map<Node, Node>    previous = new HashMap<>();
        Set<Node>          closed   = new HashSet<>();

        // Open set ordered by f = g + h
        MinHeapPriorityQueue open = new MinHeapPriorityQueue();

        int nodesExplored = 0;

        // Initialise: g[start] = 0, f[start] = h(start)
        for (Node n : graph.getAllNodes()) {
            g.put(n, Integer.MAX_VALUE);
        }
        g.put(start, 0);
        open.insert(start, heuristic(start, end));

        while (!open.isEmpty()) {
            MinHeapPriorityQueue.Entry entry = open.extractMin();
            if (entry == null) break;

            Node current = entry.getNode();

            // Skip if already confirmed
            if (closed.contains(current)) continue;
            closed.add(current);
            nodesExplored++;

            // Reached target — stop
            if (current.equals(end)) break;

            // Expand neighbours
            for (Edge edge : graph.getNeighbors(current)) {
                Node neighbour = edge.getTarget();
                if (closed.contains(neighbour)) continue;

                int gCurrent = g.getOrDefault(current, Integer.MAX_VALUE);
                if (gCurrent == Integer.MAX_VALUE) continue;

                int tentativeG = gCurrent + edge.getWeight();

                if (tentativeG < g.getOrDefault(neighbour, Integer.MAX_VALUE)) {
                    g.put(neighbour, tentativeG);
                    previous.put(neighbour, current);

                    // f = g + h
                    int f = tentativeG + heuristic(neighbour, end);
                    open.insert(neighbour, f);
                }
            }
        }

        List<Node> path = reconstructPath(previous, start, end);
        int cost = g.getOrDefault(end, Integer.MAX_VALUE);
        if (path.isEmpty()) cost = Integer.MAX_VALUE;

        return new PathResult(path, cost, nodesExplored);
    }

    /*
     * Euclidean distance heuristic scaled by movement cost (10).
     *
     * h(n) = sqrt((dr^2 + dc^2)) * 10
     *
     * This is admissible because straight moves cost at least 10 + 1 = 11,
     * and diagonal moves cost at least 14 + 1 = 15, so h never overshoots.
     */
    private int heuristic(Node a, Node b) {
        double dr = a.getRow() - b.getRow();
        double dc = a.getCol() - b.getCol();
        return (int)(Math.sqrt(dr * dr + dc * dc) * 10);
    }

    /*
     * Walks the previous[] map backwards from end to start.
     * Returns empty list if target was unreachable.
     */
    private List<Node> reconstructPath(Map<Node, Node> previous, Node start, Node end) {
        List<Node> path = new ArrayList<>();
        Node current = end;

        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }

        Collections.reverse(path);

        if (!path.isEmpty() && path.get(0).equals(start)) {
            return path;
        }
        return new ArrayList<>();
    }
}