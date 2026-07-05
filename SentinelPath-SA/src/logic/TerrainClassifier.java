package logic;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import model.TerrainType;

import java.util.HashMap;
import java.util.Map;

/*
 *
 * This class is responsible for classifying parts of an image
 * into terrain categories such as ROAD, GRASS, VEGETATION, WATER and BLOCKED
 *
 * The image is divided into a grid
 * For each grid cell the average color is calculated
 * That average color is then compared to reference terrain colors
 * 
 */
public class TerrainClassifier {

    // Stores reference colors for each terrain type
    private final Map<TerrainType, Color> referenceColors = new HashMap<>();

    /*
     * Initializes the reference colors used for terrain matching
     */
    public TerrainClassifier() {
        referenceColors.put(TerrainType.ROAD, Color.rgb(130, 130, 130));
        referenceColors.put(TerrainType.GRASS, Color.rgb(90, 170, 90));
        referenceColors.put(TerrainType.VEGETATION, Color.rgb(30, 110, 40));
        referenceColors.put(TerrainType.WATER, Color.rgb(50, 110, 210));
    }

    /*
     * 
     * Divides the image into a grid and classifies each grid cell
     *
     * @param image The image selected by the user
     * @param rows Number of grid rows
     * @param cols Number of grid columns
     * @return A 2D terrain map
     */
    public TerrainType[][] classifyImage(Image image, int rows, int cols) {
        TerrainType[][] terrainMap = new TerrainType[rows][cols];
        PixelReader reader = image.getPixelReader();

        int imageWidth = (int) image.getWidth();
        int imageHeight = (int) image.getHeight();

        double cellWidth = (double) imageWidth / cols;
        double cellHeight = (double) imageHeight / rows;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                // Determine the pixel boundaries of the current cell
                int startX = (int) Math.floor(c * cellWidth);
                int startY = (int) Math.floor(r * cellHeight);
                int endX = (int) Math.min(imageWidth - 1, Math.floor((c + 1) * cellWidth) - 1);
                int endY = (int) Math.min(imageHeight - 1, Math.floor((r + 1) * cellHeight) - 1);

                // Compute average color of this cell
                Color averageColor = averageColor(reader, startX, startY, endX, endY);

                // Classify average color into a terrain type
                terrainMap[r][c] = classify(averageColor);
            }
        }

        return terrainMap;
    }

    /*
     * Calculates the average color inside a grid cell
     * To improve performance, it samples pixels at intervals
     * instead of checking every single pixel
     */
    private Color averageColor(PixelReader reader, int startX, int startY, int endX, int endY) {
        double red = 0;
        double green = 0;
        double blue = 0;
        int count = 0;

        int stepX = Math.max(1, (endX - startX + 1) / 5);
        int stepY = Math.max(1, (endY - startY + 1) / 5);

        for (int y = startY; y <= endY; y += stepY) {
            for (int x = startX; x <= endX; x += stepX) {
                Color color = reader.getColor(x, y);
                red += color.getRed();
                green += color.getGreen();
                blue += color.getBlue();
                count++;
            }
        }

        if (count == 0) {
            return Color.BLACK;
        }

        return new Color(red / count, green / count, blue / count, 1.0);
    }

    /*
     * Compares a given color to all reference colors
     * and returns the closest matching terrain type
     */
    public TerrainType classify(Color pixelColor) {
        TerrainType closestType = TerrainType.GRASS;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<TerrainType, Color> entry : referenceColors.entrySet()) {
            double distance = colorDistance(pixelColor, entry.getValue());

            if (distance < minDistance) {
                minDistance = distance;
                closestType = entry.getKey();
            }
        }

        return closestType;
    }

    /*
     * Computes Euclidean distance between two RGB colors
     * A smaller distance means the colors are more similar
     */
    private double colorDistance(Color c1, Color c2) {
        double dr = c1.getRed() - c2.getRed();
        double dg = c1.getGreen() - c2.getGreen();
        double db = c1.getBlue() - c2.getBlue();

        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}