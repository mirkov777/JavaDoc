import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ClientFrame {
    private Connection conn;
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;

    private void showErrorDialog(String message, Exception e) {
        JOptionPane.showMessageDialog(null, message + "\nError: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }

    public ClientFrame(Connection conn) {
        this.conn = conn;
        frame = new JFrame("Clients Management");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        model = new DefaultTableModel(new String[] { "First Name", "Last Name", "Email", "Phone", "Age" }, 0);
        table = new JTable(model);
        loadClients();

        JPanel panel = new JPanel(new FlowLayout());

        JButton btnAdd = new JButton("Add");
        JButton btnDelete = new JButton("Delete");
        JButton btnSearch = new JButton("Search");

        panel.add(btnAdd);
        panel.add(btnDelete);
        panel.add(btnSearch);

        btnAdd.addActionListener(e -> addClient());
        btnDelete.addActionListener(e -> deleteClient());

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void loadClients() {
        model.setRowCount(0);
        try (Statement statement = conn.createStatement();
             ResultSet res = statement.executeQuery("SELECT * FROM Clients"))
        {
            while (res.next()) {
                model.addRow(
                        new Object[] {
                                res.getInt("client_id"),
                                res.getString("first_name"),
                                res.getString("last_name"),
                                res.getString("email"),
                                res.getString("phone"),
                                res.getInt("age")
                        });
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to load clients from the database.", e);
        }
    }

    private void addClient() {
        String fist_name = JOptionPane.showInputDialog("Enter first name:");
        String last_name = JOptionPane.showInputDialog("Enter last name:");
        String email = JOptionPane.showInputDialog("Enter email:");
        String phone = JOptionPane.showInputDialog("Enter phone:");
        String age_string = JOptionPane.showInputDialog("Enter age:");
        int age = Integer.parseInt(age_string);

        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO Clients (first_name, last_name, email, phone, age) VALUES (?, ?, ?, ?, ?)");) {
            statement.setString(1, fist_name);
            statement.setString(2, last_name);
            statement.setString(3, email);
            statement.setString(4, phone);
            statement.setInt(5, age);
            statement.executeUpdate();
            loadClients();
        } catch (SQLException e) {
            showErrorDialog("Failed to add new client.", e);
        }
    }

    private void deleteClient() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(frame, "Select a client to delete.");
            return;
        }
        int clientId = (int) model.getValueAt(row, 0);

        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Clients WHERE client_id = ?")) {
            stmt.setInt(1, clientId);
            stmt.executeUpdate();
            loadClients();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}