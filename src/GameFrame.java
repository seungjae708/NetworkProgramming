import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    public GameFrame() {
        setTitle("Gomoku Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Gomoku Game", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 10));

        JButton startGameButton = new JButton("Start Game");
        JButton exitButton = new JButton("Exit");

        buttonPanel.add(startGameButton);
        buttonPanel.add(exitButton);

        add(buttonPanel, BorderLayout.CENTER);

        // 이벤트 핸들러
        startGameButton.addActionListener(e -> {
//            dispose(); // 메인 화면 닫기
            new GomokuClient("localhost", 5000); // 클라이언트 실행
        });


        exitButton.addActionListener(e -> System.exit(0));

        setVisible(true);
        setResizable(false);
    }

}
