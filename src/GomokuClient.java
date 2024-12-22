import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
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
    private JPanel messageArea = new JPanel(); // 기본 초기화
    private JTextField messageField;
    private JLabel timerLabel;
    private JLabel timerLabel1;
    private GamePanel.RoundedButton leftArrow ;
    private GamePanel.RoundedButton rightArrow;
    private Timer timer;
    private int timeLeft = 20;
    private boolean undoRequested = false;
    private final List<TurnRecord> gameHistory = new ArrayList<>();
    private int currentReplayStep = -1; // 현재 재생 중인 단계
    private boolean isGameEnded = false;

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

            gamePanel.setBounds(50, 50, 580, 580);
            background.add(gamePanel);

            JPanel infoPanel = createInfoPanel();
            infoPanel.setBounds(620, 50, 310, 300);
            background.add(infoPanel);

            JPanel chatPanel = createChatPanel();
            chatPanel.setBounds(650, 290, 300, 340);
            background.add(chatPanel);

            leftArrow = new GamePanel.RoundedButton("<",15);
            rightArrow = new GamePanel.RoundedButton(">",15);

            leftArrow.setBounds(645, 75, 50, 50);
            rightArrow.setBounds(920, 75, 50, 50);

            // 버튼 클릭 시 이전/다음 단계로 이동
            leftArrow.addActionListener(e -> showPreviousStep());
            rightArrow.addActionListener(e -> showNextStep());

            leftArrow.setBackground(new Color(255, 182, 193)); // 밝은 회색 배경
            leftArrow.setForeground(Color.BLACK); // 검은 글자
            rightArrow.setBackground(new Color(255, 182, 193)); // 밝은 회색 배경
            rightArrow.setForeground(Color.BLACK); // 검은 글자

            leftArrow.setVisible(false);
            rightArrow.setVisible(false);

            background.add(leftArrow);
            background.add(rightArrow);

            frame.add(mainPanel);
            frame.setVisible(true);
            frame.setResizable(false);

            new Thread(this::listenToServer).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class TurnRecord {
        int row;
        int col;
        char playerSymbol;
        String playerRole;  // 플레이어 구분을 위한 필드 추가

        public TurnRecord(int row, int col, char playerSymbol, String playerRole) {
            this.row = row;
            this.col = col;
            this.playerSymbol = playerSymbol;
            this.playerRole = playerRole;
        }
    }

    private void handleMove(int row, int col) {
        if (currentReplayStep != -1) {
            JOptionPane.showMessageDialog(frame, "기록 재생 중에는 돌을 놓을 수 없습니다!", "경고", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!isPlayerTurn) {
            JOptionPane.showMessageDialog(frame, "상대방의 턴입니다!", "오류", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            char currentSymbol = playerRole.equals("Player 1 (X).") ? 'X' : 'O';
            board[row][col] = currentSymbol;

            // 현재 수를 기록에 추가 (플레이어 구분 정보도 포함)
            gameHistory.add(new TurnRecord(row, col, currentSymbol, playerRole));

            output.writeUTF(row + "," + col);
            resetTimer();
            isPlayerTurn = false;
            gamePanel.repaint();
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
                    String[] parts = message.split(":", 3); // "CHAT:", "Player", "Message"
                    if (parts.length == 3) {
                        String sender = parts[1].trim();
                        String content = parts[2].trim();

                        SwingUtilities.invokeLater(() -> {
                            boolean isOwnMessage = sender.equals(playerRole);
                            addMessage(messageArea, sender + ": " + content, isOwnMessage);
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
                    isPlayerTurn = true;
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
                            resetTimer();
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

                    if (!previousTurn) {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(frame, "[" + playerRole + "] Your turn!", "Game Alert", JOptionPane.INFORMATION_MESSAGE));
                    }
                } else if (message.equals("Opponent turn.")) {
                    isPlayerTurn = false;
                    resetTimer();
                } else if (message.equals("You win!") || message.equals("You lose!") || message.equals("Draw!")) {
                    isGameEnded = true;
                    String finalMessage = message;

                    // 게임 기록 디버깅 출력
                    System.out.println("=== 게임 기록 출력 ===");
                    for (TurnRecord record : gameHistory) {
                        System.out.println("Row = " + record.row + ", Col = " + record.col +
                                ", Symbol = " + record.playerSymbol + ", Role = " + record.playerRole);
                    }
                    System.out.println("===================");

                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, finalMessage, "Game Status", JOptionPane.INFORMATION_MESSAGE);
                        enableReplayMode();
                    });
                }
                else {
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
        timerPanel.setBounds(90, 20, 200, 60);
        timerPanel.setLayout(null);
        timerPanel.setBackground(new Color(255, 248, 220));
        timerPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 182, 193), 3));

        timerLabel = new JLabel("20", SwingConstants.CENTER);
        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 30));
        timerLabel.setForeground(new Color(255, 105, 180));
        timerLabel.setBounds(-10, 10, 180, 40);
        timerPanel.add(timerLabel);
        infoPanel.add(timerPanel);

        timerLabel1 = new JLabel("초", SwingConstants.CENTER);
        timerLabel1.setFont(new Font("맑은 고딕", Font.BOLD, 30));
        timerLabel1.setForeground(new Color(255, 105, 180));
        timerLabel1.setBounds(30, 10, 180, 40);
        timerPanel.add(timerLabel1);
        infoPanel.add(timerPanel);

        JLabel roleLabel = new JLabel("You are " + playerRole, SwingConstants.CENTER);
        roleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        roleLabel.setForeground(new Color(255, 105, 180));
        roleLabel.setBounds(120, 190, 260, 40);
        infoPanel.add(roleLabel);

        // playerRole에 따라 적절한 GIF를 추가
        if (playerRole.equals("Player 1 (X).")) {
            JLabel leftGif = new JLabel(new ImageIcon(getClass().getResource("/images/bunny.gif")));
            leftGif.setBounds(-10, 70, 200, 200); // GIF의 위치와 크기 설정
            infoPanel.add(leftGif);

            // black.png를 리사이즈하여 추가
            ImageIcon blackIcon = new ImageIcon(getClass().getResource("/images/black.png"));
            Image resizedBlack = blackIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(resizedBlack));
            imageLabel.setBounds(200, 140, 50, 50); // 이미지의 위치와 크기 설정
            infoPanel.add(imageLabel);
        } else if (playerRole.equals("Player 2 (O).")) {
            JLabel rightGif = new JLabel(new ImageIcon(getClass().getResource("/images/bear.gif")));
            rightGif.setBounds(-10, 70, 200, 200); // GIF의 위치와 크기 설정
            infoPanel.add(rightGif);

            // white.png를 리사이즈하여 추가
            ImageIcon whiteIcon = new ImageIcon(getClass().getResource("/images/white.png"));
            Image resizedWhite = whiteIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(resizedWhite));
            imageLabel.setBounds(200, 140, 50, 50); // 이미지의 위치와 크기 설정
            infoPanel.add(imageLabel);
        }

        return infoPanel;
    }

    private void enableReplayMode() {
        if (!isGameEnded) return;

        currentReplayStep = gameHistory.size() - 1;

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame,
                    "게임 기록 재생이 가능합니다.\n" +
                            "← : 이전 수 보기\n" +
                            "→ : 다음 수 보기\n\n" +
                            "흑돌(X)과 백돌(O)이 번갈아가며 표시됩니다.",
                    "기록 재생 안내",
                    JOptionPane.INFORMATION_MESSAGE);

            leftArrow.setVisible(true);
            rightArrow.setVisible(true);
        });
    }


    private void showPreviousStep() {
        if (currentReplayStep >= 0) { // 첫 번째 수 이하로 내려가지 않도록 조건 설정
            // 현재 단계의 돌 제거
            TurnRecord record = gameHistory.get(currentReplayStep);
            board[record.row][record.col] = '.'; // 현재 기록의 돌만 제거
            gamePanel.repaint(); // 보드 갱신

            // 이전 단계로 이동
            currentReplayStep--;
        } else {
            JOptionPane.showMessageDialog(frame, "첫 번째 수입니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    private void showNextStep() {
        if (currentReplayStep < gameHistory.size() - 1) { // 마지막 단계 이상으로 넘어가지 않도록 조건 설정
            // 다음 단계로 이동
            currentReplayStep++;

            // 기록에서 돌 추가
            TurnRecord record = gameHistory.get(currentReplayStep);
            board[record.row][record.col] = record.playerSymbol; // 해당 위치에 돌 추가
            gamePanel.repaint(); // 보드 갱신
        } else {
            JOptionPane.showMessageDialog(frame, "마지막 수입니다.", "알림", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(null);
        chatPanel.setBorder(BorderFactory.createLineBorder(new Color(255, 182, 193), 3));
        chatPanel.setBackground(new Color(255, 248, 220));

        // 클래스 멤버 변수인 messageArea를 초기화
        messageArea.setLayout(new BoxLayout(messageArea, BoxLayout.Y_AXIS));
        messageArea.setBackground(new Color(255, 240, 245));

        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setBounds(10, 10, 280, 200);
        messageScrollPane.setBorder(BorderFactory.createLineBorder(new Color(255, 105, 180), 2));
        chatPanel.add(messageScrollPane);

        messageField = new JTextField();
        messageField.setBounds(10, 220, 200, 30);
        messageField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createLineBorder(new Color(255, 105, 180), 2));

        // Enter 키로 메시지를 전송
        messageField.addActionListener(e -> sendMessage());

        // CustomScrollBarUI 적용
        messageScrollPane.getVerticalScrollBar().setUI(new GamePanel.CustomScrollBarUI());
        messageScrollPane.getHorizontalScrollBar().setUI(new GamePanel.CustomScrollBarUI());
        messageScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0)); // 스크롤바 두께 설정

        chatPanel.add(messageField);

        GamePanel.RoundedButton sendButton = new GamePanel.RoundedButton("전송", 15);
        sendButton.setBounds(220, 220, 70, 30);
        sendButton.setBackground(new Color(255, 240, 245));
        sendButton.setForeground(new Color(255, 105, 180));
        sendButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        sendButton.addActionListener(e -> sendMessage());
        chatPanel.add(sendButton);

        GamePanel.RoundedButton undoButton = new GamePanel.RoundedButton("무르기 요청", 15);
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

        // 나가기 버튼 추가
        GamePanel.RoundedButton exitButton = new GamePanel.RoundedButton("나가기", 15);
        exitButton.setBounds(10, 300, 280, 30);
        exitButton.setBackground(new Color(255, 100, 100));
        exitButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        exitButton.setForeground(Color.WHITE);

        // 나가기 버튼의 클릭 동작
        exitButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "정말 나가시겠습니까?",
                    "나가기 확인",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                frame.dispose();
                System.exit(0);
            }
        });

        // 초기에는 보이지 않도록 설정
//        exitButton.setVisible(false);
        chatPanel.add(exitButton);

        // 게임 종료 시 나가기 버튼 보이기
        SwingUtilities.invokeLater(() -> {
            if (isGameEnded) {
                exitButton.setVisible(true);
            }
        });

        return chatPanel;
    }



    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            try {
                // 서버에 메시지 전송
                output.writeUTF("CHAT:" + playerRole + ": " + message);

                // 메시지 입력 필드 초기화
                messageField.setText("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void addMessage(JPanel messageArea, String message, boolean isOwnMessage) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new FlowLayout(isOwnMessage ? FlowLayout.RIGHT : FlowLayout.LEFT));
        messagePanel.setOpaque(false);

        // JTextArea로 변경하여 자동 줄바꿈 적용
        JTextArea messageLabel = new JTextArea(message);
        messageLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 메시지 내부 여백
        messageLabel.setOpaque(true);
        messageLabel.setBackground(isOwnMessage ? new Color(255, 192, 203) : new Color(255, 248, 220));
        messageLabel.setForeground(Color.BLACK);

        // JTextArea 설정
        messageLabel.setEditable(false); // 편집 불가능
        messageLabel.setLineWrap(true); // 줄바꿈 활성화
        messageLabel.setWrapStyleWord(true); // 단어 단위로 줄바꿈

        // 패널에 추가
        messagePanel.add(messageLabel);

        // 메시지 간의 간격을 추가
        messageArea.add(Box.createVerticalStrut(5)); // 메시지 사이에 5픽셀 간격 추가
        messageArea.add(messagePanel);

        // 메시지 영역 갱신
        messageArea.revalidate();
        messageArea.repaint();
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

        static class RoundedButton extends JButton {
            private final int radius;

            public RoundedButton(String text, int radius) {
                super(text);
                this.radius = radius;
                setFocusPainted(false);
                setContentAreaFilled(false);
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 배경 색상 설정
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

                super.paintComponent(g);
                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 테두리 색상 설정
                g2.setColor(getForeground());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

                g2.dispose();
            }
        }


        public static class CustomScrollBarUI extends BasicScrollBarUI {
            @Override
            protected void configureScrollBarColors() {
                thumbColor = new Color(255, 182, 193); // 스크롤 핸들 색상
                trackColor = new Color(255, 240, 245); // 트랙 색상
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 10, 10); // 둥근 모서리
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createArrowButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createArrowButton();
            }

            private JButton createArrowButton() {
                JButton button = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        // 배경색
                        g2.setColor(new Color(255, 192, 203));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                        // 화살표 그리기
                        g2.setColor(new Color(255, 105, 180));
                        int[] xPoints = {getWidth() / 4, getWidth() / 2, getWidth() * 3 / 4};
                        int[] yPoints = {getHeight() / 2 + 2, getHeight() / 4, getHeight() / 2 + 2};
                        g2.fillPolygon(xPoints, yPoints, 3);

                        g2.dispose();
                    }
                };
                button.setPreferredSize(new Dimension(12, 12));
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setContentAreaFilled(false);
                return button;
            }
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            // 배경 그리기
            g2d.setColor(new Color(245, 222, 179)); // 바둑판 배경 색상
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // 바둑판 그리기
            g.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1));
            for (int i = 0; i < BOARD_SIZE; i++) {
                int x = PADDING + i * CELL_SIZE;
                int y = PADDING + i * CELL_SIZE;
                g.drawLine(PADDING, y, PADDING + (BOARD_SIZE - 1) * CELL_SIZE, y); // 가로선
                g.drawLine(x, PADDING, x, PADDING + (BOARD_SIZE - 1) * CELL_SIZE); // 세로선
            }

            // 외곽선 그리기
            g.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2)); // 두꺼운 선
            int boardSizeWithPadding = BOARD_SIZE * CELL_SIZE; // 여백 포함 크기
            g2d.drawRect(PADDING, PADDING, boardSizeWithPadding - CELL_SIZE, boardSizeWithPadding - CELL_SIZE); // 여백을 위한 좌표 설정

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

            // 돌 그리기 및 기록 추가
            int stoneSize = (int) (CELL_SIZE * 0.8);
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    final int row = i; // final 변수 선언
                    final int col = j; // final 변수 선언

                    int x = PADDING + j * CELL_SIZE - stoneSize / 2;
                    int y = PADDING + i * CELL_SIZE - stoneSize / 2;
                    if (board[i][j] == 'X' || board[i][j] == 'O') {
                        // 돌 그리기
                        if (board[i][j] == 'X') {
                            g.drawImage(blackStone, x, y, stoneSize, stoneSize, this);
                        } else if (board[i][j] == 'O') {
                            g.drawImage(whiteStone, x, y, stoneSize, stoneSize, this);
                        }

                        // 기록 추가
                        boolean alreadyRecorded = gameHistory.stream()
                                .anyMatch(record -> record.row == row && record.col == col);
                        if (!alreadyRecorded) {
                            String player = board[i][j] == 'X' ? "Player 1 (X)." : "Player 2 (O).";
                            gameHistory.add(new TurnRecord(row, col, board[i][j], player));

                            // 디버깅 출력
                            System.out.println("기록 추가됨: Row = " + row + ", Col = " + col + ", Symbol = " + board[i][j] + ", Role = " + player);
                        }
                    }
                }
            }
        }


    }

    public static void main(String[] args) {
        new GomokuClient("localhost", 5000);
    }
}
