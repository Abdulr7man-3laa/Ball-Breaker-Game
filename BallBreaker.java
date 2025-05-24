import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;


//Main game class that creates the game window
public class BallBreaker extends JFrame {   
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;

    public BallBreaker() {
        setTitle("Ball Breaker");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        
        GamePanel gamePanel = new GamePanel();
        add(gamePanel);
        
        gamePanel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BallBreaker game = new BallBreaker();
            game.setVisible(true);
        });
    }
}

// Game panel that handles all game logic and rendering
class GamePanel extends JPanel {

    // Constants
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int BOARD_Y = 525;
    private static final int BOARD_WIDTH = 150;
    private static final int BOARD_HEIGHT = 15;
    private static final int BALL_SIZE = 20;
    private static final int HEADER_HEIGHT = 72;
    
    private static final double SPEED_INCREASE = 0.5;
    private static final double BOARD_ACCELERATION = 0.8;
    private static final double BOARD_MAX_SPEED = 15.0;
    private static final double BOARD_SLOW_DOWN = 0.9;
    
    // Colors
    private final Color backgroundColor = new Color(18, 18, 18);
    private final Color boardColor = new Color(0, 150, 255);
    private final Color ballColor = new Color(255, 255, 255);
    private final Color textColor = new Color(255, 255, 255);
    private final Color headerBackGroundColor = new Color(30, 30, 30);
    private final Color gridColor = new Color(40, 40, 40);
    private final Color stoneColor = new Color(255, 50, 50);
    private final Color overlayColor = new Color(0, 0, 0, 150);
    private final Color gameOverOverlayColor = new Color(0, 0, 0, 200);
    
    // Game state
    private int boardX = (WINDOW_WIDTH - BOARD_WIDTH) / 2;
    private double mainBallSpeed = 6.9;
    private double boardSpeed = 0;

    // Player input 
    private boolean movingLeft = false;
    private boolean movingRight = false;

    // Ball position (Default: center)
    private double ballX = (WINDOW_WIDTH - BALL_SIZE) / 2;
    private double ballY = (WINDOW_HEIGHT - BALL_SIZE) / 2 - 72;

    // The horizontal and vertical direction of the ball (its speed).
    private double ballSpeedX = 0;
    private double ballSpeedY = mainBallSpeed;
    
    // Game stats
    private int score = 0;
    private int level = 1;
    private String lives = "❤️❤️❤️";
    
    // Game flow control
    private boolean gameStarted = false;
    private boolean isPaused = false;
    private boolean isGameOver = false;
    
    // This is the list of stones that the ball breaks.
    private List<Stone> stones = new ArrayList<>();
     
    public GamePanel() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        
        initializeStones();
        setupKeyListeners();
        startGameLoop();
    }

    private void setupKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e.getKeyCode());
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyRelease(e.getKeyCode());
            }
        });
    }

    private void startGameLoop() {
        Timer timer = new Timer(16, e -> updateGame());
        timer.start(); 
    }

    private void handleKeyPress(int keyCode) {
        if (keyCode == KeyEvent.VK_LEFT) movingLeft = true;
        if (keyCode == KeyEvent.VK_RIGHT) movingRight = true;
        if (keyCode == KeyEvent.VK_SPACE) toggleGameState();
        if (keyCode == KeyEvent.VK_R && isGameOver) resetGame();
    }

    private void handleKeyRelease(int keyCode) {
        if (keyCode == KeyEvent.VK_LEFT) movingLeft = false;
        if (keyCode == KeyEvent.VK_RIGHT) movingRight = false;
    }

    private void toggleGameState() {
        if (!gameStarted) gameStarted = true;
        else if (!isGameOver) isPaused = !isPaused;
    }

    private void updateGame() {
        if (gameStarted && !isPaused && !isGameOver) {
            updateBoard();
            updateBall();

            checkBoardCollision();
            checkStoneCollisions();
            checkBottomBorder();

            checkLevelCompletion();
        }
        repaint();
    }

    private void updateBoard() {
        if (movingLeft) boardSpeed -= BOARD_ACCELERATION;
        if (movingRight) boardSpeed += BOARD_ACCELERATION;
        
        boardSpeed = Math.max(-BOARD_MAX_SPEED, Math.min(BOARD_MAX_SPEED, boardSpeed * BOARD_SLOW_DOWN));
        boardX = (int) Math.max(0, Math.min(WINDOW_WIDTH - BOARD_WIDTH -15, boardX + boardSpeed));
    }

    private void updateBall() {
        ballX += ballSpeedX;
        ballY += ballSpeedY;
        handleWallCollisions();
    }

    private void handleWallCollisions() {
        if (ballX <= 0 || ballX >= WINDOW_WIDTH - 36) {
            ballSpeedX *= -1; // → Multiply the speed by -1 to change direction (turn around).
        }
        if (ballY <= HEADER_HEIGHT) {
            ballSpeedY *= -1; // → Multiply the speed by -1 to change direction (turn around).
        }
    }

    private void checkBoardCollision() {
        Rectangle ballRect = new Rectangle((int)ballX, (int)ballY, BALL_SIZE, BALL_SIZE);
        Rectangle boardRect = new Rectangle(boardX, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT);
        
        if (ballRect.intersects(boardRect)) {
            double hitPosition = (ballX + (BALL_SIZE/2)) - (boardX + (BOARD_WIDTH/2));
            ballSpeedX = hitPosition / 15.0;
            ballSpeedY = -Math.abs(ballSpeedY);
        }
    }
    

    private void checkStoneCollisions() {
        Rectangle ballRect = new Rectangle((int)ballX, (int)ballY, BALL_SIZE, BALL_SIZE);
        
        for (Stone stone : new ArrayList<>(stones)) {
            if (stone.isActive() && ballRect.intersects(stone.getBounds())) {
                ballSpeedY *= -1;
                stone.destroy();
                score += 10;
            }
        }
    }
    

    private void checkBottomBorder() {
        if (ballY >= WINDOW_HEIGHT) {
            updateLives();
            if (!isGameOver) {
                resetBall();
            }
        }
    }
    

    private void updateLives() {
        switch (lives) {
            case "❤️❤️❤️" -> {
                lives = "❤️❤️💔";
                resetBoard();
            }
            case "❤️❤️💔" -> {
                lives = "❤️💔💔";
                resetBoard();
            }
            case "❤️💔💔" -> {
                lives = "💔💔💔";
                isGameOver = true;
            }
        }
    }
    
    

    private void checkLevelCompletion() {
        boolean allDestroyed = true;
        
        for (Stone stone : stones) {
            if (stone.isActive()) {
                allDestroyed = false;
                break;
            }
        }
        
        if (allDestroyed) {
            if (level < 10) {
                level++;
                initializeStones();
                resetBall();
                resetBoard();
            } else {
                isGameOver = true;
            }
        }
    }
    

    private void resetBall() {
        ballX = (WINDOW_WIDTH - BALL_SIZE) / 2;
        ballY = (WINDOW_HEIGHT - BALL_SIZE) / 2 - 77;
        ballSpeedX = (Math.random() * 4) - 2;
        ballSpeedY = mainBallSpeed;
    }

    private void resetBoard() {
        boardX = (WINDOW_WIDTH - BOARD_WIDTH) / 2;
        boardSpeed = 0;
    }
    

    private void resetGame() {
        score = 0;
        lives = "❤️❤️❤️";
        isGameOver = false;
        level = 1;
        initializeStones();
        resetBall();
        resetBoard();
        gameStarted = false;
        isPaused = false;
    }

    private void initializeStones() {
        stones.clear();
        
        int rows = level;
        int cols = 10;
        int stoneWidth = 65;
        int stoneHeight = 25;
        int startY = 100;
        int padding = 10;
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int x = 20 + j * (stoneWidth + padding);
                int y = startY + i * (stoneHeight + padding);
                stones.add(new Stone(x, y, stoneWidth, stoneHeight));
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        drawBackground(g2d);
        drawStones(g2d);
        drawBoard(g2d);
        drawBall(g2d);
        drawUI(g2d);
        drawGameStateMessages(g2d);
    }
 
    private void drawBackground(Graphics2D g2d) {
        // Background
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Grid
        g2d.setColor(gridColor);
        for (int i = 0; i < WINDOW_WIDTH; i += 40) {
            g2d.drawLine(i, 0, i, WINDOW_HEIGHT);
        }
        for (int i = 0; i < WINDOW_HEIGHT; i += 40) {
            g2d.drawLine(0, i, WINDOW_WIDTH, i);
        }
    }
 
    private void drawStones(Graphics2D g2d) {
        g2d.setColor(stoneColor);
        for (Stone stone : stones) {
            if (stone.isActive()) {
                g2d.fillRect(stone.x, stone.y, stone.width, stone.height);
            }
        }
    }

    private void drawBoard(Graphics2D g2d) {
        g2d.setColor(boardColor);
        g2d.fillRoundRect(boardX, BOARD_Y, BOARD_WIDTH, BOARD_HEIGHT, 10, 10);
    }

    private void drawBall(Graphics2D g2d) {
        g2d.setColor(ballColor);
        g2d.fillOval((int)ballX, (int)ballY, BALL_SIZE, BALL_SIZE);
    }

    private void drawUI(Graphics2D g2d) {
        // Header background
        GradientPaint gradient = new GradientPaint(0, 0, headerBackGroundColor, 0, HEADER_HEIGHT, headerBackGroundColor.darker());
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WINDOW_WIDTH, HEADER_HEIGHT);
        
        // Score
        g2d.setColor(textColor);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 20, 30);
        
        // Level
        String levelText = "Level: " + level;
        int levelX = (WINDOW_WIDTH - g2d.getFontMetrics().stringWidth(levelText)) / 2;
        g2d.drawString(levelText, levelX, 30);
        
        // Lives
        g2d.setFont(new Font("Segoe UI Emoji", Font.BOLD, 24));
        g2d.drawString(lives, WINDOW_WIDTH - 120, 40);
    }
    

    private void drawGameStateMessages(Graphics2D g2d) {
        if (!gameStarted || isPaused) {
            String message = isPaused ? "Press SPACE to Resume" : "Press SPACE to Start";
            drawCenteredMessage(g2d, message, 30);
        }
        
        if (isGameOver) {
            drawGameOverScreen(g2d);
        }
    }
    

    private void drawCenteredMessage(Graphics2D g2d, String message, int yOffset) {
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 30));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (WINDOW_WIDTH - fm.stringWidth(message)) / 2;
        int y = WINDOW_HEIGHT/2 + yOffset;
        
        // Message background
        g2d.setColor(overlayColor);
        g2d.fillRoundRect(x - 20, y - 40, fm.stringWidth(message) + 40, 60, 20, 20);
        
        // Message text
        g2d.setColor(textColor);
        g2d.drawString(message, x, y);
    }
    

    private void drawGameOverScreen(Graphics2D g2d) {
        // Dark overlay
        g2d.setColor(gameOverOverlayColor);
        g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        // Game over title
        drawCenteredMessage(g2d, "GAME OVER!", -80);
        
        // Stats
        String[] stats = {
            "Score: " + score,
            "Level: " + level,
            "Press R to Restart"
        };
        
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
        int startY = WINDOW_HEIGHT/2 - 30;
        
        for (int i = 0; i < stats.length; i++) {
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WINDOW_WIDTH - fm.stringWidth(stats[i])) / 2;
            int y = startY + (i * 40);
            g2d.drawString(stats[i], x, y);
        }
    }
    

    private static class Stone {
        int x, y, width, height;
        private boolean active;
        
        public Stone(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.active = true;
        }
        
        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
        
         public boolean isActive() {
            return active;
        }
        
        public void destroy() {
            this.active = false;
        }
    }
}