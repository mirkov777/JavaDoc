import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DoctorFrame {
    private Connection conn;
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;

    public DoctorFrame(Connection conn) {
        this.conn = conn;
        frame = new JFrame("Doctors Management");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        model = new DefaultTableModel(new String[] { "First Name", "Last Name", "Specialization", "Email", "Phone", "Experience", "Rating" }, 0);
        table = new JTable(model);
        loadDoctors();

        JPanel panel = new JPanel(new FlowLayout());

        JButton btnAdd = new JButton("Add Doctor");
        JButton btnDelete = new JButton("Delete Doctor");
        JButton btnSearch = new JButton("Search Doctors");

        try {
            Image addBtnImg = ImageIO.read(getClass().getResource("assets/sun.png")).getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            btnAdd.setIcon(new ImageIcon(addBtnImg));
            btnAdd.setBorder(null);

            Image deleteBtnImg = ImageIO.read(getClass().getResource("assets/sun.png")).getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            btnAdd.setIcon(new ImageIcon(deleteBtnImg));
            btnAdd.setBorder(null);

            Image searchBtnImg = ImageIO.read(getClass().getResource("assets/sun.png")).getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            btnAdd.setIcon(new ImageIcon(searchBtnImg));
            btnAdd.setBorder(null);

            Image img = ImageIO.read(getClass().getResource("assets/sun.png")).getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            btnAdd.setIcon(new ImageIcon(img));
            btnAdd.setBorder(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        panel.add(btnAdd);
        panel.add(btnDelete);
        panel.add(btnSearch);

        btnAdd.addActionListener(e -> addDoctor());
        btnDelete.addActionListener(e -> deleteDoctor());
        btnSearch.addActionListener(e -> searchDoctors());

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void loadDoctors() {
        model.setRowCount(0);
        String queryStr = "SELECT d.first_name, d.last_name, s.name, d.email, d.phone, d.years_of_exp, d.rating " +
                          "FROM Doctors d JOIN Specializations s ON d.specialization_id = s.specialization_id";

        try (Statement statement = conn.createStatement();
             ResultSet res = statement.executeQuery(queryStr)) {

            while (res.next()) {
                model.addRow(new Object[]{
                        res.getString("first_name"),
                        res.getString("last_name"),
                        res.getString("name"),
                        res.getString("email"),
                        res.getString("phone"),
                        res.getInt("years_of_exp"),
                        res.getFloat("rating")
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to load doctors.", e);
        }
    }

    private void addDoctor() {
        String firstName = getUserInput("Enter first name:");
        if (firstName == null) return;
        String lastName = getUserInput("Enter last name:");
        if (lastName == null) return;
        String specialization = getUserInput("Enter specialization:");
        if (specialization == null) return;
        String email = getUserInput("Enter email:");
        if (email == null) return;
        String phone = getUserInput("Enter phone:");
        if (phone == null) return;
        String expString = getUserInput("Enter years of experience:");
        if (expString == null) return;

        try {
            int experience = Integer.parseInt(expString);
            int specializationId;

            //check if the spec already exists
            String specQuery = "SELECT specialization_id FROM Specializations WHERE name = ?";
            try (PreparedStatement specStmt = conn.prepareStatement(specQuery)) {
                specStmt.setString(1, specialization);
                ResultSet specRes = specStmt.executeQuery();

                if (specRes.next()) {
                    specializationId = specRes.getInt(1); // Use existing specialization_id
                } else {
                    // if not, create it
                    String insertSpec = "INSERT INTO Specializations (name) VALUES (?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSpec, Statement.RETURN_GENERATED_KEYS)) {
                        insertStmt.setString(1, specialization);
                        insertStmt.executeUpdate();

                        ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            specializationId = generatedKeys.getInt(1); // Get newly created specialization_id
                        } else {
                            showErrorDialog("Failed to create specialization.", new SQLException("No ID returned."));
                            return;
                        }
                    }
                }
            }

            String insertDoctor = "INSERT INTO Doctors (first_name, last_name, specialization_id, email, phone, years_of_exp) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = conn.prepareStatement(insertDoctor)) {
                statement.setString(1, firstName);
                statement.setString(2, lastName);
                statement.setInt(3, specializationId);
                statement.setString(4, email);
                statement.setString(5, phone);
                statement.setInt(6, experience);
                statement.executeUpdate();
            }

            loadDoctors();
        } catch (SQLException e) {
            showErrorDialog("Failed to add doctor.", e);
        } catch (NumberFormatException e) {
            showErrorDialog("Invalid input for years of experience.", e);
        }
    }

    private void deleteDoctor() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(frame, "Select a doctor to delete.");
            return;
        }

        String email = (String) model.getValueAt(row, 3);

        try (PreparedStatement statement = conn.prepareStatement("DELETE FROM Doctors WHERE email = ?")) {
            statement.setString(1, email);
            statement.executeUpdate();
            loadDoctors();
        } catch (SQLException e) {
            showErrorDialog("Failed to delete doctor.", e);
        }
    }

    private void searchDoctors() {
        String specialization = getUserInput("Enter specialization:");
        if (specialization == null) return;
        String minExperience = getUserInput("Enter minimum years of experience:");
        if (minExperience == null) return;

        model.setRowCount(0);
        String queryStr = "SELECT d.first_name, d.last_name, s.name, d.email, d.phone, d.years_of_exp, d.rating " +
                          "FROM Doctors d JOIN Specializations s ON d.specialization_id = s.specialization_id " +
                          "WHERE s.name LIKE ? AND d.years_of_exp >= ?";

        try (PreparedStatement statement = conn.prepareStatement(queryStr)) {
            statement.setString(1, "%" + specialization + "%");
            statement.setInt(2, Integer.parseInt(minExperience));
            ResultSet res = statement.executeQuery();

            while (res.next()) {
                model.addRow(new Object[] {
                        res.getString("first_name"),
                        res.getString("last_name"),
                        res.getString("name"),
                        res.getString("email"),
                        res.getString("phone"),
                        res.getInt("years_of_exp"),
                        res.getFloat("rating")
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to search doctors.", e);
        }
    }

    private void showErrorDialog(String message, Exception e) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame,
                                                               message + "\nError: " + e.getMessage(),
                                                                  "Database Error", JOptionPane.ERROR_MESSAGE));
        e.printStackTrace();
    }

    // Custom func for user input handling
    private String getUserInput(String data) {
        String input = JOptionPane.showInputDialog(frame, data);
        if (input == null || input.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Action cancelled.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        return input;
    }
}