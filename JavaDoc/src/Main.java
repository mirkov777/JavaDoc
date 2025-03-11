import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class Main {
    private static final String DB_URL = "jdbc:sqlserver://localhost;databaseName=JavaDoc;user=JavaDoc;password=JavaDoc123;trustServerCertificate=true;";
    private static Connection conn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            connectDb();
            createUI();
        });
    }

    private static void connectDb() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            conn = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to database successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC driver not found.");
            e.printStackTrace();
            System.exit(1);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database connection failed!", "err", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void createUI() {
        JFrame mainFrame = new JFrame("JavaDoc - Medical Appointments");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 400);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(3, 1));

        JButton btnClients = new JButton("Clients");
        JButton btnDoctors = new JButton("Doctors");
        JButton btnAppointments = new JButton("Appointments");

        btnClients.addActionListener(e -> new ClientsFrame(conn));
        btnDoctors.addActionListener(e -> new DoctorsFrame(conn));
        btnAppointments.addActionListener(e -> new AppointmentsFrame(conn));

        mainPanel.add(btnClients);
        mainPanel.add(btnDoctors);
        mainPanel.add(btnAppointments);

        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);
    }
}