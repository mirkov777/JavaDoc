import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DoctorsFrame {
    private Connection conn; //database conn
    private JFrame frame; // main frame for doctors
    private JTable table; // table with data
    private DefaultTableModel model; //default col/row titles
    private JComboBox<String> cmbSpecialization; //combo box for spec criteria
    private JTextField minExperienceInput; //mix exp criteria
    private JTextField minRatingInput; //min rating criteria

    public DoctorsFrame(Connection conn) {
        this.conn = conn;
        frame = new JFrame("Doctors Management");
        frame.setSize(700, 450);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        model = new DefaultTableModel(new String[]{"First Name", "Last Name", "Specialization", "Email", "Phone", "Experience", "Rating"}, 0);
        table = new JTable(model);
        loadDoctors();

        JPanel panel = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Add");
        JButton btnDelete = new JButton("Delete");
        JButton btnSearch = new JButton("Search");
        JButton btnReset = new JButton("Reset");

        panel.add(btnAdd);
        panel.add(btnDelete);
        panel.add(btnSearch);
        panel.add(btnReset);

        btnAdd.addActionListener(e -> addDoctor());
        btnDelete.addActionListener(e -> deleteDoctor());
        btnSearch.addActionListener(e -> searchDoctors());
        btnReset.addActionListener(e -> loadDoctors());

        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("Specialization:"));
        cmbSpecialization = new JComboBox<>();
        loadSpecializations();
        searchPanel.add(cmbSpecialization);

        searchPanel.add(new JLabel("Min Exp:"));
        minExperienceInput = new JTextField(5);
        searchPanel.add(minExperienceInput);

        searchPanel.add(new JLabel("Min Rating:"));
        minRatingInput = new JTextField(5);
        searchPanel.add(minRatingInput);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(searchPanel, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void loadDoctors() {
        model.setRowCount(0);
        String queryStr = "SELECT d.first_name, d.last_name, s.name, d.email, d.phone, d.years_of_exp, d.rating " +
                          "FROM Doctors d JOIN Specializations s ON d.specialization_id = s.specialization_id";

        try (Statement stmt = conn.createStatement(); ResultSet res = stmt.executeQuery(queryStr)) {
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

    private void loadSpecializations() {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM Specializations")) {
            //default
            cmbSpecialization.addItem("Any");
            while (rs.next()) {
                cmbSpecialization.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to load specializations.", e);
        }
    }

    private void addDoctor() {
        String firstName = JOptionPane.showInputDialog("Enter first name:");
        String lastName = JOptionPane.showInputDialog("Enter last name:");
        String email = JOptionPane.showInputDialog("Enter email:");
        String phone = JOptionPane.showInputDialog("Enter phone:");
        String expStr = JOptionPane.showInputDialog("Enter years of experience:");
        String specialization = JOptionPane.showInputDialog("Enter specialization:");

        if (firstName == null || lastName == null || email == null || phone == null || expStr == null || specialization == null) return;

        try {
            int experience = Integer.parseInt(expStr);
            int spec_id = getOrCreateSpecialization(specialization);

            String insertDoctor = "INSERT INTO Doctors (first_name, last_name, specialization_id, email, phone, years_of_exp) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertDoctor)) {
                stmt.setString(1, firstName);
                stmt.setString(2, lastName);
                stmt.setInt(3, spec_id);
                stmt.setString(4, email);
                stmt.setString(5, phone);
                stmt.setInt(6, experience);
                stmt.executeUpdate();
            }

            loadDoctors();
        } catch (SQLException e) {
            showErrorDialog("Failed to add doctor.", e);
        } catch (NumberFormatException e) {
            showErrorDialog("Invalid experience value.", e);
        }
    }

    // use when creating doctors to avioid complication
    private int getOrCreateSpecialization(String specialization) throws SQLException {
        int specializationId;

        String specQuery = "SELECT specialization_id FROM Specializations WHERE name = ?";
        try (PreparedStatement statement = conn.prepareStatement(specQuery)) {
            statement.setString(1, specialization);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        String insertSpec = "INSERT INTO Specializations (name) VALUES (?)";
        try (PreparedStatement statement = conn.prepareStatement(insertSpec, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, specialization);
            statement.executeUpdate();
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        }

        throw new SQLException("Failed to create specialization.");
    }

    private void deleteDoctor() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(frame, "Select a doctor to delete.");
            return;
        }

        String email = (String) model.getValueAt(row, 3);
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM Doctors WHERE email = ?")) {
            stmt.setString(1, email);
            stmt.executeUpdate();
            loadDoctors();
        } catch (SQLException e) {
            showErrorDialog("Failed to delete doctor.", e);
        }
    }

    private void searchDoctors() {
        String specialization = (String)cmbSpecialization.getSelectedItem();
        String minExpStr = minExperienceInput.getText().trim();
        String minRatingStr = minRatingInput.getText().trim();

        boolean hasSpecFilter = specialization != null && !specialization.equals("Any");
        boolean hasExpFilter = !minExpStr.isEmpty();
        boolean hasRatingFilter = !minRatingStr.isEmpty();

        if (!hasSpecFilter && !hasExpFilter && !hasRatingFilter) {
            JOptionPane.showMessageDialog(frame, "Enter at least one search criteria!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String queryStr = "SELECT d.first_name, d.last_name, s.name, d.email, d.phone, d.years_of_exp, d.rating " +
                          "FROM Doctors d JOIN Specializations s ON d.specialization_id = s.specialization_id WHERE 1=1";

        if (hasSpecFilter) queryStr += "AND s.name LIKE ? ";
        if (hasExpFilter) queryStr += "AND d.years_of_exp >= ? ";
        if (hasRatingFilter) queryStr += "AND d.rating >= ? ";
        queryStr += "ORDER BY d.years_of_exp DESC";

        try (PreparedStatement statement = conn.prepareStatement(queryStr)) {
            int index = 1;
            if (hasSpecFilter) statement.setString(index++, "%" + specialization + "%");
            if (hasExpFilter) statement.setInt(index++, Integer.parseInt(minExpStr));
            if (hasRatingFilter) statement.setFloat(index, Float.parseFloat(minRatingStr));

            ResultSet rs = statement.executeQuery();
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("name"), rs.getString("email"),
                        rs.getString("phone"), rs.getInt("years_of_exp"),
                        rs.getFloat("rating")
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to search doctors.", e);
        }
    }

    private void showErrorDialog(String message, Exception e) {
        JOptionPane.showMessageDialog(frame, message + "\nError: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}
