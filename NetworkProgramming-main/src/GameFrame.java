import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class GameFrame extends JFrame {
    private Font customFont;

    public GameFrame() {
        setTitle("콩알콩알 오목 게임");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(null);

        // 배경 이미지 추가
        JLabel background = new JLabel();
        try {
            ImageIcon bgIcon = new ImageIcon(getClass().getResource("/images/background.jpg"));
            Image scaledImage = bgIcon.getImage().getScaledInstance(800, 600, Image.SCALE_SMOOTH);
            background.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            System.err.println("배경 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
        background.setBounds(0, 0, 800, 600);
        add(background);

        // 폰트 로드
        customFont = loadCustomFont("/fonts/CookieRun Regular.otf", 16f);

        // 제목 레이블
        JLabel titleLabel = new JLabel("콩알콩알 오목 게임", SwingConstants.CENTER);
        titleLabel.setBounds(200, 100, 400, 50);
        if (customFont != null) {
            titleLabel.setFont(customFont.deriveFont(40f));
        } else {
            titleLabel.setFont(new Font("Serif", Font.BOLD, 28));
        }
        background.add(titleLabel); // 배경 위에 추가

        // Start Game 버튼
        JButton startGameButton = createStyledButton("Start Game");
        startGameButton.setBounds(300, 250, 200, 40);
        background.add(startGameButton); // 배경 위에 추가

        // How to Play 버튼
        JButton howToPlayButton = createStyledButton("How to Play");
        howToPlayButton.setBounds(300, 325, 200, 40);
        background.add(howToPlayButton); // 배경 위에 추가

        // Exit 버튼
        JButton exitButton = createStyledButton("Exit");
        exitButton.setBounds(300, 400, 200, 40);
        background.add(exitButton); // 배경 위에 추가

        // 왼쪽 GIF 추가
        JLabel leftGif = new JLabel(new ImageIcon(getClass().getResource("/images/bunny.gif")));
        leftGif.setBounds(50, 30, 200, 200); // GIF의 위치와 크기 설정
        background.add(leftGif);

        // 오른쪽 GIF 추가
        JLabel rightGif = new JLabel(new ImageIcon(getClass().getResource("/images/bear.gif")));
        rightGif.setBounds(550, 30, 200, 200); // GIF의 위치와 크기 설정
        background.add(rightGif);

        // 이벤트 핸들러
        startGameButton.addActionListener(e -> {
            // 현재 시작 화면 숨기기
            setVisible(false);

            // 두 개의 MainFrame 창 띄우기
            SwingUtilities.invokeLater(() -> {
                new MainFrame("Player 1").setVisible(true);
                new MainFrame("Player 2").setVisible(true);
            });
        });

        howToPlayButton.addActionListener(e -> {
            setVisible(false);
            new HowToPlayFrame(this);
        });

        exitButton.addActionListener(e -> System.exit(0));

        setVisible(true);
        setResizable(false);
    }

    private JButton createStyledButton(String text) {
        RoundedButton button = new RoundedButton(text, 20); // 반지름 20으로 둥글게
        button.setFont(customFont != null ? customFont.deriveFont(16f) : new Font("Serif", Font.BOLD, 16));
        button.setBackground(new Color(255, 228, 181)); // 버튼 배경색
        button.setForeground(new Color(90, 60, 30)); // 버튼 텍스트 색상
        return button;
    }

    private Font loadCustomFont(String path, float size) {
        try {
            InputStream fontStream = getClass().getResourceAsStream(path);
            if (fontStream == null) {
                System.err.println("폰트 파일을 찾을 수 없습니다: " + path);
                return null;
            }
            return Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    class RoundedButton extends JButton {
        private final int radius;

        public RoundedButton(String text, int radius) {
            super(text);
            this.radius = radius;
            setFocusPainted(false); // 클릭 시 테두리 없애기
            setContentAreaFilled(false); // 배경 비활성화
            setOpaque(false); // 기본 배경 제거
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 배경 색상 설정
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

            // 텍스트 그리기
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::new);
    }
}
