import javax.swing.*;
import java.awt.*;
import java.io.InputStream;

public class HowToPlayFrame extends JFrame {
    public HowToPlayFrame(GameFrame mainFrame) {
        setTitle("How to Play");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLayout(null); // 레이아웃 매니저 제거
        setLocationRelativeTo(null); // 화면 중앙에 창 배치

        Font customFont = loadCustomFont("/fonts/CookieRun Regular.otf", 18f);

        // 배경 이미지 추가
        JLabel background = new JLabel();
        try {
            ImageIcon bgIcon = new ImageIcon(getClass().getResource("/images/background.jpg")); // 리소스에서 이미지 로드
            Image scaledImage = bgIcon.getImage().getScaledInstance(600, 400, Image.SCALE_SMOOTH); // 크기 조정
            background.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            System.err.println("배경 이미지를 로드할 수 없습니다: " + e.getMessage());
        }
        background.setBounds(0, 0, 600, 400); // 이미지 위치와 크기 설정
        add(background);

        // 설명 텍스트
        JLabel instructions = new JLabel("<html><div style='text-align: center; font-family: \"CookieRun\"; font-size: 24px;'>"
                + "How to Play</div><br><br><br>"
                + "<div style='text-align: center; font-family: \"CookieRun\"; font-size: 14px;'>"
                + "1. 오목판에 바둑알을 두어 5개의 연속된 줄을 완성하세요.<br><br>"
                + "2. 검정과 흰색이 번갈아가며 진행됩니다.<br><br>"
                + "3. 먼저 5개의 줄을 만든 플레이어가 승리합니다!"
                + "</div></html>");
        instructions.setHorizontalAlignment(SwingConstants.CENTER); // 텍스트를 가로로 중앙 정렬
        instructions.setBounds(50, 20, 500, 300); // 텍스트 위치와 크기 설정
        background.add(instructions);

        // 닫기 버튼
        JButton closeButton = createStyledButton("메인화면으로 돌아가기", customFont);
        closeButton.setBounds(200, 300, 200, 40); // 버튼 위치와 크기 설정
        closeButton.addActionListener(e -> {
            dispose(); // HowToPlayFrame 닫기
            mainFrame.setVisible(true); // GameFrame 다시 표시
        });
        background.add(closeButton);

//        // 왼쪽 GIF 추가
//        JLabel leftGif = new JLabel(new ImageIcon(getClass().getResource("/images/bunny.gif")));
//        leftGif.setBounds(50, -20, 200, 200); // GIF의 위치와 크기 설정
//        background.add(leftGif);
//
//        // 오른쪽 GIF 추가
//        JLabel rightGif = new JLabel(new ImageIcon(getClass().getResource("/images/bear.gif")));
//        rightGif.setBounds(350, -20, 200, 200); // GIF의 위치와 크기 설정
//        background.add(rightGif);

        setVisible(true);
    }

    private JButton createStyledButton(String text, Font font) {
        RoundedButton button = new RoundedButton(text, 20); // 반지름 20으로 둥글게
        button.setFont(font != null ? font.deriveFont(16f) : new Font("Serif", Font.BOLD, 16));
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
}