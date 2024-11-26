import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Stack;

// GomokuGame 클래스: 2인용 오목 게임을 구현하는 JFrame 기반 프로그램
public class GomokuGame extends JFrame {
    private static final int BOARD_SIZE = 15; // 오목판의 크기 (15x15)
    private static final int CELL_SIZE = 40; // 각 셀의 픽셀 크기
    private char[][] board = new char[BOARD_SIZE][BOARD_SIZE]; // 오목판 상태를 저장하는 배열
    private boolean isPlayer1Turn = true; // 플레이어 턴 여부 (true: Player 1, false: Player 2)
    private Stack<int[]> history = new Stack<>(); // 이전 수를 저장하는 스택 (Undo 기능에 사용)

    // GomokuGame 생성자
    public GomokuGame() {
        setTitle("2-Player Gomoku Game"); // 창 제목 설정
        setSize(BOARD_SIZE * CELL_SIZE + 100, BOARD_SIZE * CELL_SIZE + 100); // 창 크기 설정
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // 창 닫기 동작 설정

        // 오목판 초기화
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = '.'; // 빈 셀은 '.'로 초기화
            }
        }

        GamePanel gamePanel = new GamePanel(); // 게임판을 그릴 패널 생성
        add(gamePanel, BorderLayout.CENTER); // 중앙에 게임판 추가

        JPanel controlPanel = new JPanel(); // 하단 컨트롤 패널 생성
        JButton undoButton = new JButton("Undo"); // Undo 버튼 생성
        undoButton.addActionListener(e -> { // Undo 버튼 클릭 시 동작 정의
            undoMove();
            gamePanel.repaint(); // 화면 갱신
        });
        controlPanel.add(undoButton); // 컨트롤 패널에 Undo 버튼 추가

        add(controlPanel, BorderLayout.SOUTH); // 하단에 컨트롤 패널 추가
    }

    // 이전 수를 되돌리는 메서드
    private void undoMove() {
        if (!history.isEmpty()) { // 이전 수가 있는 경우
            int[] lastMove = history.pop(); // 스택에서 마지막 수를 꺼냄
            board[lastMove[0]][lastMove[1]] = '.'; // 해당 위치를 빈칸으로 만듦
            isPlayer1Turn = !isPlayer1Turn; // 턴을 이전 플레이어로 되돌림
        } else {
            JOptionPane.showMessageDialog(this, "No moves to undo!"); // 되돌릴 수 없음을 알림
        }
    }

    // 승리 조건을 검사하는 메서드
    private boolean checkWin(int row, int col, char symbol) {
        int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}}; // 검사할 방향 (가로, 세로, 대각선)
        for (int[] dir : directions) {
            int count = 1; // 현재 위치의 돌 포함

            // 해당 방향으로 양쪽을 검사
            for (int d = -1; d <= 1; d += 2) { // -1: 한쪽 방향, +1: 반대 방향
                int r = row, c = col;
                while (true) {
                    r += dir[0] * d; // 방향에 따른 행 이동
                    c += dir[1] * d; // 방향에 따른 열 이동
                    if (r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE && board[r][c] == symbol) {
                        count++; // 같은 돌이 이어지면 카운트 증가
                    } else {
                        break; // 다른 돌이 나오면 중단
                    }
                }
            }
            if (count >= 5) return true; // 5개 이상 이어지면 승리
        }
        return false; // 승리 조건 미충족
    }

    // 금수(3x3) 여부를 검사하는 메서드 (현재는 비활성화된 상태)
    private boolean isForbidden(int row, int col, char symbol) {
        // 향후 3x3 검사 로직 추가 가능
        return false; // 기본적으로 금수가 없다고 반환
    }

    // 게임판을 그리는 패널 클래스
    private class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // 클릭한 위치를 격자 좌표로 변환
                    int col = Math.round((float) e.getX() / CELL_SIZE);
                    int row = Math.round((float) e.getY() / CELL_SIZE);

                    // 잘못된 클릭 처리
                    if (row >= BOARD_SIZE || col >= BOARD_SIZE || board[row][col] != '.') {
                        return; // 이미 돌이 있거나 범위를 벗어난 경우 무시
                    }

                    char currentSymbol = isPlayer1Turn ? 'X' : 'O'; // 현재 플레이어의 돌
                    if (isForbidden(row, col, currentSymbol)) { // 금수 검사
                        JOptionPane.showMessageDialog(GomokuGame.this, "Forbidden move (3x3 rule)!"); // 금수 경고
                        return;
                    }

                    board[row][col] = currentSymbol; // 오목판에 돌 추가
                    history.push(new int[]{row, col}); // 수 기록

                    if (checkWin(row, col, currentSymbol)) { // 승리 검사
                        repaint();
                        JOptionPane.showMessageDialog(GomokuGame.this, (isPlayer1Turn ? "Player 1" : "Player 2") + " wins!"); // 승리 메시지
                        System.exit(0); // 게임 종료
                    }

                    isPlayer1Turn = !isPlayer1Turn; // 턴 교체
                    repaint(); // 화면 갱신
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 오목판 그리기
            g.setColor(Color.BLACK);
            for (int i = 0; i <= BOARD_SIZE; i++) {
                g.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE); // 세로선
                g.drawLine(0, i * CELL_SIZE, BOARD_SIZE * CELL_SIZE, i * CELL_SIZE); // 가로선
            }

            // 돌 그리기
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == 'X') { // Player 1의 돌
                        g.setColor(Color.BLACK);
                        g.fillOval(j * CELL_SIZE - CELL_SIZE / 4, i * CELL_SIZE - CELL_SIZE / 4, CELL_SIZE / 2, CELL_SIZE / 2);
                    } else if (board[i][j] == 'O') { // Player 2의 돌
                        g.setColor(Color.WHITE);
                        g.fillOval(j * CELL_SIZE - CELL_SIZE / 4, i * CELL_SIZE - CELL_SIZE / 4, CELL_SIZE / 2, CELL_SIZE / 2);
                    }
                }
            }
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() { // UI 스레드에서 실행
            @Override
            public void run() {
                GomokuGame game = new GomokuGame(); // 게임 실행
                game.setVisible(true); // 창 표시
            }
        });
    }
}
