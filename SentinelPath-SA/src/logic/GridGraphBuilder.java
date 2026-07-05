package logic;

import model.Graph;
import model.Node;
import model.TerrainType;

/*
 * Builds a weighted graph from a 2D terrain map
 *
 * Each terrain cell becomes a node
 * Each node connects to up to 8 neighbors
 *
 * Edge weights depend on:
 * - movement cost (straight or diagonal)
 * - terrain cost of the destination node
 * - risk multiplier from the UI slider
 */
public class GridGraphBuilder {

    private final int rows;
    private final int cols;
    private final double riskMultiplier;

    private final Node[][] nodes;
    private final Graph graph;

    /*
     * @param rows number of rows in the grid
     * @param cols number of columns in the grid
     * @param riskMultiplier multiplier used to make non-road terrain more expensive
     */
    public GridGraphBuilder(int rows, int cols, double riskMultiplier) {
        this.rows = rows;
        this.cols = cols;
        this.riskMultiplier = riskMultiplier;
        this.nodes = new Node[rows][cols];
        this.graph = new Graph();
    }

    /*
     * Builds the graph from a terrain map
     */
    public void buildFromTerrain(TerrainType[][] terrainMap) {
        createNodes(terrainMap);
        connectNeighbors();
    }

    /*
     * Creates nodes from each terrain cell
     */
    private void createNodes(TerrainType[][] terrainMap) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Node node = new Node(r, c, terrainMap[r][c]);
                nodes[r][c] = node;
                graph.addNode(node);
            }
        }
    }

    /*
     * Connects each node to its valid surrounding neighbors
     */
    private void connectNeighbors() {
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Node current = nodes[r][c];

                if (current.isBlocked()) {
                    continue;
                }

                for (int i = 0; i < 8; i++) {
                    int nr = r + dr[i];
                    int nc = c + dc[i];

                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                        Node neighbor = nodes[nr][nc];

                        if (!neighbor.isBlocked()) {
                            boolean diagonal = Math.abs(dr[i]) + Math.abs(dc[i]) == 2;

                            // Straight moves are cheaper than diagonal moves
                            int movementCost = diagonal ? 14 : 10;

                            // Destination terrain cost adjusted by risk multiplier
                            int adjustedTerrainCost = getAdjustedTerrainCost(neighbor.getTerrainType());

                            int totalWeight = movementCost + adjustedTerrainCost;
                            graph.addDirectedEdge(current, neighbor, totalWeight);
                        }
                    }
                }
            }
        }
    }

    /*
     * Adjusts terrain cost using the risk multiplier
     *
     * ROAD stays cheap
     * Other terrain gets more expensive as risk increases
     */
    private int getAdjustedTerrainCost(TerrainType terrainType) {
        switch (terrainType) {
            case ROAD:
                return 1;
            case GRASS:
                return (int) Math.round(5 * riskMultiplier);
            case VEGETATION:
                return (int) Math.round(10 * riskMultiplier);
            case WATER:
                return (int) Math.round(1000 * riskMultiplier);
            case BLOCKED:
                return 99999;
            default:
                return terrainType.getCost();
        }
    }

    /*
     * 
     * @return the 2D node grid
     */
    public Node[][] getNodes() {
        return nodes;
    }

    /*
     * @return the completed graph
     */
    public Graph getGraph() {
        return graph;
    }
}