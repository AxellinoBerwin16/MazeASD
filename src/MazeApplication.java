import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class MazeApplication extends JFrame {
    // --- COLORS ---
    public static final Color BG_COLOR = new Color(30, 30, 46);
    public static final Color PANEL_COLOR = new Color(40, 41, 61);
    public static final Color ACCENT_COLOR = new Color(139, 233, 253);
    public static final Color TEXT_COLOR = new Color(220, 220, 220);
    public static final Color GREEN_ACCENT = new Color(80, 250, 123);
    public static final Color PURPLE_ACCENT = new Color(189, 147, 249); // Warna Alternatif
    public static final Color WALL_COLOR = new Color(98, 114, 164);

    private MazePanel mazePanel;
    private MazeGenerator generator;
    private MazeSolver solver;

    // Controls
    private JSpinner rowsSpinner, colsSpinner;
    private JComboBox<String> algoGenCombo, algoSolveCombo;
    private JCheckBox terrainCheck, loopCheck; // Loop Checkbox baru
    private JSlider speedSlider;
    private JLabel statExplored, statPathLen, statCost, statTime;
    private JButton btnSolve, btnAlternative; // Tombol baru

    // State
    private List<Cell> primaryPath = new ArrayList<>();

    public MazeApplication() {
        setTitle("Maze Solver: Multi-Path Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        generator = new MazeGenerator(20, 20);
        generator.generatePrim();
        solver = new MazeSolver(generator.getGrid());

        mazePanel = new MazePanel(generator.getGrid());
        add(mazePanel, BorderLayout.CENTER);

        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.EAST);

        setSize(1250, 850);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(PANEL_COLOR);
        sidebar.setBorder(new EmptyBorder(20, 20, 20, 20));
        sidebar.setPreferredSize(new Dimension(300, 800));

        // GRID
        addSectionHeader(sidebar, "GRID SETTINGS");
        JPanel gridPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        gridPanel.setBackground(PANEL_COLOR);
        rowsSpinner = createDarkSpinner(20);
        colsSpinner = createDarkSpinner(20);
        gridPanel.add(createLabeledComponent("Rows:", rowsSpinner));
        gridPanel.add(createLabeledComponent("Cols:", colsSpinner));
        sidebar.add(gridPanel);
        sidebar.add(Box.createVerticalStrut(10));

        // GENERATION
        addSectionHeader(sidebar, "GENERATION");
        algoGenCombo = createDarkCombo(new String[]{"Prim's Algorithm", "Kruskal's Algorithm"});
        sidebar.add(algoGenCombo);
        sidebar.add(Box.createVerticalStrut(10));

        terrainCheck = new JCheckBox("Weighted Terrain");
        styleCheckBox(terrainCheck);
        sidebar.add(terrainCheck);

        loopCheck = new JCheckBox("Enable Multiple Paths"); // Checkbox agar maze punya loop
        styleCheckBox(loopCheck);
        sidebar.add(loopCheck);

        sidebar.add(Box.createVerticalStrut(15));
        JButton btnGen = createStyledButton("GENERATE MAZE", ACCENT_COLOR);
        btnGen.addActionListener(e -> generateMaze());
        sidebar.add(btnGen);
        sidebar.add(Box.createVerticalStrut(25));

        // SOLVER
        addSectionHeader(sidebar, "PATHFINDING");
        algoSolveCombo = createDarkCombo(new String[]{"Dijkstra (Best)", "A* (Best)", "BFS", "DFS"});
        sidebar.add(algoSolveCombo);
        sidebar.add(Box.createVerticalStrut(15));

        JLabel lblSpeed = new JLabel("Animation Speed");
        lblSpeed.setForeground(TEXT_COLOR);
        sidebar.add(lblSpeed);
        speedSlider = new JSlider(1, 100, 50);
        speedSlider.setBackground(PANEL_COLOR);
        sidebar.add(speedSlider);
        sidebar.add(Box.createVerticalStrut(15));

        // Tombol Utama
        btnSolve = createStyledButton("SOLVE BEST PATH", GREEN_ACCENT);
        btnSolve.setForeground(Color.BLACK);
        btnSolve.addActionListener(e -> solveMaze());
        sidebar.add(btnSolve);
        sidebar.add(Box.createVerticalStrut(10));

        // Tombol Alternatif (Awalnya disable)
        btnAlternative = createStyledButton("SHOW ALTERNATIVE", PURPLE_ACCENT);
        btnAlternative.setForeground(Color.BLACK);
        btnAlternative.setEnabled(false);
        btnAlternative.addActionListener(e -> solveAlternative());
        sidebar.add(btnAlternative);
        sidebar.add(Box.createVerticalStrut(25));

        // STATS
        addSectionHeader(sidebar, "STATISTICS");
        JPanel statsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        statsPanel.setBackground(new Color(30, 30, 46));
        statsPanel.setBorder(BorderFactory.createLineBorder(WALL_COLOR, 1));
        statExplored = createStatLabel("Cells Explored: -");
        statPathLen = createStatLabel("Path Length: -");
        statCost = createStatLabel("Total Cost: -");
        statTime = createStatLabel("Time: -");
        statsPanel.add(padding(statExplored));
        statsPanel.add(padding(statPathLen));
        statsPanel.add(padding(statCost));
        statsPanel.add(padding(statTime));
        sidebar.add(statsPanel);

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(createLegend());
        return sidebar;
    }

    private void generateMaze() {
        if(mazePanel.isAnimating()) return;
        int r = (int) rowsSpinner.getValue();
        int c = (int) colsSpinner.getValue();
        generator.resize(r, c);

        String algo = (String) algoGenCombo.getSelectedItem();
        if (algo.contains("Prim")) generator.generatePrim();
        else generator.generateKruskal();

        // LOGIC BARU: Tambah loop jika dicentang
        if (loopCheck.isSelected()) {
            generator.addLoops(0.05); // 5% dinding dihapus acak untuk buat loop
        }

        if (terrainCheck.isSelected()) generator.generateTerrain();

        solver.updateGrid(generator.getGrid());
        mazePanel.setGrid(generator.getGrid(), r, c);

        resetStats();
        btnAlternative.setEnabled(false); // Reset tombol alt
        primaryPath.clear();
    }

    private void solveMaze() {
        if(mazePanel.isAnimating()) return;

        String algo = getSelectedAlgo();
        boolean found = solver.solve(algo);

        if (found) {
            primaryPath = new ArrayList<>(solver.finalPath); // Simpan path utama
            btnAlternative.setEnabled(true); // Hidupkan tombol alternatif

            int delay = 105 - speedSlider.getValue();
            if (delay < 5) delay = 5;
            mazePanel.setAlternativeMode(false); // Reset warna jadi Gold
            mazePanel.animate(solver.explorationSteps, solver.finalPath, delay, this::updateStats);
        } else {
            JOptionPane.showMessageDialog(this, "No Path Found!");
        }
    }

    // Logic Tombol Alternatif
    private void solveAlternative() {
        if(mazePanel.isAnimating()) return;
        if(primaryPath.isEmpty()) return;

        String algo = getSelectedAlgo();
        // Cari path dengan penalti pada path utama
        boolean found = solver.solveAlternative(algo, primaryPath);

        if (found) {
            int delay = 105 - speedSlider.getValue();
            if (delay < 5) delay = 5;

            mazePanel.setAlternativeMode(true); // Ubah warna jadi Ungu
            mazePanel.animate(solver.explorationSteps, solver.finalPath, delay, this::updateStats);
        } else {
            JOptionPane.showMessageDialog(this, "No Alternative Path Found (Maze might be too tight!)");
        }
    }

    private String getSelectedAlgo() {
        String algo = (String) algoSolveCombo.getSelectedItem();
        if(algo.contains("BFS")) return "BFS";
        if(algo.contains("DFS")) return "DFS";
        if(algo.contains("Dijkstra")) return "Dijkstra";
        return "A*";
    }

    private void updateStats() {
        statExplored.setText("Cells Explored: " + solver.cellsExploredCount);
        statPathLen.setText("Path Length: " + solver.pathLength);
        statCost.setText("Total Cost: " + solver.pathCost);
        statTime.setText("Time: " + solver.executionTime + " µs");
    }

    private void resetStats() {
        statExplored.setText("Cells Explored: -");
        statPathLen.setText("Path Length: -");
        statCost.setText("Total Cost: -");
        statTime.setText("Time: -");
    }

    // UI Styles
    private void styleCheckBox(JCheckBox cb) { cb.setForeground(TEXT_COLOR); cb.setBackground(PANEL_COLOR); cb.setFocusPainted(false); }
    private void addSectionHeader(JPanel p, String text) { JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(ACCENT_COLOR); p.add(l); p.add(Box.createVerticalStrut(8)); }
    private JSpinner createDarkSpinner(int val) { JSpinner s = new JSpinner(new SpinnerNumberModel(val, 5, 200, 1)); s.setMaximumSize(new Dimension(100, 30)); return s; }
    private JComboBox<String> createDarkCombo(String[] items) { JComboBox<String> box = new JComboBox<>(items); box.setMaximumSize(new Dimension(300, 35)); return box; }
    private JButton createStyledButton(String text, Color bg) { JButton btn = new JButton(text); btn.setMaximumSize(new Dimension(300, 40)); btn.setBackground(bg); btn.setForeground(Color.WHITE); btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); btn.setFocusPainted(false); btn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setAlignmentX(Component.CENTER_ALIGNMENT); return btn; }
    private JLabel createStatLabel(String text) { JLabel l = new JLabel(text); l.setForeground(Color.WHITE); l.setFont(new Font("Monospaced", Font.PLAIN, 12)); return l; }
    private JPanel padding(JComponent c) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT)); p.setOpaque(false); p.add(c); return p; }
    private JPanel createLabeledComponent(String label, JComponent comp) { JPanel p = new JPanel(new BorderLayout()); p.setBackground(PANEL_COLOR); JLabel l = new JLabel(label); l.setForeground(TEXT_COLOR); p.add(l, BorderLayout.WEST); p.add(comp, BorderLayout.EAST); p.setMaximumSize(new Dimension(300, 30)); return p; }
    private JPanel createLegend() { JPanel p = new JPanel(new GridLayout(3, 2)); p.setBackground(PANEL_COLOR); p.setBorder(BorderFactory.createTitledBorder(null, "Legend", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, TEXT_COLOR)); p.add(legendItem(new Color(56, 142, 60), "Grass (2)")); p.add(legendItem(new Color(121, 85, 72), "Mud (5)")); p.add(legendItem(new Color(25, 118, 210), "Water (10)")); p.add(legendItem(new Color(255, 215, 0), "Best Path")); p.add(legendItem(new Color(189, 147, 249), "Alt Path")); return p; }
    private JPanel legendItem(Color c, String t) { JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT)); p.setOpaque(false); JPanel colorBox = new JPanel(); colorBox.setPreferredSize(new Dimension(12, 12)); colorBox.setBackground(c); p.add(colorBox); JLabel l = new JLabel(t); l.setForeground(TEXT_COLOR); l.setFont(new Font("Arial", Font.PLAIN, 10)); p.add(l); return p; }

    public static void main(String[] args) { SwingUtilities.invokeLater(MazeApplication::new); }
}

class MazePanel extends JPanel {
    private Cell[][] grid;
    private int rows, cols;
    private Timer timer;
    private boolean isAnimating = false;

    private List<Cell> exploreList, pathList;
    private int animIndex = 0;
    private boolean phaseExplore = true;
    private Runnable onFinish;
    private boolean isAlternativeMode = false; // State warna path

    public MazePanel(Cell[][] grid) {
        this.grid = grid;
        this.rows = grid.length;
        this.cols = grid[0].length;
        setBackground(MazeApplication.BG_COLOR);
    }

    public void setGrid(Cell[][] grid, int r, int c) {
        if(isAnimating && timer != null) timer.stop();
        this.grid = grid;
        this.rows = r;
        this.cols = c;
        this.isAnimating = false;
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++) grid[i][j].resetSolverState();
        repaint();
    }

    public void setAlternativeMode(boolean mode) { this.isAlternativeMode = mode; }
    public boolean isAnimating() { return isAnimating; }

    public void animate(List<Cell> explore, List<Cell> path, int delay, Runnable callback) {
        if(this.grid == null) return;
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++) grid[i][j].resetSolverState();

        this.exploreList = explore;
        this.pathList = path;
        this.onFinish = callback;
        this.animIndex = 0;
        this.phaseExplore = true;
        this.isAnimating = true;

        if(timer != null && timer.isRunning()) timer.stop();
        timer = new Timer(delay, e -> updateAnimation());
        timer.start();
    }

    private void updateAnimation() {
        if (phaseExplore) {
            int stepsPerFrame = (exploreList.size() > 2000) ? 10 : (exploreList.size() > 500) ? 3 : 1;
            for(int k=0; k<stepsPerFrame; k++) {
                if (animIndex < exploreList.size()) {
                    exploreList.get(animIndex).searched = true;
                    animIndex++;
                } else {
                    phaseExplore = false;
                    animIndex = 0;
                    break;
                }
            }
        } else {
            if (animIndex < pathList.size()) {
                pathList.get(animIndex).isPath = true;
                animIndex++;
            } else {
                timer.stop();
                isAnimating = false;
                if(onFinish != null) onFinish.run();
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (grid == null) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelW = getWidth(), panelH = getHeight(), pad = 20;
        int availableW = panelW - (2 * pad), availableH = panelH - (2 * pad);
        int cellSize = Math.min(availableW / cols, availableH / rows);
        if (cellSize < 2) cellSize = 2;

        int startX = (panelW - (cellSize * cols)) / 2;
        int startY = (panelH - (cellSize * rows)) / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = startX + c * cellSize, y = startY + r * cellSize;
                Cell cell = grid[r][c];

                // Logic Warna
                if (cell.isPath) {
                    // Jika mode alternatif, warna Ungu, jika tidak Emas
                    g2d.setColor(isAlternativeMode ? MazeApplication.PURPLE_ACCENT : new Color(255, 215, 0));
                } else if (cell.searched) {
                    if(cell.terrainColor != null) g2d.setColor(cell.terrainColor.darker());
                    else g2d.setColor(new Color(70, 70, 90));
                } else if (cell.terrainColor != null) g2d.setColor(cell.terrainColor);
                else g2d.setColor(new Color(45, 45, 60));
                g2d.fillRect(x, y, cellSize, cellSize);

                if (r == 0 && c == 0) { g2d.setColor(MazeApplication.GREEN_ACCENT); g2d.fillRect(x + cellSize/4, y + cellSize/4, cellSize/2, cellSize/2); }
                else if (r == rows - 1 && c == cols - 1) { g2d.setColor(new Color(255, 85, 85)); g2d.fillRect(x + cellSize/4, y + cellSize/4, cellSize/2, cellSize/2); }

                g2d.setColor(MazeApplication.WALL_COLOR);
                g2d.setStroke(new BasicStroke(Math.max(1, cellSize/15)));
                if (cell.topWall) g2d.drawLine(x, y, x + cellSize, y);
                if (cell.bottomWall) g2d.drawLine(x, y + cellSize, x + cellSize, y + cellSize);
                if (cell.leftWall) g2d.drawLine(x, y, x, y + cellSize);
                if (cell.rightWall) g2d.drawLine(x + cellSize, y, x + cellSize, y + cellSize);
            }
        }
    }
}