import javax.swing.*;
import java.awt.*;

public class BottomPanel extends JPanel {
    public BottomPanel() {
        setBackground(Color.MAGENTA); // 배경색
        JLabel label = new JLabel("Game Status", SwingConstants.CENTER); // 중앙 정렬된 라벨
        add(label); // 라벨 추가
    }
}
