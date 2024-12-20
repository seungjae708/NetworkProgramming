import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;

public class GomokuClient {
    private static final int BOARD_SIZE = 19;
    private static final int CELL_SIZE = 30; // 각 셀의 크기
    private char[][] board = new char[BOARD_SIZE][BOARD_SIZE]; // 게임 보드 상태
    private String playerRole = ""; // 플레이어 역할 (Player 1 또는 Player 2)
    private DataInputStream input;
    private DataOutputStream output;
    private GamePanel gamePanel; // 게임 패널 참조
    private JFrame frame; // 메인 프레임
    private boolean isPlayerTurn = false; // 클라이언트 턴 여부 추적

    public GomokuClient(String serverAddress, int port) {
        try {
            // 서버와 연결
            Socket socket = new Socket(serverAddress, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            // 역할 정보 수신
            playerRole = input.readUTF(); // 서버에서 역할 정보 수신 (Player 1 또는 Player 2)

            // GUI 생성
            frame = new JFrame("Gomoku Client - " + playerRole); // 프레임 제목에 역할 표시
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(BOARD_SIZE * CELL_SIZE + 200, BOARD_SIZE * CELL_SIZE + 100);

            // 보드 초기화
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    board[i][j] = '.';
                }
            }

            gamePanel = new GamePanel(); // 커스터마이즈된 패널
            SidePanel sidePanel = new SidePanel(); // 오른쪽 패널

            // 프레임 레이아웃 설정
            frame.setLayout(new BorderLayout());
            frame.add(gamePanel, BorderLayout.CENTER); // 게임 패널을 중앙에 배치
            frame.add(sidePanel, BorderLayout.EAST);   // 오른쪽 패널 배치

            frame.setVisible(true);
            frame.setResizable(false);

            // 서버로부터 데이터를 수신하는 스레드
            new Thread(this::listenToServer).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMove(int row, int col) {
        try {
            output.writeUTF(row + "," + col); // 서버로 좌표 전송
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenToServer() {
        try {
            while (true) {
                String message = input.readUTF(); // 서버로부터 메시지 수신

                if (message.startsWith("Current board:")) {
                    updateBoard(message); // 보드 상태 업데이트
                    gamePanel.repaint(); // 화면 갱신
                } else if (message.startsWith("Forbidden move")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, message, "Invalid Move", JOptionPane.WARNING_MESSAGE);
                    });
                    // 금지된 이동 후 턴 복구
                    isPlayerTurn = true; // 플레이어가 다시 이동할 수 있도록 설정
                } else if (message.equals("Your turn.")) {
                    isPlayerTurn = true; // 본인의 턴으로 설정
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "[" + playerRole + "] Your turn!", "Game Alert", JOptionPane.INFORMATION_MESSAGE);
                    });
                } else if (message.startsWith("Invalid move")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, message, "Invalid Move", JOptionPane.WARNING_MESSAGE);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, message, "Game Status", JOptionPane.INFORMATION_MESSAGE);
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void updateBoard(String boardState) {
        String[] rows = boardState.split("\n");
        for (int i = 0; i < BOARD_SIZE; i++) {
            char[] cells = rows[i + 1].toCharArray(); // 보드 상태 파싱
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = cells[j]; // 상태 반영
            }
        }
    }

    // 게임판을 그리는 커스터마이즈된 패널 클래스
    private class GamePanel extends JPanel {
        private Image blackStone;
        private Image whiteStone;
        private static final int PADDING = 20; // 바둑판 가장자리 여백

        public GamePanel() {
            setPreferredSize(new Dimension(
                    BOARD_SIZE * CELL_SIZE + PADDING * 2,
                    BOARD_SIZE * CELL_SIZE + PADDING * 2));
            setBackground(new Color(245, 222, 179)); // 바둑판 색상 (베이지톤)

            // 이미지 로드
            blackStone = new ImageIcon(getClass().getResource("/images/black.png")).getImage();
            whiteStone = new ImageIcon(getClass().getResource("/images/white.png")).getImage();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int x = e.getX();
                    int y = e.getY();

                    // 좌표를 교차점 기준으로 변환
                    int col = (x - PADDING + CELL_SIZE / 2) / CELL_SIZE;
                    int row = (y - PADDING + CELL_SIZE / 2) / CELL_SIZE;

                    // 본인의 턴인지 확인
                    if (!isPlayerTurn) {
                        JOptionPane.showMessageDialog(frame, "It's not your turn!", "Game Alert", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // 유효한 좌표인지 확인
                    if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE || board[row][col] != '.') {
                        JOptionPane.showMessageDialog(frame, "Invalid move!", "Game Alert", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // 서버에 좌표 전송
                    handleMove(row, col);
                    isPlayerTurn = false; // 턴 종료
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            g2d.setColor(new Color(235, 190, 125)); // 바둑판 색
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 외곽선 그리기
            g.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2)); // 두꺼운 선
            int boardSizeWithPadding = BOARD_SIZE * CELL_SIZE; // 여백 포함 크기
            g2d.drawRect(10, 10, boardSizeWithPadding-15, boardSizeWithPadding-15); // 여백을 위한 좌표 설정

            // 바둑판 그리기
            g2d.setStroke(new BasicStroke(1)); // 얇은 선
            for (int i = 0; i < BOARD_SIZE; i++) {
                int x = PADDING + i * CELL_SIZE;
                int y = PADDING + i * CELL_SIZE;
                g.drawLine(PADDING, y, PADDING + (BOARD_SIZE - 1) * CELL_SIZE, y); // 가로선
                g.drawLine(x, PADDING, x, PADDING + (BOARD_SIZE - 1) * CELL_SIZE); // 세로선
            }

            // 교차점 강조 (별점 그리기)
            g2d.setColor(Color.BLACK);
            int dotSize = 6; // 점 크기
            int[][] starPoints = { // 정확한 교차점 위치
                    {3, 3}, {3, 15}, {15, 3}, {15, 15}, {9, 9}, {3, 9}, {9, 3}, {9, 15}, {15, 9}
            };
            for (int[] point : starPoints) {
                int x = PADDING + point[0] * CELL_SIZE - dotSize / 2; // 별점의 중심 조정
                int y = PADDING + point[1] * CELL_SIZE - dotSize / 2; // 별점의 중심 조정
                g2d.fillOval(x, y, dotSize, dotSize);
            }

            // 돌 크기: CELL_SIZE * 0.8
            int stoneSize = (int) (CELL_SIZE * 0.8);

            // 돌 그리기
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    int x = PADDING + j * CELL_SIZE - stoneSize / 2;
                    int y = PADDING + i * CELL_SIZE - stoneSize / 2;
                    if (board[i][j] == 'X') { // 검은 돌
                        g.drawImage(blackStone, x, y, stoneSize, stoneSize, this);
                    } else if (board[i][j] == 'O') { // 흰 돌
                        g.drawImage(whiteStone, x, y, stoneSize, stoneSize, this);
                    }
                }
            }
        }
    }



}