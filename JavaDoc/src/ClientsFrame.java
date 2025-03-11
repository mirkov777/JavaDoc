import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ClientsFrame {
    private Connection conn;
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JTextField regDateInput;
    private JComboBox<String> cmbSpecialization;

    private void showErrorDialog(String message, Exception e) {
        JOptionPane.showMessageDialog(null, message + "\nError: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }

    public ClientsFrame(Connection conn) {
        this.conn = conn;
        frame = new JFrame("Clients Management");
        frame.setSize(700, 450);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        model = new DefaultTableModel(new String[]{"First Name", "Last Name", "Email", "Phone", "Age", "Registration Date"}, 0);
        table = new JTable(model);
        loadClients();

        // Control Panel
        JPanel panel = new JPanel(new FlowLayout());

        JButton btnAdd = new JButton("Add");
        JButton btnDelete = new JButton("Delete");
        JButton btnSearch = new JButton("Search");
        JButton btnReset = new JButton("Reset");

        panel.add(btnAdd);
        panel.add(btnDelete);
        panel.add(btnSearch);
        panel.add(btnReset);

        btnAdd.addActionListener(e -> addClient());
        btnDelete.addActionListener(e -> deleteClient());
        btnSearch.addActionListener(e -> searchClients());
        btnReset.addActionListener(e -> loadClients());

        // Search Fields Panel
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("Registration Date:"));
        regDateInput = new JTextField(10);
        searchPanel.add(regDateInput);

        searchPanel.add(new JLabel("Doctor Specialization:"));
        cmbSpecialization = new JComboBox<>();
        loadSpecializations();  // Fill specialization dropdown
        searchPanel.add(cmbSpecialization);

        // Layout
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(searchPanel, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void loadClients() {
        model.setRowCount(0);
        try (Statement statement = conn.createStatement();
             ResultSet res = statement.executeQuery("SELECT first_name, last_name, email, phone, age, registration_date FROM Clients")) {
            while (res.next()) {
                model.addRow(new Object[]{
                        res.getString("first_name"),
                        res.getString("last_name"),
                        res.getString("email"),
                        res.getString("phone"),
                        res.getInt("age"),
                        res.getTimestamp("registration_date")
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to load clients.", e);
        }
    }

    private void loadSpecializations() {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM Specializations")) {
            cmbSpecialization.addItem("Any");
            while (rs.next()) {
                cmbSpecialization.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to load specializations.", e);
        }
    }

    private void addClient() {
        String firstName = JOptionPane.showInputDialog("Enter first name:");
        String lastName = JOptionPane.showInputDialog("Enter last name:");
        String email = JOptionPane.showInputDialog("Enter email:");
        String phone = JOptionPane.showInputDialog("Enter phone:");
        String ageStr = JOptionPane.showInputDialog("Enter age:");

        if (firstName == null || lastName == null || email == null || phone == null || ageStr == null) return;
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Invalid age entered.");
            return;
        }

        try (PreparedStatement statement = conn.prepareStatement("INSERT INTO Clients (first_name, last_name, email, phone, age) VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, firstName);
            statement.setString(2, lastName);
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
        String email = (String) model.getValueAt(row, 2);

        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Clients WHERE email = ?")) {
            stmt.setString(1, email);
            stmt.executeUpdate();
            loadClients();
        } catch (SQLException e) {
            showErrorDialog("Failed to delete client.", e);
        }
    }

    private void searchClients() {
        String regDate = regDateInput.getText().trim();
        boolean hasDateFilter = !regDate.isEmpty();

        if (!hasDateFilter) {
            JOptionPane.showMessageDialog(frame, "Please enter at least one search criteria!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String queryStr = "SELECT first_name, last_name, email, phone, age, registration_date FROM Clients WHERE 1=1 ";

        if (hasDateFilter) queryStr += "AND registration_date >= ? ";
        queryStr += "ORDER BY registration_date DESC";

        try (PreparedStatement statement = conn.prepareStatement(queryStr)) {
            int index = 1;
            if (hasDateFilter) statement.setString(index++, regDate);

            ResultSet res = statement.executeQuery();
            model.setRowCount(0);

            while (res.next()) {
                model.addRow(new Object[]{
                        res.getString("first_name"),
                        res.getString("last_name"),
                        res.getString("email"),
                        res.getString("phone"),
                        res.getInt("age"),
                        res.getTimestamp("registration_date")
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to search clients.", e);
        }
    }

}
