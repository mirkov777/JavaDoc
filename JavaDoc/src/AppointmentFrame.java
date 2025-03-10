import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

public class AppointmentFrame {
    public AppointmentFrame(Connection conn) {
        JFrame frame = new JFrame("Appointments Management");
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }
}
