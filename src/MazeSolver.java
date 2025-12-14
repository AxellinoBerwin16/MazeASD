import java.util.*;

public class MazeSolver {
    private Cell[][] grid;
    private int rows, cols;

    public List<Cell> explorationSteps = new ArrayList<>();
    public List<Cell> finalPath = new ArrayList<>();

    public int cellsExploredCount = 0;
    public int pathCost = 0;
    public int pathLength = 0;
    public long executionTime = 0;

    public MazeSolver(Cell[][] grid) { updateGrid(grid); }

    public void updateGrid(Cell[][] grid) {
        this.grid = grid;
        this.rows = grid.length;
        this.cols = grid[0].length;
    }

    // --- MAIN SOLVER ---
    public boolean solve(String algorithm) {
        return solveInternal(algorithm, new ArrayList<>()); // Solve normal
    }

    // --- ALTERNATIVE SOLVER ---
    // Mencari jalan selain yang ada di 'excludePath'
    public boolean solveAlternative(String algorithm, List<Cell> previousPath) {
        // Logika: Naikkan 'cost' dari sel yang sudah dipakai di path sebelumnya
        // agar algoritma dipaksa mencari jalan memutar.
        for (Cell c : previousPath) {
            c.weight += 1000; // Penalty besar
        }

        boolean found = solveInternal(algorithm, new ArrayList<>());

        // Kembalikan weight ke semula
        for (Cell c : previousPath) {
            c.weight -= 1000;
        }
        return found;
    }

    private boolean solveInternal(String algorithm, List<Cell> ignoreList) {
        explorationSteps.clear();
        finalPath.clear();
        cellsExploredCount = 0;
        pathCost = 0;
        pathLength = 0;

        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++) grid[i][j].resetSolverState();

        Cell start = grid[0][0];
        Cell end = grid[rows-1][cols-1];

        long startTime = System.nanoTime();
        boolean found = false;

        switch(algorithm) {
            case "BFS": found = bfs(start, end); break;
            case "DFS": found = dfs(start, end); break;
            case "Dijkstra": found = dijkstra(start, end); break;
            case "A*": found = aStar(start, end); break;
        }
        long endTime = System.nanoTime();
        executionTime = (endTime - startTime) / 1000;

        if(found) {
            reconstructPath(end);
            calculateStats();
        }
        return found;
    }

    private void calculateStats() {
        pathLength = finalPath.size();
        pathCost = 0;
        for(Cell c : finalPath) pathCost += (c.weight > 500 ? c.weight - 1000 : c.weight); // Adjust stat calc if penalty used
    }

    // Algoritma (Update: visited check di sini agar clean)
    private boolean bfs(Cell start, Cell end) {
        Queue<Cell> queue = new LinkedList<>();
        queue.add(start);
        Set<Cell> visited = new HashSet<>();
        visited.add(start);

        while(!queue.isEmpty()) {
            Cell current = queue.poll();
            explorationSteps.add(current);
            cellsExploredCount++;
            if(current == end) return true;
            for(Cell neighbor : getNeighbors(current)) {
                if(!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    neighbor.parent = current;
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }

    private boolean dfs(Cell start, Cell end) {
        Stack<Cell> stack = new Stack<>();
        stack.push(start);
        Set<Cell> visited = new HashSet<>();
        visited.add(start);

        while(!stack.isEmpty()) {
            Cell current = stack.pop();
            explorationSteps.add(current);
            cellsExploredCount++;
            if(current == end) return true;
            for(Cell neighbor : getNeighbors(current)) {
                if(!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    neighbor.parent = current;
                    stack.push(neighbor);
                }
            }
        }
        return false;
    }

    private boolean dijkstra(Cell start, Cell end) {
        PriorityQueue<Cell> pq = new PriorityQueue<>(Comparator.comparingInt(c -> c.gCost));
        start.gCost = 0;
        pq.add(start);
        Set<Cell> visited = new HashSet<>();

        while(!pq.isEmpty()) {
            Cell current = pq.poll();
            if(visited.contains(current)) continue;
            visited.add(current);
            explorationSteps.add(current);
            cellsExploredCount++;
            if(current == end) return true;

            for(Cell neighbor : getNeighbors(current)) {
                if(visited.contains(neighbor)) continue;
                int newCost = current.gCost + neighbor.weight;
                if(newCost < neighbor.gCost) {
                    neighbor.gCost = newCost;
                    neighbor.parent = current;
                    pq.add(neighbor);
                }
            }
        }
        return false;
    }

    private boolean aStar(Cell start, Cell end) {
        PriorityQueue<Cell> pq = new PriorityQueue<>(Comparator.comparingInt(c -> c.fCost));
        start.gCost = 0;
        start.hCost = Math.abs(end.row - start.row) + Math.abs(end.col - start.col);
        start.fCost = start.gCost + start.hCost;
        pq.add(start);
        Set<Cell> visited = new HashSet<>();

        while(!pq.isEmpty()) {
            Cell current = pq.poll();
            if(visited.contains(current)) continue;
            visited.add(current);
            explorationSteps.add(current);
            cellsExploredCount++;
            if(current == end) return true;

            for(Cell neighbor : getNeighbors(current)) {
                if(visited.contains(neighbor)) continue;
                int tentativeGCost = current.gCost + neighbor.weight;
                if(tentativeGCost < neighbor.gCost) {
                    neighbor.parent = current;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = Math.abs(end.row - neighbor.row) + Math.abs(end.col - neighbor.col);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;
                    pq.add(neighbor);
                }
            }
        }
        return false;
    }

    private List<Cell> getNeighbors(Cell c) {
        List<Cell> list = new ArrayList<>();
        if(!c.topWall && c.row > 0) list.add(grid[c.row-1][c.col]);
        if(!c.bottomWall && c.row < rows-1) list.add(grid[c.row+1][c.col]);
        if(!c.leftWall && c.col > 0) list.add(grid[c.row][c.col-1]);
        if(!c.rightWall && c.col < cols-1) list.add(grid[c.row][c.col+1]);
        return list;
    }

    private void reconstructPath(Cell end) {
        Cell current = end;
        while(current != null) {
            finalPath.add(current);
            current = current.parent;
        }
        Collections.reverse(finalPath);
    }
}