SentinalSA
JavaFX disaster response tool that converts aerial imagery into a weighted graph and finds the safest rescue route using Dijkstra's algorithm. Built for South African emergency response scenarios (KZN floods, informal settlement fires).

SentinelPath SA
Real-time disaster response pathfinding for South African emergency teams.
Converts aerial imagery into a risk-weighted graph and calculates the safest rescue route — even when standard maps fail.

The Problem
South Africa regularly faces localised disasters — the 2022 KZN floods, fires in informal settlements — where road infrastructure is destroyed and conventional mapping tools become unreliable. Emergency responders need a way to navigate unpredictable, rapidly changing terrain using only what they can see from the air. SentinelPath SA solves this by treating any aerial .jpg or .png as a live navigable map, classifying terrain by colour, building a weighted graph on-the-fly, and computing the safest path for rescue vehicles in real time.

How It Works
1. Image → Graph Abstraction
  | Concept | Implementation |
| **Nodes (V)** | Image segmented into a 50 × 50 grid; each cell = one node |
| **Edges (E)** | Each node connects to its 8 neighbours (horizontal, vertical, diagonal) |
| **Weights (W)** | Edge cost = terrain classification of the target node |
2. Terrain Classification (Task 1)
A Hash Table stores reference RGB values for known terrain types.

Each grid node's colour is sampled and matched to the nearest reference category.

Classification drives traversal cost assignment:

 | Terrain | Weight |
 | Road | 1 |
 | Vegetation / Grass | 5 |
 | Debris / Unknown | 50 |
 | Water | 1,000 |
3. Pathfinding — Dijkstra's Algorithm (Task 2)
Implemented with a manual Adaptable Priority Queue (min-heap).
Complexity: O(E log V) — handles the full 50×50 grid interactively.
The safest path updates instantly as users modify the map.
Features
Interactive Map Canvas — load any aerial .jpg / .png as the base layer.
Live Path Rendering — a dynamically coloured route line recalculates on every change.
Manual Override — click any cell to mark it as blocked (debris, collapsed structure).
Edge Deletion — remove the edge between two nodes to simulate a bridge collapse; the pathfinder reroutes automatically.
Risk Tooltip — hover over any node to see its classified terrain type and current risk score.
Risk Tolerance Sliders — tune how aggressively the algorithm avoids water vs. vegetation.
Tech Stack
| Layer | Technology |
| Language | Java 17+ |
| GUI | JavaFX |
| Graph | Custom adjacency-list implementation |
| Priority Queue | Manual binary min-heap (Adaptable PQ) |
| Classification | Hash Table (RGB → terrain type) |
Project Structure
SentinelSA/
├── src/
│   ├── graph/
│   │   ├── GridGraph.java          # Node/edge construction from image grid
│   │   ├── AdaptablePQ.java        # Manual priority queue for Dijkstra
│   │   └── Dijkstra.java           # Shortest-path implementation
│   ├── classification/
│   │   ├── TerrainClassifier.java  # Hash table RGB → terrain mapping
│   │   └── TerrainType.java        # Enum: ROAD, VEGETATION, WATER, DEBRIS
│   ├── ui/
│   │   ├── MapCanvas.java          # JavaFX canvas — renders grid + path
│   │   └── ControlPanel.java       # Sliders, buttons, interaction handlers
│   └── Main.java
├── assets/
│   └── sample_aerial.png           # Example KZN terrain image
└── README.md
Getting Started
Prerequisites
Java 17 or higher
JavaFX SDK 17+ (download)
Run
git clone https://github.com/MooketsiKosa/SentinelSA.git
cd SentinelSA

# Compile (adjust --module-path to your JavaFX SDK location)
javac --module-path /path/to/javafx-sdk/lib \
      --add-modules javafx.controls,javafx.fxml \
      -d out src/**/*.java

# Run
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp out Main
Data Sources
Springer: Disaster response mapping research
DFFE EGIS: South African Environmental GIS Portal
JBA Risk: South Africa Flood Map Insights
Academic Context
Developed as a Computer Science 3A project at the University of Johannesburg, demonstrating applied data structures (weighted graphs, hash tables, priority queues) in a real-world South African emergency response context.
