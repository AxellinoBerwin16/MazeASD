import java.util.*;

public class MazeGenerator {
    private int rows, cols;
    private Cell[][] grid;
    private Random random = new Random();

    public MazeGenerator(int rows, int cols) { resize(rows, cols); }

    public void resize(int newRows, int newCols) {
        this.rows = newRows;
        this.cols = newCols;
        this.grid = new Cell[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = new Cell(i, j);
            }
        }
    }

    public Cell[][] getGrid() { return grid; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }

    public void generatePrim() {
        resetGridData();
        List<Cell> frontier = new ArrayList<>();
        Cell start = grid[0][0];
        start.visited = true;
        addFrontier(start, frontier);

        while (!frontier.isEmpty()) {
            Cell current = frontier.remove(random.nextInt(frontier.size()));
            List<Cell> visitedNeighbors = getVisitedNeighbors(current);
            if (!visitedNeighbors.isEmpty()) {
                Cell neighbor = visitedNeighbors.get(random.nextInt(visitedNeighbors.size()));
                current.removeWall(neighbor);
                current.visited = true;
                addFrontier(current, frontier);
            }
        }
    }

    public void generateKruskal() {
        resetGridData();
        List<Edge> edges = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r < rows - 1) edges.add(new Edge(grid[r][c], grid[r+1][c]));
                if (c < cols - 1) edges.add(new Edge(grid[r][c], grid[r][c+1]));
            }
        }
        Collections.shuffle(edges);
        int[] parent = new int[rows * cols];
        for (int i = 0; i < parent.length; i++) parent[i] = i;

        for (Edge edge : edges) {
            int id1 = edge.c1.row * cols + edge.c1.col;
            int id2 = edge.c2.row * cols + edge.c2.col;
            if (find(parent, id1) != find(parent, id2)) {
                edge.c1.removeWall(edge.c2);
                union(parent, id1, id2);
            }
        }
    }

    // --- FITUR BARU: MEMBUAT BANYAK SOLUSI ---
    public void addLoops(double probability) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Jangan hapus dinding border luar
                if (r < rows - 1 && random.nextDouble() < probability) {
                    grid[r][c].removeWall(grid[r+1][c]); // Hapus dinding bawah acak
                }
                if (c < cols - 1 && random.nextDouble() < probability) {
                    grid[r][c].removeWall(grid[r][c+1]); // Hapus dinding kanan acak
                }
            }
        }
    }

    public void generateTerrain() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if ((i == 0 && j == 0) || (i == rows-1 && j == cols-1)) {
                    grid[i][j].setTerrain("Default");
                    continue;
                }
                double chance = random.nextDouble();
                if (chance < 0.03) grid[i][j].setTerrain("Water");
                else if (chance < 0.08) grid[i][j].setTerrain("Mud");
                else if (chance < 0.15) grid[i][j].setTerrain("Grass");
                else grid[i][j].setTerrain("Default");
            }
        }
    }

    private void resetGridData() {
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++) grid[i][j] = new Cell(i, j);
    }
    private void addFrontier(Cell cell, List<Cell> frontier) {
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for(int[] d : dirs) {
            int nr = cell.row + d[0], nc = cell.col + d[1];
            if(isValid(nr, nc) && !grid[nr][nc].visited && !frontier.contains(grid[nr][nc])) frontier.add(grid[nr][nc]);
        }
    }
    private List<Cell> getVisitedNeighbors(Cell cell) {
        List<Cell> list = new ArrayList<>();
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for(int[] d : dirs) {
            int nr = cell.row + d[0], nc = cell.col + d[1];
            if(isValid(nr, nc) && grid[nr][nc].visited) list.add(grid[nr][nc]);
        }
        return list;
    }
    private boolean isValid(int r, int c) { return r >= 0 && r < rows && c >= 0 && c < cols; }
    private int find(int[] parent, int i) { return (parent[i] == i) ? i : (parent[i] = find(parent, parent[i])); }
    private void union(int[] parent, int i, int j) { parent[find(parent, i)] = find(parent, j); }
    private static class Edge { Cell c1, c2; Edge(Cell c1, Cell c2) { this.c1 = c1; this.c2 = c2; } }
}