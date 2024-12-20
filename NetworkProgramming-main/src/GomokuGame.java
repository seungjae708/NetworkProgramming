import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Stack;

public class GomokuGame extends JFrame {
    private static final int BOARD_SIZE = 15; // 오목판 크기
    private static final int CELL_SIZE = 40; // 셀 크기
    private char[][] board = new char[BOARD_SIZE][BOARD_SIZE]; // 오목판 상태 저장
    private boolean isPlayer1Turn = true; // 현재 턴 여부
    private boolean isUndoRequested = false; // 무르기 요청 여부
    private Stack<int[]> history = new Stack<>(); // 수 기록

    public GomokuGame() {
        setTitle("2-Player Gomoku Game"); // 창 제목
        setSize(BOARD_SIZE * CELL_SIZE + 150, BOARD_SIZE * CELL_SIZE + 150); // 창 크기
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 닫기 동작
        setLayout(new BorderLayout());

        // 오목판 초기화
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = '.';
            }
        }

        GamePanel gamePanel = new GamePanel(); // 게임판 패널
        add(gamePanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(); // 버튼 패널
        controlPanel.setLayout(new FlowLayout());

        // 무르기 요청 버튼
        JButton undoRequestButton = new JButton("Request Undo");
        undoRequestButton.addActionListener(e -> {
            if (isUndoRequested) {
                JOptionPane.showMessageDialog(this, "Undo already requested!");
                return;
            }
            isUndoRequested = true;
            JOptionPane.showMessageDialog(this, "Undo request sent. Waiting for opponent's response.");
        });
        controlPanel.add(undoRequestButton);

        // 무르기 허용 버튼
        JButton allowUndoButton = new JButton("Allow Undo");
        allowUndoButton.addActionListener(e -> {
            if (isUndoRequested) {
                undoMove();
                isUndoRequested = false;
                gamePanel.repaint();
            } else {
                JOptionPane.showMessageDialog(this, "No undo request to allow!");
            }
        });
        controlPanel.add(allowUndoButton);

        add(controlPanel, BorderLayout.SOUTH);
    }

    // 무르기 기능
    private void undoMove() {
        if (!history.isEmpty()) {
            int[] lastMove = history.pop();
            board[lastMove[0]][lastMove[1]] = '.';
            isPlayer1Turn = !isPlayer1Turn;
            JOptionPane.showMessageDialog(this, "Undo performed!");
        } else {
            JOptionPane.showMessageDialog(this, "No moves to undo!");
        }
    }

    // 승리 검사
    private boolean checkWin(int row, int col, char symbol) {
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] dir : directions) {
            int count = 1;

            for (int d = -1; d <= 1; d += 2) {
                int r = row, c = col;
                while (true) {
                    r += dir[0] * d;
                    c += dir[1] * d;
                    if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == symbol) {
                        count++;
                    } else {
                        break;
                    }
                }
            }
            if (count >= 5) return true;
        }
        return false;
    }

    // 3x3 금수 검사
    private boolean isForbidden(int row, int col, char symbol) {
        int openThreeCount = countOpenThrees(row, col, symbol);
        return openThreeCount >= 2;
    }

    // 열린 삼(3x3) 개수 계산
    private int countOpenThrees(int row, int col, char symbol) {
        int count = 0;
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

        for (int[] dir : directions) {
            int consecutive = 1;
            boolean openStart = false, openEnd = false;

            int r = row - dir[0], c = col - dir[1];
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == symbol) {
                consecutive++;
                r -= dir[0];
                c -= dir[1];
            }
            if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == '.') {
                openStart = true;
            }

            r = row + dir[0];
            c = col + dir[1];
            while (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == symbol) {
                consecutive++;
                r += dir[0];
                c += dir[1];
            }
            if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == '.') {
                openEnd = true;
            }

            if (consecutive == 3 && openStart && openEnd) {
                count++;
            }
        }
        return count;
    }

    // 게임판 패널
    private class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int col = e.getX() / CELL_SIZE;
                    int row = e.getY() / CELL_SIZE;

                    if (row >= BOARD_SIZE || col >= BOARD_SIZE || board[row][col] != '.') return;

                    char currentSymbol = isPlayer1Turn ? 'X' : 'O';
                    if (isForbidden(row, col, currentSymbol)) {
                        JOptionPane.showMessageDialog(GomokuGame.this, "Forbidden move (3x3 rule)!");
                        return;
                    }

                    board[row][col] = currentSymbol;
                    history.push(new int[]{row, col});

                    if (checkWin(row, col, currentSymbol)) {
                        repaint();
                        JOptionPane.showMessageDialog(GomokuGame.this, (isPlayer1Turn ? "Player 1" : "Player 2") + " wins!");
                        System.exit(0);
                    }

                    isPlayer1Turn = !isPlayer1Turn;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.setColor(Color.BLACK);
            for (int i = 0; i <= BOARD_SIZE; i++) {
                g.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE);
                g.drawLine(0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE, i * CELL_SIZE);
            }

            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == 'X') {
                        g.setColor(Color.BLACK);
                        g.fillOval(j * CELL_SIZE + CELL_SIZE / 4, i * CELL_SIZE + CELL_SIZE / 4, CELL_SIZE / 2, CELL_SIZE / 2);
                    } else if (board[i][j] == 'O') {
                        g.setColor(Color.WHITE);
                        g.fillOval(j * CELL_SIZE + CELL_SIZE / 4, i * CELL_SIZE + CELL_SIZE / 4, CELL_SIZE / 2, CELL_SIZE / 2);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GomokuGame game = new GomokuGame();
            game.setVisible(true);
        });
    }
}
