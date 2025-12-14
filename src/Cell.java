import java.awt.Color;

public class Cell {
    public int row, col;
    public boolean topWall = true, rightWall = true, bottomWall = true, leftWall = true;

    // State Visualisasi
    public boolean visited = false; // Saat generate
    public boolean searched = false; // Saat solving (Closed Set)
    public boolean isPath = false;   // Jalur solusi

    // Bobot / Terrain
    public int weight = 1;
    public Color terrainColor; // Akan di-set null utk default agar ikut tema
    public String terrainType = "Default";

    // Algoritma props
    public int gCost = Integer.MAX_VALUE;
    public int hCost = 0;
    public int fCost = 0;
    public Cell parent = null;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.terrainColor = null; // null means use default theme color
    }

    public void setTerrain(String type) {
        this.terrainType = type;
        switch (type) {
            case "Grass":
                this.weight = 2;
                this.terrainColor = new Color(56, 142, 60); // Darker Green
                break;
            case "Mud":
                this.weight = 5;
                this.terrainColor = new Color(121, 85, 72); // Brown
                break;
            case "Water":
                this.weight = 10;
                this.terrainColor = new Color(25, 118, 210); // Dark Blue
                break;
            default:
                this.weight = 1;
                this.terrainColor = null; // Default Theme Color
                break;
        }
    }

    public void removeWall(Cell neighbor) {
        int rDiff = this.row - neighbor.row;
        int cDiff = this.col - neighbor.col;

        if (rDiff == 1) { this.topWall = false; neighbor.bottomWall = false; }
        else if (rDiff == -1) { this.bottomWall = false; neighbor.topWall = false; }
        if (cDiff == 1) { this.leftWall = false; neighbor.rightWall = false; }
        else if (cDiff == -1) { this.rightWall = false; neighbor.leftWall = false; }
    }

    public void resetSolverState() {
        this.searched = false;
        this.isPath = false;
        this.parent = null;
        this.gCost = Integer.MAX_VALUE;
        this.hCost = 0;
        this.fCost = 0;
    }
}