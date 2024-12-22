import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GomokuClient {
    private static final int BOARD_SIZE = 19;
    private static final int CELL_SIZE = 30;
    private char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
    private String playerRole = "";
    private DataInputStream input;
    private DataOutputStream output;
    private GamePanel gamePanel;
    private JFrame frame;
    private boolean isPlayerTurn = false;
    private JTextArea chatArea;
    private JTextField messageField;
    private JLabel timerLabel;
    private Timer timer;
    private int timeLeft = 20;
    private boolean undoRequested = false;

    public GomokuClient(String serverAddress, int port) {
        try {
            Socket socket = new Socket(serverAddress, port);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            playerRole = input.readUTF();

            frame = new JFrame("Gomoku Client - " + playerRole);
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);

            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    board[i][j] = '.';
                }
            }

            gamePanel = new GamePanel();

            JPanel mainPanel = new JPanel(null);
            JLabel background = new JLabel();
            try {
                ImageIcon bgIcon = new ImageIcon(getClass().getResource("/images/background.jpg"));
                Image scaledImage = bgIcon.getImage().getScaledInstance(1000, 700, Image.SCALE_SMOOTH);
                background.setIcon(new ImageIcon(scaledImage));
            } catch (Exception e) {
                System.err.println("배경 이미지를 로드할 수 없습니다: " + e.getMessage());
            }
            background.setBounds(0, 0, 1000, 700);
            mainPanel.add(background);

            gamePanel.setBounds(50, 50, 570, 570);
            background.add(gamePanel);

            JPanel infoPanel = createInfoPanel();
            infoPanel.setBounds(650, 50, 300, 200);
            background.add(infoPanel);

            JPanel chatPanel = createChatPanel();
            chatPanel.setBounds(650, 300, 300, 350);
            background.add(chatPanel);

            frame.add(mainPanel);
            frame.setVisible(true);
            frame.setResizable(false);

            new Thread(this::listenToServer).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMove(int row, int col) {
        if (!isPlayerTurn) {
            JOptionPane.showMessageDialog(frame, "상대방의 턴입니다!", "오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            output.writeUTF(row + "," + col); // 서버로 좌표 전송
            resetTimer(); // 타이머 리셋
            isPlayerTurn = false; // 턴 종료
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void startTimer() {
        timeLeft = 20;
        timerLabel.setText(String.valueOf(timeLeft));
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText(String.valueOf(timeLeft));
            if (timeLeft == 0) {
                if (isPlayerTurn) { // 자신의 턴일 때만 랜덤 수 두기
                    makeRandomMove();
                }
            }
        });
        timer.start();
    }

    private void resetTimer() {
        if (timer != null) {
            timer.stop();
        }
        timeLeft = 20;
        timerLabel.setText(String.valueOf(timeLeft));
    }


    private void makeRandomMove() {
        // 빈 칸의 좌표를 수집
        List<int[]> emptyCells = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == '.') { // 빈 칸이면 좌표를 저장
                    emptyCells.add(new int[]{i, j});
                }
            }
        }

        // 빈 칸이 없으면 종료
        if (emptyCells.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "더 이상 놓을 수 있는 위치가 없습니다!");
            return;
        }

        // 랜덤하게 빈 칸 중 하나 선택
        Random random = new Random();
        int[] randomCell = emptyCells.get(random.nextInt(emptyCells.size()));

        // 선택한 좌표에 돌을 둠
        int row = randomCell[0];
        int col = randomCell[1];
        board[row][col] = (playerRole.equals("Player 1") ? 'X' : 'O'); // 자신의 돌 놓기
        gamePanel.repaint(); // 화면 갱신

        try {
            output.writeUTF(row + "," + col); // 서버에 위치 전송
            isPlayerTurn = false; // 차례 넘기기
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        resetTimer(); // 타이머 초기화
    }

    private void listenToServer() {
        try {
            while (true) {
                String message = input.readUTF();

                // 채팅 메시지는 항상 즉시 표시
                if (message.startsWith("CHAT:")) {
                    // 자신이 보낸 메시지는 다시 출력하지 않음
                    String player = message.split(":")[1].trim(); // 메시지에서 Player 정보를 추출
                    if (!playerRole.equals(player)) {
                        SwingUtilities.invokeLater(() -> {
                            chatArea.append(message.substring(5) + "\n");
                        });
                    }
                    continue;
                }

                if (message.startsWith("Current board:")) {
                    updateBoard(message);
                    gamePanel.repaint();
                } else if (message.equals("Forbidden move! Try again.")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "금지된 수입니다! 다시 시도하세요.", "경고", JOptionPane.WARNING_MESSAGE);
                    });
                    isPlayerTurn = true; // 차례 유지
                } else if (message.equals("UNDO_RESPONSE_REQUIRED")) {
                    int response = JOptionPane.showConfirmDialog(
                            frame,
                            "상대방이 무르기를 요청했습니다. 허락하시겠습니까?",
                            "무르기 요청",
                            JOptionPane.YES_NO_OPTION
                    );
                    try {
                        if (response == JOptionPane.YES_OPTION) {
                            output.writeUTF("UNDO_ACCEPTED");
                            resetTimer(); // 타이머 리셋
                        } else {
                            output.writeUTF("UNDO_REJECTED");
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else if (message.equals("UNDO_SUCCESSFUL")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "무르기가 수락되었습니다.");
                        undoRequested = false;
                        isPlayerTurn = true;
                        resetTimer();
                        startTimer();
                    });
                } else if (message.equals("UNDO_REJECTED")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "상대방이 무르기를 거절했습니다.");
                        undoRequested = false;
                    });
                } else if (message.equals("Your turn.")) {
                    boolean previousTurn = isPlayerTurn;
                    isPlayerTurn = true;
                    resetTimer();
                    startTimer();

                    // 실제로 턴이 변경될 때만 메시지 표시
                    if (!previousTurn) {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(frame, "[" + playerRole + "] Your turn!", "Game Alert", JOptionPane.INFORMATION_MESSAGE));
                    }
                } else if (message.equals("Opponent turn.")) {
                    isPlayerTurn = false;
                    resetTimer();
                } else {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(frame, message, "Game Status", JOptionPane.INFORMATION_MESSAGE));
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

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(null);
        infoPanel.setOpaque(false);

        JPanel timerPanel = new JPanel();
        timerPanel.setBounds(50, 20, 200, 60);
        timerPanel.setLayout(null);
        timerPanel.setBackground(new Color(255, 248, 220));
        timerPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 182, 193), 3));

        timerLabel = new JLabel("20", SwingConstants.CENTER);
        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 30));
        timerLabel.setForeground(new Color(255, 105, 180));
        timerLabel.setBounds(10, 10, 180, 40);
        timerPanel.add(timerLabel);
        infoPanel.add(timerPanel);

        JLabel roleLabel = new JLabel("Role: " + playerRole, SwingConstants.CENTER);
        roleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        roleLabel.setForeground(new Color(255, 105, 180));
        roleLabel.setBounds(20, 100, 260, 40);
        infoPanel.add(roleLabel);


        return infoPanel;
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(null);
        chatPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 182, 193), 3));
        chatPanel.setBackground(new Color(255, 248, 220));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(255, 240, 245));
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        chatArea.setBorder(BorderFactory.createLineBorder(new Color(255, 105, 180), 2));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBounds(10, 10, 280, 200);
        chatPanel.add(chatScrollPane);

        messageField = new JTextField();
        messageField.setBounds(10, 220, 200, 30);
        messageField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createLineBorder(new Color(255, 105, 180), 2));
        chatPanel.add(messageField);

        JButton sendButton = new JButton("전송");
        sendButton.setBounds(220, 220, 70, 30);
        sendButton.setBackground(new Color(255, 240, 245));
        sendButton.setForeground(new Color(255, 105, 180));
        sendButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        sendButton.setBorder(BorderFactory.createLineBorder(new Color(255, 105, 180), 2));
        sendButton.addActionListener(e -> sendMessage());
        chatPanel.add(sendButton);

        JButton undoButton = new JButton("무르기 요청");
        undoButton.setBounds(10, 260, 280, 30);
        undoButton.setBackground(new Color(255, 192, 203));
        undoButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        undoButton.addActionListener(e -> {
            if (!isPlayerTurn) {
                JOptionPane.showMessageDialog(frame, "자신의 턴에만 무르기를 요청할 수 있습니다.");
                return;
            }
            if (!undoRequested) {
                try {
                    output.writeUTF("UNDO_REQUEST");
                    undoRequested = true;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(frame, "이미 무르기를 요청했습니다.");
            }
        });
        chatPanel.add(undoButton);

        return chatPanel;
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            try {
                // 메시지를 서버로 전송
                output.writeUTF("CHAT:" + playerRole + ": " + message);

                // 메시지를 자신의 채팅창에 추가
                chatArea.append(playerRole + ": " + message + "\n");

                // 메시지 입력창 초기화
                messageField.setText("");
            } catch (Exception ex) {
                ex.printStackTrace();
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
            setOpaque(false);

            // 이미지 로드
            blackStone = new ImageIcon(getClass().getResource("/images/black.png")).getImage();
            whiteStone = new ImageIcon(getClass().getResource("/images/white.png")).getImage();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // 클릭 이벤트를 SwingUtilities.invokeLater로 래핑
                    SwingUtilities.invokeLater(() -> {
                        // 턴 검사를 가장 먼저 수행
                        if (!isPlayerTurn) {
                            JOptionPane.showMessageDialog(frame,
                                    "상대방의 턴입니다!",
                                    "차례 오류",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        int x = e.getX();
                        int y = e.getY();

                        int col = (x - PADDING + CELL_SIZE / 2) / CELL_SIZE;
                        int row = (y - PADDING + CELL_SIZE / 2) / CELL_SIZE;

                        // 유효한 위치인지 검사
                        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
                            JOptionPane.showMessageDialog(frame,
                                    "보드 범위를 벗어났습니다!",
                                    "위치 오류",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        // 이미 돌이 놓여있는지 검사
                        if (board[row][col] != '.') {
                            JOptionPane.showMessageDialog(frame,
                                    "이미 돌이 놓여있는 위치입니다!",
                                    "위치 오류",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        // 모든 검증을 통과한 경우에만 handleMove 호출
                        handleMove(row, col);
                    });
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            g2d.setColor(new Color(245, 222, 179));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i < BOARD_SIZE; i++) {
                int x = PADDING + i * CELL_SIZE;
                int y = PADDING + i * CELL_SIZE;
                g.drawLine(PADDING, y, PADDING + (BOARD_SIZE - 1) * CELL_SIZE, y);
                g.drawLine(x, PADDING, x, PADDING + (BOARD_SIZE - 1) * CELL_SIZE);
            }

            int stoneSize = (int) (CELL_SIZE * 0.8);
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    int x = PADDING + j * CELL_SIZE - stoneSize / 2;
                    int y = PADDING + i * CELL_SIZE - stoneSize / 2;
                    if (board[i][j] == 'X') {
                        g.drawImage(blackStone, x, y, stoneSize, stoneSize, this);
                    } else if (board[i][j] == 'O') {
                        g.drawImage(whiteStone, x, y, stoneSize, stoneSize, this);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        new GomokuClient("localhost", 5000);
    }
}
