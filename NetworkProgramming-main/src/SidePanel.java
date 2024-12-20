import javax.swing.*;
import java.awt.*;

public class SidePanel extends JPanel {
    public SidePanel() {
        setLayout(new GridLayout(2, 1)); // 두 개의 행으로 나뉘는 레이아웃
        setPreferredSize(new Dimension(150, 600)); // 패널의 폭을 고정

        // 상단과 하단 패널 추가
        JPanel topPanel = new TopPanel(); // TopPanel 클래스 사용
        JPanel bottomPanel = new BottomPanel(); // BottomPanel 클래스 사용

        add(topPanel); // 상단 패널 추가
        add(bottomPanel); // 하단 패널 추가
    }
}
