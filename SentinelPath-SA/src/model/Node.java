package model;

/*
 * Represents one cell in the terrain grid
 *
 * Each node stores:
 * - its row index
 * - its column index
 * - its terrain type
 */
public class Node {

    private final int row;
    private final int col;
    private TerrainType terrainType;
    private boolean blocked;

    /*
     * Creates a node at a specific row and column
     * and assigns its terrain type
     */
    public Node(int row, int col, TerrainType terrainType) {
        this.row = row;
        this.col = col;
        this.terrainType = terrainType;
        this.blocked = terrainType == TerrainType.BLOCKED;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public TerrainType getTerrainType() {
        return terrainType;
    }

    /*
     * Updates the terrain type of the node
     */
    public void setTerrainType(TerrainType terrainType) {
        this.terrainType = terrainType;
        this.blocked = terrainType == TerrainType.BLOCKED;
    }

    /*
     * Returns true if the node is blocked
     */
    public boolean isBlocked() {
        return blocked;
    }

    /*
     * Sets blocked state directly
     */
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
        if (blocked) {
            this.terrainType = TerrainType.BLOCKED;
        }
    }

    @Override
    public String toString() {
        return "Node(" + row + "," + col + "," + terrainType + ")";
    }

    /*
     * Two nodes are equal if they have the same row and column
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Node)) return false;

        Node other = (Node) obj;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }
}