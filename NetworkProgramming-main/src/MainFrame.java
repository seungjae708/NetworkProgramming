import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Random;

public class MainFrame extends JFrame {
    private static final int BOARD_SIZE = 19; // 바둑판 크기
    private static final int CELL_SIZE = 30; // 셀 크기
    private char[][] board = new char[BOARD_SIZE][BOARD_SIZE]; // 바둑판 상태 저장
    private boolean isMyTurn = false; // 현재 플레이어의 턴 여부
    private String player; // 플레이어 이름 (Player1 또는 Player2)
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private JTextArea chatArea; // 채팅 메시지 표시
    private JTextField messageField; // 채팅 메시지 입력
    private JLabel timerLabel; // 타이머 표시
    private Timer timer; // Swing 타이머
    private int timeLeft = 20; // 20초 타이머
    private boolean undoRequested = false; // 무르기 요청 여부

    public MainFrame(String player) {
        this.player = player;

        setTitle(player + " - 오목 게임");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(null);

        // 보드 초기화
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = '.'; // 빈 칸 초기화
            }
        }

        // 서버 연결
        connectToServer();

        // 배경 이미지 설정
        JLabel background = new JLabel();
        try {
            ImageIcon bgIcon = new ImageIcon(getClass().getResource("/images/background.jpg"));
            Image scaledImage = bgIcon.getImage().getScaledInstance(1000, 700, Image.SCALE_SMOOTH);
            background.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            System.err.println("배경 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
        background.setBounds(0, 0, 1000, 700);
        add(background);

        // 오목판 패널
        JPanel gamePanel = createGamePanel();
        gamePanel.setBounds(50, 50, 570, 570);
        background.add(gamePanel);

        // 오른쪽 정보 패널
        JPanel infoPanel = createInfoPanel(player);
        infoPanel.setBounds(650, 50, 300, 200);
        background.add(infoPanel);

        // 채팅 및 무르기 패널
        JPanel chatPanel = createChatPanel();
        chatPanel.setBounds(650, 300, 300, 350); // 높이 늘려서 무르기 버튼 포함
        background.add(chatPanel);

        setVisible(true);

        // 서버로부터 보드 상태 수신
        new Thread(this::listenToServer).start();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5000); // 서버 주소 및 포트
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            String serverMessage = input.readUTF();
            isMyTurn = serverMessage.equals("Your turn."); // 서버로부터 첫 턴 여부 수신
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "서버에 연결할 수 없습니다.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private JPanel createGamePanel() {
        JPanel gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int padding = 20; // 가장자리 여백

                // 바둑판 배경 그리기
                g2.setColor(new Color(245, 222, 179)); // 바둑판 색상
                g2.fillRect(padding, padding, CELL_SIZE * (BOARD_SIZE - 1), CELL_SIZE * (BOARD_SIZE - 1));

                // 바둑판 선 그리기
                g2.setColor(Color.BLACK);
                for (int i = 0; i < BOARD_SIZE; i++) {
                    // 가로선
                    g2.drawLine(padding, padding + i * CELL_SIZE, padding + (BOARD_SIZE - 1) * CELL_SIZE, padding + i * CELL_SIZE);
                    // 세로선
                    g2.drawLine(padding + i * CELL_SIZE, padding, padding + i * CELL_SIZE, padding + (BOARD_SIZE - 1) * CELL_SIZE);
                }

                // 돌 그리기
                for (int i = 0; i < BOARD_SIZE; i++) {
                    for (int j = 0; j < BOARD_SIZE; j++) {
                        if (board[i][j] == 'X') { // Player1 돌
                            g2.setColor(Color.BLACK);
                            g2.fillOval(padding + j * CELL_SIZE - CELL_SIZE / 4, padding + i * CELL_SIZE - CELL_SIZE / 4, CELL_SIZE / 2, CELL_SIZE / 2);
                        } else if (board[i][j] == 'O') { // Player2 돌
                            g2.setColor(Color.WHITE);
                            g2.fillOval(padding + j * CELL_SIZE - CELL_SIZE / 4, padding + i * CELL_SIZE - CELL_SIZE / 4, CELL_SIZE / 2, CELL_SIZE / 2);
                        }
                    }
                }
            }
        };

        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isMyTurn) {
                    JOptionPane.showMessageDialog(null, "It's not your turn!");
                    return;
                }

                int padding = 20; // 가장자리 여백
                int x = e.getX();
                int y = e.getY();

                // 클릭한 좌표를 보드 좌표로 변환
                int col = (x - padding + CELL_SIZE / 2) / CELL_SIZE;
                int row = (y - padding + CELL_SIZE / 2) / CELL_SIZE;

                // 유효한 클릭인지 확인
                if (row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE && board[row][col] == '.') {
                    try {
                        output.writeUTF(row + "," + col); // 서버에 좌표 전송
                        isMyTurn = false; // 턴 종료
                        resetTimer(); // 타이머 초기화
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        gamePanel.setPreferredSize(new Dimension(CELL_SIZE * BOARD_SIZE, CELL_SIZE * BOARD_SIZE));
        gamePanel.setOpaque(false);
        return gamePanel;
    }

    private JPanel createInfoPanel(String player) {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(null);
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

        JLabel playerLabel = new JLabel(player, SwingConstants.LEFT);
        playerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        playerLabel.setForeground(new Color(255, 105, 180));
        playerLabel.setBounds(70, 100, 150, 50);
        infoPanel.add(playerLabel);

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
            if (!undoRequested) {
                try {
                    output.writeUTF("UNDO_REQUEST");
                    undoRequested = true;
                    JOptionPane.showMessageDialog(this, "무르기 요청을 보냈습니다.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "이미 무르기를 요청했습니다.");
            }
        });
        chatPanel.add(undoButton);

        return chatPanel;
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            try {
                output.writeUTF("CHAT:" + player + ": " + message);
                messageField.setText("");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void listenToServer() {
        try {
            while (true) {
                String message = input.readUTF();
                if (message.startsWith("CHAT:")) {
                    chatArea.append(message.substring(5) + "\n");
                } else if (message.startsWith("Current board:")) {
                    updateBoard(message);
                    repaint();
                } else if (message.equals("Your turn.")) {
                    isMyTurn = true;
                    startTimer();
                } else if (message.equals("UNDO_REQUEST_FROM_OTHER_PLAYER")) {
                    int result = JOptionPane.showConfirmDialog(
                            this,
                            "상대방이 무르기를 요청했습니다. 허락하시겠습니까?",
                            "무르기 요청",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (result == JOptionPane.YES_OPTION) {
                        output.writeUTF("UNDO_ACCEPT");
                    } else {
                        output.writeUTF("UNDO_DECLINE");
                    }
                } else if (message.equals("UNDO_ACCEPT")) {
                    JOptionPane.showMessageDialog(this, "상대방이 무르기를 허용했습니다.");
                    undoLastMove();
                    repaint();
                } else if (message.equals("UNDO_DECLINE")) {
                    JOptionPane.showMessageDialog(this, "상대방이 무르기를 거절했습니다.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void undoLastMove() {
        for (int i = BOARD_SIZE - 1; i >= 0; i--) {
            for (int j = BOARD_SIZE - 1; j >= 0; j--) {
                if (board[i][j] == (isMyTurn ? 'X' : 'O')) {
                    board[i][j] = '.';
                    return;
                }
            }
        }
    }

    private void updateBoard(String boardState) {
        String[] rows = boardState.split("\n");
        for (int i = 0; i < BOARD_SIZE; i++) {
            board[i] = rows[i + 1].toCharArray();
        }
    }

    private void startTimer() {
        resetTimer();
        timer = new Timer(1000, e -> {
            timeLeft--;
            timerLabel.setText(String.valueOf(timeLeft));
            if (timeLeft == 0) {
                makeRandomMove();
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
        Random random = new Random();
        int row, col;
        do {
            row = random.nextInt(BOARD_SIZE);
            col = random.nextInt(BOARD_SIZE);
        } while (board[row][col] != '.');

        try {
            output.writeUTF(row + "," + col);
            isMyTurn = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        resetTimer();
    }

    public static void main(String[] args) {
    	SwingUtilities.invokeLater(() -> {
            new MainFrame("Player1"); // 첫 번째 플레이어
            new MainFrame("Player2"); // 두 번째 플레이어
        });
    }
}
