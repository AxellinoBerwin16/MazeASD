import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.IOException;

// Pastikan class ini public agar bisa dibaca oleh MazeApplication
public class MazePanel extends JPanel {
    private Cell[][] grid;
    private int rows, cols;
    private Timer timer;
    private boolean isAnimating = false;

    // Animation Data
    private List<Cell> exploreList, pathList;
    private int animIndex = 0;
    private boolean phaseExplore = true;
    private Runnable onFinish;
    private boolean isAlternativeMode = false;

    // --- VARIABLES UNTUK GAMBAR MINECRAFT ---
    private BufferedImage imgDefault, imgGrass, imgMud, imgWater;

    public MazePanel(Cell[][] grid) {
        this.grid = grid;
        this.rows = grid.length;
        this.cols = grid[0].length;

        // Mengambil warna background dari MazeApplication
        // Pastikan MazeApplication.BG_COLOR public static
        setBackground(MazeApplication.BG_COLOR);

        // Load Gambar saat Panel dibuat
        loadTextures();
    }

    private void loadTextures() {
        try {
            // Mengambil gambar dari folder src (classpath)
            // Pastikan file gambar ada di folder src project kamu
            // Gunakan "/" di depan nama file
            if (getClass().getResource("/default.png") != null)
                imgDefault = ImageIO.read(getClass().getResource("/default.png"));

            if (getClass().getResource("/grass.png") != null)
                imgGrass = ImageIO.read(getClass().getResource("/grass.png"));

            if (getClass().getResource("/mud.png") != null)
                imgMud = ImageIO.read(getClass().getResource("/mud.png"));

            if (getClass().getResource("/water.png") != null)
                imgWater = ImageIO.read(getClass().getResource("/water.png"));

        } catch (Exception e) {
            System.err.println("GAGAL MEMUAT GAMBAR! Pastikan file .png ada di folder src.");
            e.printStackTrace();
        }
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
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

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

                // 1. GAMBAR TEKSTUR (BACKGROUND)
                BufferedImage textureToDraw = imgDefault; // Default fallback

                // Pilih gambar berdasarkan terrainType string dari Cell
                if ("Grass".equals(cell.terrainType) && imgGrass != null) textureToDraw = imgGrass;
                else if ("Mud".equals(cell.terrainType) && imgMud != null) textureToDraw = imgMud;
                else if ("Water".equals(cell.terrainType) && imgWater != null) textureToDraw = imgWater;

                if (textureToDraw != null) {
                    g2d.drawImage(textureToDraw, x, y, cellSize, cellSize, null);
                } else {
                    // Jika gambar gagal loading atau null, pakai warna fallback
                    if(cell.terrainColor != null) g2d.setColor(cell.terrainColor);
                    else g2d.setColor(new Color(45, 45, 60));
                    g2d.fillRect(x, y, cellSize, cellSize);
                }

                // 2. EFEK VISUAL DI ATAS TEKSTUR (Overlay)
                if (cell.isPath) {
                    // PATH
                    g2d.setColor(isAlternativeMode ? new Color(189, 147, 249, 200) : new Color(255, 215, 0, 200));
                    int pathSize = cellSize / 2;
                    g2d.fillRect(x + pathSize/2, y + pathSize/2, pathSize, pathSize);

                } else if (cell.searched) {
                    // EXPLORED (Fog)
                    g2d.setColor(new Color(0, 0, 0, 100));
                    g2d.fillRect(x, y, cellSize, cellSize);
                }

                // 3. START & END POINTS
                if (r == 0 && c == 0) {
                    g2d.setColor(MazeApplication.GREEN_ACCENT);
                    g2d.fillOval(x + cellSize/4, y + cellSize/4, cellSize/2, cellSize/2);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawOval(x + cellSize/4, y + cellSize/4, cellSize/2, cellSize/2);
                }
                else if (r == rows - 1 && c == cols - 1) {
                    g2d.setColor(new Color(255, 85, 85));
                    g2d.fillOval(x + cellSize/4, y + cellSize/4, cellSize/2, cellSize/2);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawOval(x + cellSize/4, y + cellSize/4, cellSize/2, cellSize/2);
                }

                // 4. GAMBAR DINDING (WALLS)
                g2d.setColor(MazeApplication.WALL_COLOR);
                int strokeWidth = Math.max(2, cellSize/10);
                g2d.setStroke(new BasicStroke(strokeWidth));

                if (cell.topWall) g2d.drawLine(x, y, x + cellSize, y);
                if (cell.bottomWall) g2d.drawLine(x, y + cellSize, x + cellSize, y + cellSize);
                if (cell.leftWall) g2d.drawLine(x, y, x, y + cellSize);
                if (cell.rightWall) g2d.drawLine(x + cellSize, y, x + cellSize, y + cellSize);
            }
        }
    }
}