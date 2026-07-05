package model;

/*
 *
 * Represents a weighted connection from one node to another
 *
 * Each edge stores:
 * - the source node
 * - the target node
 * - the traversal weight
 */
public class Edge {

    private final Node source;
    private final Node target;
    private final int weight;

    /*
     * Constructor
     */
    public Edge(Node source, Node target, int weight) {
        this.source = source;
        this.target = target;
        this.weight = weight;
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    public int getWeight() {
        return weight;
    }
}