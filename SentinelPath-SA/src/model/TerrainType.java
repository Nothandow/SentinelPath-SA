package model;

import javafx.scene.paint.Color;

/*
 * Represents the terrain categories used in the project
 *
 * Each terrain type has:
 * - a traversal cost (used in pathfinding)
 * - an overlay color (used for display)
 */
public enum TerrainType {
    ROAD(1, Color.rgb(120, 120, 120, 0.35)),
    GRASS(5, Color.rgb(80, 180, 80, 0.35)),
    VEGETATION(10, Color.rgb(20, 120, 40, 0.35)),
    WATER(1000, Color.rgb(40, 100, 220, 0.35)),
    BLOCKED(99999, Color.rgb(220, 50, 50, 0.45));

    private final int cost;
    private final Color overlayColor;

    /*
     * @param cost traversal cost
     * @param overlayColor color used when drawing terrain overlay
     */
    TerrainType(int cost, Color overlayColor) {
        this.cost = cost;
        this.overlayColor = overlayColor;
    }

    /*
     * Returns the traversal cost
     */
    public int getCost() {
        return cost;
    }

    /*
     * Returns the display overlay color
     */
    public Color getOverlayColor() {
        return overlayColor;
    }
}