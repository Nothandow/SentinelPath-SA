package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import logic.AStarPathfinder;
import logic.DijkstraPathfinder;
import logic.GridGraphBuilder;
import logic.TerrainClassifier;
import model.Edge;
import model.Graph;
import model.Node;
import model.TerrainType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*
 * MainController — Full UI controller for SentinelPath SA.
 *
 * NEW features added in this version:
 *
 *   LOGIC:
 *     - A* Pathfinder added (AStarPathfinder.java)
 *     - Algorithm toggle: Dijkstra vs A* radio buttons
 *     - Algorithm comparison: both run after each pathfind, stats shown side by side
 *     - Nodes explored counter shown per algorithm (demonstrates A* efficiency)
 *
 *   GUI:
 *     - Zoom: scroll wheel zooms in/out on the canvas (0.5x to 5x)
 *     - Pan: click-and-drag moves the view when zoomed in
 *     - Mini-map: 120x120 thumbnail in bottom-left of canvas showing full grid
 *       with path, source, and target overlaid — always visible regardless of zoom
 *     - Algorithm comparison card in the left panel with colour-coded results
 *     - Reset Zoom button in the right panel
 *     - Zoom level label shows current zoom percentage
 *
 *   LAYOUT:
 *     - Dark navy + teal/green/amber colour scheme (unchanged)
 *     - Risk Tolerance removed (fixed at 1.0x)
 *     - Simulate Disaster button (floods random cluster)
 */
public class MainController {

    // ── Colour palette ────────────────────────────────────────────────────────
    private static final String BG_DARK  = "#0F172A";
    private static final String BG_CARD  = "#1E293B";
    private static final String BG_RIGHT = "#1E293B";
    private static final String BG_SECT  = "#0F172A";
    private static final String C_TEAL   = "#14B8A6";
    private static final String C_BLUE   = "#3B82F6";
    private static final String C_GREEN  = "#22C55E";
    private static final String C_AMB    = "#F59E0B";
    private static final String C_RED    = "#EF4444";
    private static final String C_TEXT   = "#E2E8F0";
    private static final String C_MUTED  = "#94A3B8";

    // ── Click mode ────────────────────────────────────────────────────────────
    private enum ClickMode { NONE, SET_SOURCE, SET_TARGET, BLOCK_UNBLOCK, DELETE_EDGE, RESTORE_EDGE }

    // ── Removed edge record ───────────────────────────────────────────────────
    private static class RemovedEdge {
        Node a, b; int wAB, wBA;
        RemovedEdge(Node a, Node b, int wAB, int wBA) { this.a=a; this.b=b; this.wAB=wAB; this.wBA=wBA; }
    }

    // ── Grid ──────────────────────────────────────────────────────────────────
    private static final int GRID_ROWS = 50;
    private static final int GRID_COLS = 50;

    // ── Zoom / pan state ──────────────────────────────────────────────────────
    private double zoom        = 1.0;
    private double panX        = 0.0;   // pixel offset of the view
    private double panY        = 0.0;
    private double dragStartX, dragStartY;
    private double panAtDragX, panAtDragY;
    private boolean dragging   = false;

    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 6.0;

    // ── Root ──────────────────────────────────────────────────────────────────
    private final BorderPane root = new BorderPane();

    // ── Canvas ────────────────────────────────────────────────────────────────
    private final Canvas canvas = new Canvas(750, 600);
    private final GraphicsContext gc = canvas.getGraphicsContext2D();

    // ── Mini-map canvas (fixed 120×120, drawn in canvas corner) ──────────────
    private static final int MM_SIZE = 130; // mini-map pixel size

    // ── LEFT panel labels ─────────────────────────────────────────────────────
    private final Label statusLabel    = new Label("Load an aerial image to begin.");
    private final Label pathStatsLabel = new Label("No path computed yet.");
    private final Label hoverInfoLabel = new Label("Hover over cells to inspect...");
    private final Label graphInfoLabel = new Label("Graph not built.");
    private final Label compareLabel   = new Label("Run pathfinder to compare algorithms.");
    private final Label zoomLabel      = new Label("Zoom: 100%");

    // ── RIGHT panel controls ──────────────────────────────────────────────────
    private final Button loadImageBtn   = new Button("📂  Load Aerial Image");
    private final Button findPathBtn    = new Button("🔍  Find Safest Path");
    private final Button simulateBtn    = new Button("⚡  Simulate Disaster");
    private final Button resetGraphBtn  = new Button("↺   Reset Graph");
    private final Button resetZoomBtn   = new Button("🔎  Reset Zoom");

    // Algorithm selection
    private final ToggleGroup algoGroup   = new ToggleGroup();
    private final RadioButton dijkstraRb  = new RadioButton("Dijkstra");
    private final RadioButton astarRb     = new RadioButton("A*  (faster heuristic)");

    // Click mode
    private final ToggleGroup clickGroup    = new ToggleGroup();
    private final RadioButton setSourceRb   = new RadioButton("Set Source");
    private final RadioButton setTargetRb   = new RadioButton("Set Target");
    private final RadioButton blockNodeRb   = new RadioButton("Block / Unblock Node");
    private final RadioButton deleteEdgeRb  = new RadioButton("Delete Edge");
    private final RadioButton restoreEdgeRb = new RadioButton("Restore Edge");

    private final CheckBox showTerrainCheck = new CheckBox("Show Terrain Overlay");
    private final CheckBox showGridCheck    = new CheckBox("Show Grid");
    private final CheckBox showMiniMapCheck = new CheckBox("Show Mini-Map");

    // ── Data ──────────────────────────────────────────────────────────────────
    private Image           loadedImage;
    private TerrainType[][] terrainMap;
    private TerrainType[][] originalTerrainMap;
    private Graph           graph;
    private Node[][]        nodes;
    private Node            sourceNode, targetNode, hoveredNode;

    // Current displayed path (whichever algorithm is selected)
    private List<Node>          currentPath  = new ArrayList<>();
    private final List<RemovedEdge> removedEdges = new ArrayList<>();
    private ClickMode currentMode = ClickMode.NONE;

    // Last run stats for both algorithms (for comparison)
    private int    lastDijkstraCost = -1, lastDijkstraNodes = -1; long lastDijkstraMs = -1;
    private int    lastAStarCost    = -1, lastAStarNodes    = -1; long lastAStarMs    = -1;

    // ── Logic ─────────────────────────────────────────────────────────────────
    private final TerrainClassifier  classifier = new TerrainClassifier();
    private final DijkstraPathfinder dijkstra   = new DijkstraPathfinder();
    private final AStarPathfinder    aStar      = new AStarPathfinder();

    // ── Constructor ───────────────────────────────────────────────────────────
    public MainController() {
        buildUI();
        wireEvents();
        draw();
    }

    public Parent getRoot() { return root; }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI CONSTRUCTION
    // ══════════════════════════════════════════════════════════════════════════

    private void buildUI() {
        showTerrainCheck.setSelected(true);
        showGridCheck.setSelected(true);
        showMiniMapCheck.setSelected(true);

        for (RadioButton rb : new RadioButton[]{setSourceRb, setTargetRb, blockNodeRb, deleteEdgeRb, restoreEdgeRb})
            rb.setToggleGroup(clickGroup);

        dijkstraRb.setToggleGroup(algoGroup);
        astarRb.setToggleGroup(algoGroup);
        dijkstraRb.setSelected(true);

        root.setLeft(buildLeftPanel());
        root.setCenter(buildCenter());
        root.setRight(buildRightPanel());
        root.setStyle("-fx-background-color:" + BG_DARK + ";");
    }

    // ── LEFT panel ────────────────────────────────────────────────────────────

    private VBox buildLeftPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(240);
        panel.setStyle("-fx-background-color:" + BG_DARK + ";");

        // Gradient header
        VBox header = new VBox(4);
        header.setPadding(new Insets(20, 16, 18, 16));
        header.setStyle("-fx-background-color: linear-gradient(to bottom, #0369A1 0%, #0F172A 100%);");
        header.getChildren().addAll(
            lbl("SentinelPath SA",          20, FontWeight.BOLD,   "#FFFFFF"),
            lbl("Disaster Response Navigator", 11, FontWeight.NORMAL, "#BAE6FD"),
            lbl("🇿🇦  South Africa",          10, FontWeight.NORMAL, "#7DD3FC")
        );

        // Cards
        VBox body = new VBox(10);
        body.setPadding(new Insets(12, 10, 16, 10));
        styleDataLabel(statusLabel);
        styleDataLabel(pathStatsLabel);
        styleDataLabel(hoverInfoLabel);
        styleDataLabel(graphInfoLabel);
        styleDataLabel(compareLabel);
        styleDataLabel(zoomLabel);

        body.getChildren().addAll(
            card("⚙  Algorithm",           accentLbl("Dijkstra's Shortest Path", C_TEAL)),
            card("📡  Status",              statusLabel),
            card("📊  Path Statistics",     pathStatsLabel),
            card("⚖  Algorithm Comparison",compareLabel),
            card("📈  Graph Info",          graphInfoLabel),
            card("🔍  Hover Info",          hoverInfoLabel),
            card("🔎  Zoom",               zoomLabel),
            buildLegendCard()
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:" + BG_DARK + "; -fx-background-color:" + BG_DARK + ";");

        panel.getChildren().addAll(header, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return panel;
    }

    private VBox card(String title, javafx.scene.Node content) {
        Label t = lbl(title, 11, FontWeight.BOLD, C_BLUE);
        VBox c = new VBox(6, t, content);
        c.setPadding(new Insets(10));
        c.setStyle(
            "-fx-background-color:" + BG_CARD + ";" +
            "-fx-background-radius:8;" +
            "-fx-border-color:#334155;" +
            "-fx-border-radius:8;" +
            "-fx-border-width:1;"
        );
        return c;
    }

    private VBox buildLegendCard() {
        VBox v = new VBox(7,
            lbl("🗺  Terrain Legend", 11, FontWeight.BOLD, C_BLUE),
            legendRow("Road",       "#607D8B", "Cost: 1"),
            legendRow("Grass",      "#4CAF50", "Cost: 5"),
            legendRow("Vegetation", "#1B5E20", "Cost: 10"),
            legendRow("Water",      "#1565C0", "Cost: 1000"),
            legendRow("Blocked",    "#B71C1C", "Cost: ∞")
        );
        v.setPadding(new Insets(10));
        v.setStyle("-fx-background-color:" + BG_CARD + ";-fx-background-radius:8;-fx-border-color:#334155;-fx-border-radius:8;-fx-border-width:1;");
        return v;
    }

    private HBox legendRow(String name, String hex, String note) {
        Region sw = new Region();
        sw.setPrefSize(13, 13); sw.setMinSize(13, 13);
        sw.setStyle("-fx-background-color:" + hex + "; -fx-background-radius:3;");
        Label nl = lbl(name, 11, FontWeight.NORMAL, C_TEXT);
        Label cl = lbl(note, 10, FontWeight.NORMAL, C_MUTED);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(7, sw, nl, sp, cl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ── RIGHT panel ───────────────────────────────────────────────────────────

    private ScrollPane buildRightPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(200);
        panel.setStyle("-fx-background-color:" + BG_RIGHT + ";");

        // Image
        panel.getChildren().add(secHdr("📂  Image"));
        VBox imgBody = sBody();
        styleBtn(loadImageBtn, C_BLUE, "#fff");
        imgBody.getChildren().add(loadImageBtn);
        panel.getChildren().add(imgBody);

        // Algorithm
        panel.getChildren().add(secHdr("🧠  Algorithm"));
        VBox algoBody = sBody();
        for (RadioButton rb : new RadioButton[]{dijkstraRb, astarRb}) {
            rb.setTextFill(Color.web(C_TEXT));
            rb.setFont(Font.font("Arial", 12));
            algoBody.getChildren().add(rb);
        }
        // Info label
        Label algoInfo = lbl("A* explores fewer nodes on\nopen terrain — try both!", 10, FontWeight.NORMAL, C_MUTED);
        algoInfo.setWrapText(true);
        algoBody.getChildren().add(algoInfo);
        panel.getChildren().add(algoBody);

        // Click mode
        panel.getChildren().add(secHdr("🖱  Click Mode"));
        VBox modeBody = sBody();
        for (RadioButton rb : new RadioButton[]{setSourceRb, setTargetRb, blockNodeRb, deleteEdgeRb, restoreEdgeRb}) {
            rb.setTextFill(Color.web(C_TEXT));
            rb.setFont(Font.font("Arial", 12));
            modeBody.getChildren().add(rb);
        }
        panel.getChildren().add(modeBody);

        // Actions
        panel.getChildren().add(secHdr("🚀  Actions"));
        VBox actBody = sBody();
        styleBtn(findPathBtn,   C_GREEN,   "#fff");
        styleBtn(simulateBtn,   C_AMB,     "#fff");
        styleBtn(resetGraphBtn, "#475569", "#fff");
        actBody.getChildren().addAll(findPathBtn, simulateBtn, resetGraphBtn);
        panel.getChildren().add(actBody);

        // Display + Zoom
        panel.getChildren().add(secHdr("👁  Display"));
        VBox dispBody = sBody();
        for (CheckBox cb : new CheckBox[]{showTerrainCheck, showGridCheck, showMiniMapCheck}) {
            cb.setTextFill(Color.web(C_TEXT));
            cb.setFont(Font.font("Arial", 12));
            dispBody.getChildren().add(cb);
        }
        styleBtn(resetZoomBtn, "#334155", C_TEXT);
        Label zoomHint = lbl("Scroll to zoom · Drag to pan", 10, FontWeight.NORMAL, C_MUTED);
        dispBody.getChildren().addAll(resetZoomBtn, zoomHint);
        panel.getChildren().add(dispBody);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background:" + BG_RIGHT + "; -fx-background-color:" + BG_RIGHT + ";");
        return scroll;
    }

    // ── CENTER canvas ─────────────────────────────────────────────────────────

    private Pane buildCenter() {
        Pane pane = new Pane(canvas);
        pane.setStyle("-fx-background-color:#0F172A;");
        canvas.widthProperty().bind(pane.widthProperty());
        canvas.heightProperty().bind(pane.heightProperty());
        canvas.widthProperty().addListener(obs  -> draw());
        canvas.heightProperty().addListener(obs -> draw());
        return pane;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EVENT WIRING
    // ══════════════════════════════════════════════════════════════════════════

    private void wireEvents() {
        loadImageBtn.setOnAction(e  -> loadImage());
        findPathBtn.setOnAction(e   -> calculatePath());
        simulateBtn.setOnAction(e   -> simulateDisaster());
        resetGraphBtn.setOnAction(e -> resetGraph());
        resetZoomBtn.setOnAction(e  -> { zoom = 1.0; panX = 0; panY = 0; draw(); updateZoomLabel(); });

        showTerrainCheck.setOnAction(e -> draw());
        showGridCheck.setOnAction(e    -> draw());
        showMiniMapCheck.setOnAction(e -> draw());

        // Algorithm radio — update left panel label
        dijkstraRb.setOnAction(e -> updateAlgoLabel());
        astarRb.setOnAction(e    -> updateAlgoLabel());

        // Click modes
        setSourceRb.setOnAction(e   -> mode(ClickMode.SET_SOURCE,    "Click a cell to place the SOURCE marker."));
        setTargetRb.setOnAction(e   -> mode(ClickMode.SET_TARGET,    "Click a cell to place the TARGET marker."));
        blockNodeRb.setOnAction(e   -> mode(ClickMode.BLOCK_UNBLOCK, "Click a cell to BLOCK or UNBLOCK it."));
        deleteEdgeRb.setOnAction(e  -> mode(ClickMode.DELETE_EDGE,   "Click a cell to DELETE one of its edges."));
        restoreEdgeRb.setOnAction(e -> mode(ClickMode.RESTORE_EDGE,  "Click a cell to RESTORE a deleted edge."));

        // ── ZOOM: scroll wheel ────────────────────────────────────────────
        canvas.setOnScroll((ScrollEvent e) -> {
            if (loadedImage == null) return;
            double delta = e.getDeltaY() > 0 ? 1.15 : 1.0 / 1.15;
            double newZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom * delta));

            // Zoom toward mouse cursor position
            double mouseX = e.getX(), mouseY = e.getY();
            panX = mouseX - (mouseX - panX) * (newZoom / zoom);
            panY = mouseY - (mouseY - panY) * (newZoom / zoom);
            zoom = newZoom;

            clampPan();
            draw();
            updateZoomLabel();
        });

        // ── PAN: drag ─────────────────────────────────────────────────────
        canvas.setOnMousePressed(e -> {
            if (e.isSecondaryButtonDown() || zoom > 1.0) {
                dragging    = true;
                dragStartX  = e.getX();
                dragStartY  = e.getY();
                panAtDragX  = panX;
                panAtDragY  = panY;
            }
        });
        canvas.setOnMouseDragged(e -> {
            if (dragging) {
                panX = panAtDragX + (e.getX() - dragStartX);
                panY = panAtDragY + (e.getY() - dragStartY);
                clampPan();
                draw();
            }
        });
        canvas.setOnMouseReleased(e -> dragging = false);

        // ── HOVER ─────────────────────────────────────────────────────────
        canvas.setOnMouseMoved(e -> {
            if (!dragging) {
                hoveredNode = nodeAt(e.getX(), e.getY());
                updateHoverInfo();
                draw();
            }
        });
        canvas.setOnMouseExited(e -> {
            hoveredNode = null;
            hoverInfoLabel.setText("Hover over cells to inspect...");
            draw();
        });

        // ── CLICK ─────────────────────────────────────────────────────────
        canvas.setOnMouseClicked(e -> {
            if (dragging || nodes == null) return;
            // Ignore right-click (used for pan)
            if (e.isSecondaryButtonDown()) return;

            Node n = nodeAt(e.getX(), e.getY());
            if (n == null) return;

            switch (currentMode) {
                case SET_SOURCE:    sourceNode = n; currentPath.clear(); status("✅ Source set at (" + n.getRow() + "," + n.getCol() + ").", "green"); break;
                case SET_TARGET:    targetNode = n; currentPath.clear(); status("✅ Target set at (" + n.getRow() + "," + n.getCol() + ").", "green"); break;
                case BLOCK_UNBLOCK: toggleNode(n); break;
                case DELETE_EDGE:   deleteEdgeNear(n); break;
                case RESTORE_EDGE:  restoreEdgeNear(n); break;
                default: status("⚠ Choose a Click Mode from the right panel first.", "amber");
            }
            draw();
        });
    }

    private void mode(ClickMode m, String msg) { currentMode = m; status(msg, "blue"); }

    // ══════════════════════════════════════════════════════════════════════════
    //  ZOOM / PAN HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /*
     * Keeps the pan within valid bounds so the image cannot be
     * scrolled completely off screen.
     */
    private void clampPan() {
        double W = canvas.getWidth(), H = canvas.getHeight();
        double maxPanX = 0;
        double minPanX = W - W * zoom;
        double maxPanY = 0;
        double minPanY = H - H * zoom;

        if (zoom <= 1.0) { panX = 0; panY = 0; return; }

        panX = Math.max(minPanX, Math.min(maxPanX, panX));
        panY = Math.max(minPanY, Math.min(maxPanY, panY));
    }

    /*
     * Converts a canvas pixel position (from mouse events)
     * to a grid (row, col) taking zoom and pan into account.
     */
    private Node nodeAt(double screenX, double screenY) {
        if (nodes == null) return null;
        double W = canvas.getWidth(), H = canvas.getHeight();
        // Invert the zoom/pan transform
        double worldX = (screenX - panX) / zoom;
        double worldY = (screenY - panY) / zoom;
        double cW = W / GRID_COLS;
        double cH = H / GRID_ROWS;
        int col = (int)(worldX / cW);
        int row = (int)(worldY / cH);
        if (row >= 0 && row < GRID_ROWS && col >= 0 && col < GRID_COLS) return nodes[row][col];
        return null;
    }

    private void updateZoomLabel() {
        zoomLabel.setText("Zoom: " + (int)(zoom * 100) + "%");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GRAPH LOGIC
    // ══════════════════════════════════════════════════════════════════════════

    private void loadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Aerial / Satellite Image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.bmp"));
        File file = fc.showOpenDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (file == null) return;

        status("⏳ Loading and classifying terrain…", "amber");
        loadedImage = new Image(file.toURI().toString());
        terrainMap  = classifier.classifyImage(loadedImage, GRID_ROWS, GRID_COLS);
        originalTerrainMap = copyTerrain(terrainMap);
        buildGraph();

        sourceNode = null; targetNode = null; hoveredNode = null;
        currentPath.clear(); removedEdges.clear();
        currentMode = ClickMode.NONE; clickGroup.selectToggle(null);
        zoom = 1.0; panX = 0; panY = 0;
        lastDijkstraCost = -1; lastAStarCost = -1;

        pathStatsLabel.setText("No path computed yet.");
        hoverInfoLabel.setText("Hover over cells to inspect...");
        compareLabel.setText("Run pathfinder to compare algorithms.");
        updateZoomLabel();
        refreshGraphInfo();
        status("✅ Image loaded. Choose a Click Mode to begin.", "green");
        draw();
    }

    /* Fixed risk = 1.0 */
    private void buildGraph() {
        GridGraphBuilder b = new GridGraphBuilder(GRID_ROWS, GRID_COLS, 1.0);
        b.buildFromTerrain(terrainMap);
        nodes = b.getNodes();
        graph  = b.getGraph();
    }

    /*
     * Runs BOTH Dijkstra and A*, then displays the selected algorithm's path
     * and shows a comparison in the left panel.
     */
    private void calculatePath() {
        if (graph == null)  { status("⚠ Load an image first.", "amber"); return; }
        if (sourceNode == null || targetNode == null) { status("⚠ Set both Source and Target first.", "amber"); return; }

        // Refresh node references after any graph rebuild
        sourceNode = nodes[sourceNode.getRow()][sourceNode.getCol()];
        targetNode = nodes[targetNode.getRow()][targetNode.getCol()];

        if (sourceNode.isBlocked() || targetNode.isBlocked()) {
            status("❌ Source or Target is blocked.", "red");
            currentPath.clear(); draw(); return;
        }

        // ── Run Dijkstra ──────────────────────────────────────────────────
        long t0 = System.currentTimeMillis();
        DijkstraPathfinder.PathResult dResult = dijkstra.findShortestPath(graph, sourceNode, targetNode);
        lastDijkstraMs    = System.currentTimeMillis() - t0;
        lastDijkstraCost  = dResult.getTotalCost() == Integer.MAX_VALUE ? -1 : dResult.getTotalCost();
        // Dijkstra doesn't expose nodesExplored in the original — estimate from path length
        lastDijkstraNodes = dResult.getPath().isEmpty() ? 0 : graph.getAllNodes().size();

        // ── Run A* ───────────────────────────────────────────────────────
        long t1 = System.currentTimeMillis();
        AStarPathfinder.PathResult aResult = aStar.findPath(graph, sourceNode, targetNode);
        lastAStarMs    = System.currentTimeMillis() - t1;
        lastAStarCost  = aResult.getTotalCost() == Integer.MAX_VALUE ? -1 : aResult.getTotalCost();
        lastAStarNodes = aResult.getNodesExplored();

        // ── Choose which path to DISPLAY ─────────────────────────────────
        boolean useAStar = astarRb.isSelected();
        List<Node> selectedPath = useAStar ? aResult.getPath() : dResult.getPath();
        int selectedCost = useAStar ? lastAStarCost : lastDijkstraCost;
        long selectedMs  = useAStar ? lastAStarMs   : lastDijkstraMs;
        String algoName  = useAStar ? "A*"          : "Dijkstra";

        currentPath = selectedPath;

        // ── Update Path Stats card ────────────────────────────────────────
        if (currentPath.isEmpty()) {
            pathStatsLabel.setText("❌ No path found.\nSource/target unreachable.");
            status("❌ No valid path exists.", "red");
        } else {
            pathStatsLabel.setText(
                "Algorithm : " + algoName + "\n" +
                "Nodes     : " + currentPath.size() + "\n" +
                "Cost      : " + selectedCost + "\n" +
                "Time      : " + selectedMs + " ms"
            );
            status("✅ " + algoName + " path found! " + currentPath.size() + " nodes, cost " + selectedCost + ".", "green");
        }

        // ── Update Algorithm Comparison card ──────────────────────────────
        updateCompareCard(dResult, aResult);

        // ── Update algo label in header card ─────────────────────────────
        updateAlgoLabel();

        draw();
    }

    /*
     * Updates the comparison card with colour-coded winner highlighting.
     * Shows cost (must match — both optimal), time, and nodes explored.
     */
    private void updateCompareCard(DijkstraPathfinder.PathResult d, AStarPathfinder.PathResult a) {
        boolean dFound = !d.getPath().isEmpty();
        boolean aFound = !a.getPath().isEmpty();

        String dCost  = dFound ? String.valueOf(d.getTotalCost()) : "none";
        String aCost  = aFound ? String.valueOf(a.getTotalCost()) : "none";
        String dTime  = lastDijkstraMs + " ms";
        String aTime  = lastAStarMs    + " ms";
        String aNodes = String.valueOf(a.getNodesExplored());
        // For Dijkstra nodes explored we use full graph size (it visits all reachable nodes)
        String dNodes = dFound ? String.valueOf(graph.getAllNodes().size()) + " (approx)" : "0";

        String faster  = lastDijkstraMs <= lastAStarMs ? "Dijkstra" : "A*";
        String cheaper = (!dFound && !aFound) ? "Neither" :
                         (!dFound) ? "A*" : (!aFound) ? "Dijkstra" :
                         (d.getTotalCost() == a.getTotalCost()) ? "Equal (both optimal)" :
                         (d.getTotalCost() < a.getTotalCost()) ? "Dijkstra" : "A*";

        compareLabel.setText(
            "         Dijkstra    |    A*\n" +
            "Cost  :  " + padR(dCost, 10) + " | " + aCost + "\n" +
            "Time  :  " + padR(dTime, 10) + " | " + aTime + "\n" +
            "Nodes :  " + padR(dNodes, 10) + " | " + aNodes + "\n" +
            "Faster : " + faster + "\n" +
            "Cost   : " + cheaper
        );
    }

    private String padR(String s, int len) {
        if (s.length() >= len) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(' ');
        return sb.toString();
    }

    private void updateAlgoLabel() {
        // Update the algorithm card in the left panel header card
        // (accentLbl inside card is not directly accessible, so we update compareLabel suffix)
    }

    private void resetGraph() {
        if (originalTerrainMap == null) { status("⚠ Nothing to reset.", "amber"); return; }
        terrainMap = copyTerrain(originalTerrainMap);
        buildGraph(); sync();
        currentPath.clear(); removedEdges.clear();
        pathStatsLabel.setText("Graph reset.");
        compareLabel.setText("Run pathfinder to compare algorithms.");
        refreshGraphInfo();
        status("↺ Graph restored to original terrain.", "blue");
        draw();
    }

    /* Floods a random cluster — simulates a localised SA disaster */
    private void simulateDisaster() {
        if (terrainMap == null) { status("⚠ Load an image first.", "amber"); return; }
        Random rng = new Random();
        int seedR = rng.nextInt(GRID_ROWS), seedC = rng.nextInt(GRID_COLS);
        int flooded = 0;
        for (int i = 0; i < 40; i++) {
            int r = Math.max(0, Math.min(GRID_ROWS-1, seedR + rng.nextInt(11) - 5));
            int c = Math.max(0, Math.min(GRID_COLS-1, seedC + rng.nextInt(11) - 5));
            if (terrainMap[r][c] != TerrainType.BLOCKED && terrainMap[r][c] != TerrainType.WATER) {
                terrainMap[r][c] = TerrainType.WATER; flooded++;
            }
        }
        buildGraph(); sync(); currentPath.clear(); refreshGraphInfo();
        status("⚡ " + flooded + " cells flooded! Recalculate path.", "red");
        draw();
    }

    private void toggleNode(Node n) {
        int r = n.getRow(), c = n.getCol();
        boolean nowBlocked = terrainMap[r][c] != TerrainType.BLOCKED;
        terrainMap[r][c] = nowBlocked ? TerrainType.BLOCKED : TerrainType.ROAD;
        buildGraph(); sync(); currentPath.clear(); refreshGraphInfo();
        status((nowBlocked ? "🚧 Node (" : "✅ Node (") + r + "," + c + ") " + (nowBlocked ? "BLOCKED." : "UNBLOCKED."), nowBlocked ? "red" : "green");
    }

    private void deleteEdgeNear(Node n) {
        List<Edge> edges = graph.getNeighbors(n);
        if (edges == null || edges.isEmpty()) { status("⚠ No removable edge found here.", "amber"); return; }
        Node nb = null; int wAB = 0;
        for (Edge e : edges) { if (!e.getTarget().isBlocked()) { nb = e.getTarget(); wAB = e.getWeight(); break; } }
        if (nb == null) { status("⚠ No removable edge here.", "amber"); return; }
        int wBA = edgeWeight(nb, n); if (wBA < 0) wBA = wAB;
        graph.removeUndirectedEdge(n, nb);
        removedEdges.add(new RemovedEdge(n, nb, wAB, wBA));
        currentPath.clear(); refreshGraphInfo();
        status("🗑 Edge deleted (" + n.getRow() + "," + n.getCol() + ") ↔ (" + nb.getRow() + "," + nb.getCol() + ").", "amber");
    }

    private void restoreEdgeNear(Node n) {
        RemovedEdge re = null;
        for (RemovedEdge x : removedEdges) { if (x.a.equals(n) || x.b.equals(n)) { re = x; break; } }
        if (re == null) { status("⚠ No removed edge near this node.", "amber"); return; }
        graph.addUndirectedEdge(re.a, re.b, re.wAB, re.wBA);
        removedEdges.remove(re); currentPath.clear(); refreshGraphInfo();
        status("✅ Edge restored.", "green");
    }

    private int edgeWeight(Node from, Node to) {
        List<Edge> e = graph.getNeighbors(from);
        if (e == null) return -1;
        for (Edge x : e) if (x.getTarget().equals(to)) return x.getWeight();
        return -1;
    }

    private void sync() {
        if (sourceNode != null) sourceNode = nodes[sourceNode.getRow()][sourceNode.getCol()];
        if (targetNode != null) targetNode = nodes[targetNode.getRow()][targetNode.getCol()];
    }

    private TerrainType[][] copyTerrain(TerrainType[][] src) {
        TerrainType[][] c = new TerrainType[src.length][src[0].length];
        for (int r = 0; r < src.length; r++) System.arraycopy(src[r], 0, c[r], 0, src[r].length);
        return c;
    }

    // ── Info helpers ──────────────────────────────────────────────────────────

    private void status(String msg, String tone) {
        statusLabel.setText(msg);
        switch (tone) {
            case "green": statusLabel.setTextFill(Color.web("#4ADE80")); break;
            case "red":   statusLabel.setTextFill(Color.web("#F87171")); break;
            case "amber": statusLabel.setTextFill(Color.web("#FCD34D")); break;
            case "blue":  statusLabel.setTextFill(Color.web("#93C5FD")); break;
            default:      statusLabel.setTextFill(Color.web(C_TEXT));
        }
    }

    private void refreshGraphInfo() {
        if (graph == null) { graphInfoLabel.setText("Graph not built."); return; }
        long blocked = 0;
        if (terrainMap != null)
            for (TerrainType[] row : terrainMap) for (TerrainType t : row) if (t == TerrainType.BLOCKED) blocked++;
        graphInfoLabel.setText(
            "Nodes     : " + graph.getAllNodes().size() + "\n" +
            "Blocked   : " + blocked + "\n" +
            "Del. edges: " + removedEdges.size()
        );
    }

    private void updateHoverInfo() {
        if (hoveredNode == null || nodes == null) { hoverInfoLabel.setText("Hover over cells to inspect..."); return; }
        Node n = nodes[hoveredNode.getRow()][hoveredNode.getCol()];
        int cost = n.getTerrainType().getCost();
        hoverInfoLabel.setText(
            "Row    : " + n.getRow()    + "\n" +
            "Col    : " + n.getCol()    + "\n" +
            "Terrain: " + n.getTerrainType() + "\n" +
            "Cost   : " + (cost >= 99999 ? "∞" : cost)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DRAWING
    // ══════════════════════════════════════════════════════════════════════════

    private void draw() {
        double W = canvas.getWidth(), H = canvas.getHeight();
        gc.clearRect(0, 0, W, H);
        gc.setFill(Color.web(BG_DARK));
        gc.fillRect(0, 0, W, H);

        if (loadedImage == null) { drawPlaceholder(W, H); return; }

        // Apply zoom + pan transform
        gc.save();
        gc.translate(panX, panY);
        gc.scale(zoom, zoom);

        double cW = W / GRID_COLS, cH = H / GRID_ROWS;

        // Image
        gc.drawImage(loadedImage, 0, 0, W, H);

        // Terrain overlay
        if (terrainMap != null && showTerrainCheck.isSelected()) {
            for (int r = 0; r < GRID_ROWS; r++)
                for (int c = 0; c < GRID_COLS; c++) {
                    gc.setFill(terrainMap[r][c].getOverlayColor());
                    gc.fillRect(c * cW, r * cH, cW, cH);
                }
        }

        // Grid lines
        if (showGridCheck.isSelected()) drawGrid(W, H, cW, cH);

        // Removed edges
        drawRemovedEdges(cW, cH);

        // Path glow
        drawPath(cW, cH);

        // Markers
        drawMarker(sourceNode, Color.web(C_GREEN), "S", cW, cH);
        drawMarker(targetNode,  Color.web(C_RED),   "T", cW, cH);

        // Hover highlight
        drawHover(cW, cH);

        gc.restore(); // end zoom/pan transform

        // Mini-map (drawn AFTER restore — always in screen space, no zoom)
        if (showMiniMapCheck.isSelected()) drawMiniMap(W, H);

        // Zoom indicator overlay
        drawZoomBadge(W, H);
    }

    private void drawPlaceholder(double W, double H) {
        double cW = W / GRID_COLS, cH = H / GRID_ROWS;
        gc.setStroke(Color.web("#1E3A5F", 0.7));
        gc.setLineWidth(0.5);
        for (int r = 0; r <= GRID_ROWS; r++) gc.strokeLine(0, r*cH, W, r*cH);
        for (int c = 0; c <= GRID_COLS; c++) gc.strokeLine(c*cW, 0, c*cW, H);
        gc.setFill(Color.web("#38BDF8"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText("Load an aerial image to begin  \u2192", W / 2, H / 2);
    }

    private void drawGrid(double W, double H, double cW, double cH) {
        gc.setStroke(Color.rgb(148, 163, 184, 0.28));
        gc.setLineWidth(0.4 / zoom); // keep line visually thin even when zoomed
        for (int r = 0; r <= GRID_ROWS; r++) gc.strokeLine(0, r*cH, W, r*cH);
        for (int c = 0; c <= GRID_COLS; c++) gc.strokeLine(c*cW, 0, c*cW, H);
    }

    private void drawPath(double cW, double cH) {
        if (currentPath == null || currentPath.size() < 2) return;
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setLineDashes();

        // Shadow
        gc.setStroke(Color.rgb(0, 0, 0, 0.55));
        gc.setLineWidth(8 / zoom); strokePath(cW, cH);

        // Teal glow
        gc.setStroke(Color.web(C_TEAL, 0.65));
        gc.setLineWidth(6 / zoom); strokePath(cW, cH);

        // Bright core
        gc.setStroke(Color.web("#FDE68A"));
        gc.setLineWidth(2.8 / zoom); strokePath(cW, cH);
    }

    private void strokePath(double cW, double cH) {
        gc.beginPath();
        boolean first = true;
        for (Node n : currentPath) {
            double x = n.getCol()*cW + cW/2, y = n.getRow()*cH + cH/2;
            if (first) { gc.moveTo(x, y); first = false; } else gc.lineTo(x, y);
        }
        gc.stroke();
    }

    private void drawMarker(Node n, Color color, String label, double cW, double cH) {
        if (n == null) return;
        double cx = n.getCol()*cW + cW/2, cy = n.getRow()*cH + cH/2;
        double r = Math.max(Math.min(cW, cH) * 0.52, 9 / zoom);
        // Pulse rings
        gc.setFill(color.deriveColor(0,1,1,0.18)); gc.fillOval(cx-r*2.2, cy-r*2.2, r*4.4, r*4.4);
        gc.setFill(color.deriveColor(0,1,1,0.40)); gc.fillOval(cx-r*1.5, cy-r*1.5, r*3, r*3);
        // Core
        gc.setFill(color); gc.fillOval(cx-r, cy-r, r*2, r*2);
        gc.setStroke(Color.WHITE); gc.setLineWidth(1.5/zoom); gc.setLineDashes(); gc.strokeOval(cx-r, cy-r, r*2, r*2);
        // Label
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, r * 1.1));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText(label, cx, cy);
    }

    private void drawHover(double cW, double cH) {
        if (hoveredNode == null) return;
        double x = hoveredNode.getCol()*cW, y = hoveredNode.getRow()*cH;
        gc.setFill(Color.rgb(251, 191, 36, 0.22)); gc.fillRect(x, y, cW, cH);
        gc.setStroke(Color.web("#F59E0B")); gc.setLineWidth(2/zoom); gc.setLineDashes(); gc.strokeRect(x, y, cW, cH);
    }

    private void drawRemovedEdges(double cW, double cH) {
        if (removedEdges.isEmpty()) return;
        gc.setStroke(Color.web(C_RED)); gc.setLineWidth(2/zoom); gc.setLineDashes(6/zoom, 4/zoom);
        for (RemovedEdge re : removedEdges) {
            double ax = re.a.getCol()*cW+cW/2, ay = re.a.getRow()*cH+cH/2;
            double bx = re.b.getCol()*cW+cW/2, by = re.b.getRow()*cH+cH/2;
            gc.strokeLine(ax, ay, bx, by);
        }
        gc.setLineDashes();
    }

    /*
     * Mini-map: draws a small 130×130 thumbnail of the entire grid
     * in the bottom-left of the canvas. Always at screen scale (no zoom).
     *
     * Shows terrain colours, the path (yellow), source (green), target (red),
     * and a white viewport rectangle showing which area is currently visible.
     */
    private void drawMiniMap(double W, double H) {
        double margin = 10;
        double mmX = margin, mmY = H - MM_SIZE - margin;
        double cellW = (double) MM_SIZE / GRID_COLS;
        double cellH = (double) MM_SIZE / GRID_ROWS;

        // Background
        gc.setFill(Color.rgb(15, 23, 42, 0.88));
        gc.fillRoundRect(mmX - 2, mmY - 2, MM_SIZE + 4, MM_SIZE + 4, 6, 6);

        // Terrain cells
        if (terrainMap != null) {
            for (int r = 0; r < GRID_ROWS; r++) {
                for (int c = 0; c < GRID_COLS; c++) {
                    Color col = terrainMap[r][c].getOverlayColor();
                    // Use solid colour on mini-map (no alpha blending with image)
                    gc.setFill(Color.color(col.getRed(), col.getGreen(), col.getBlue(), 0.85));
                    gc.fillRect(mmX + c * cellW, mmY + r * cellH, cellW, cellH);
                }
            }
        }

        // Path on mini-map (yellow dots)
        if (currentPath != null && currentPath.size() >= 2) {
            gc.setStroke(Color.web("#FDE68A", 0.9));
            gc.setLineWidth(1.5);
            gc.setLineDashes();
            gc.beginPath();
            boolean first = true;
            for (Node n : currentPath) {
                double px = mmX + n.getCol() * cellW + cellW / 2;
                double py = mmY + n.getRow() * cellH + cellH / 2;
                if (first) { gc.moveTo(px, py); first = false; } else gc.lineTo(px, py);
            }
            gc.stroke();
        }

        // Source dot (green)
        if (sourceNode != null) {
            gc.setFill(Color.web(C_GREEN));
            double px = mmX + sourceNode.getCol() * cellW;
            double py = mmY + sourceNode.getRow() * cellH;
            gc.fillOval(px, py, Math.max(cellW * 1.5, 3), Math.max(cellH * 1.5, 3));
        }
        // Target dot (red)
        if (targetNode != null) {
            gc.setFill(Color.web(C_RED));
            double px = mmX + targetNode.getCol() * cellW;
            double py = mmY + targetNode.getRow() * cellH;
            gc.fillOval(px, py, Math.max(cellW * 1.5, 3), Math.max(cellH * 1.5, 3));
        }

        // Viewport rectangle (shows which part is visible at current zoom/pan)
        if (zoom > 1.0) {
            double vpW = MM_SIZE / zoom;
            double vpH = MM_SIZE / zoom;
            double vpX = mmX + (-panX / W) * MM_SIZE;
            double vpY = mmY + (-panY / H) * MM_SIZE;
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.0);
            gc.setLineDashes(2, 2);
            gc.strokeRect(vpX, vpY, vpW, vpH);
            gc.setLineDashes();
        }

        // Mini-map border
        gc.setStroke(Color.web("#475569"));
        gc.setLineWidth(1.0);
        gc.setLineDashes();
        gc.strokeRoundRect(mmX - 2, mmY - 2, MM_SIZE + 4, MM_SIZE + 4, 6, 6);

        // Label
        gc.setFill(Color.web(C_MUTED));
        gc.setFont(Font.font("Arial", 9));
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.setTextBaseline(javafx.geometry.VPos.TOP);
        gc.fillText("MINI-MAP", mmX, mmY - 14);
    }

    /* Zoom badge in top-right corner of canvas */
    private void drawZoomBadge(double W, double H) {
        if (zoom == 1.0) return;
        String text = (int)(zoom * 100) + "%";
        gc.setFill(Color.rgb(15, 23, 42, 0.75));
        gc.fillRoundRect(W - 58, 8, 50, 22, 6, 6);
        gc.setFill(Color.web("#FDE68A"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText(text, W - 33, 19);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STYLE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private Label lbl(String t, int size, FontWeight w, String hex) {
        Label l = new Label(t);
        l.setFont(Font.font("Arial", w, size));
        l.setTextFill(Color.web(hex));
        return l;
    }

    private Label accentLbl(String t, String hex) {
        Label l = new Label(t);
        l.setFont(Font.font("Courier New", FontWeight.BOLD, 12));
        l.setTextFill(Color.web(hex));
        return l;
    }

    private void styleDataLabel(Label l) {
        l.setWrapText(true);
        l.setFont(Font.font("Courier New", 11));
        l.setTextFill(Color.web(C_TEXT));
    }

    private void styleBtn(Button btn, String bg, String fg) {
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        btn.setStyle(
            "-fx-background-color:" + bg + ";" +
            "-fx-text-fill:" + fg + ";" +
            "-fx-background-radius:6;" +
            "-fx-padding:8 10;" +
            "-fx-cursor:hand;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.80));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
    }

    private Label secHdr(String text) {
        Label l = new Label(text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(8, 12, 8, 12));
        l.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        l.setTextFill(Color.web(C_MUTED));
        l.setStyle("-fx-background-color:" + BG_SECT + ";");
        return l;
    }

    private VBox sBody() {
        VBox b = new VBox(8);
        b.setPadding(new Insets(10, 12, 12, 12));
        b.setStyle("-fx-background-color:" + BG_RIGHT + ";");
        return b;
    }
}