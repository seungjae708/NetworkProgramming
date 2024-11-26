import javax.swing.*;
import java.awt.*;

public class TopPanel extends JPanel {
    public TopPanel() {
        setBackground(Color.CYAN); // 배경색
        JLabel label = new JLabel("Player Info", SwingConstants.CENTER); // 중앙 정렬된 라벨
        add(label); // 라벨 추가
    }
}
