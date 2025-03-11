import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AppointmentsFrame {
    private Connection conn;
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> cmbDoctors;
    private JComboBox<String> cmbClients;
    private JTextField dateInput;

    public AppointmentsFrame(Connection conn) {
        this.conn = conn;
        frame = new JFrame("Appointments Management");
        frame.setSize(700, 450);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        model = new DefaultTableModel(new String[]{"Doctor", "Client", "Date", "Reason", "Status"}, 0);
        table = new JTable(model);
        loadAppointments();

        JPanel panel = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Add");
        JButton btnDelete = new JButton("Delete");
        JButton btnSearch = new JButton("Search");
        JButton btnReset = new JButton("Reset");

        panel.add(btnAdd);
        panel.add(btnDelete);
        panel.add(btnSearch);
        panel.add(btnReset);

        btnAdd.addActionListener(e -> openAddAppointmentDialog());
        btnDelete.addActionListener(e -> deleteAppointment());
        btnSearch.addActionListener(e -> searchAppointments());
        btnReset.addActionListener(e -> loadAppointments());

        JPanel searchPanel = new JPanel(new FlowLayout());

        searchPanel.add(new JLabel("Doctor:"));
        cmbDoctors = new JComboBox<>();
        loadDoctors(cmbDoctors);
        searchPanel.add(cmbDoctors);

        searchPanel.add(new JLabel("Client:"));
        cmbClients = new JComboBox<>();
        loadClients(cmbClients);
        searchPanel.add(cmbClients);

        searchPanel.add(new JLabel("Date:"));
        dateInput = new JTextField(5);
        searchPanel.add(dateInput);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.add(searchPanel, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void loadAppointments() {
        model.setRowCount(0);
        try (Statement statement = conn.createStatement();
             ResultSet res = statement.executeQuery("SELECT d.first_name + ' ' + d.last_name AS doctor, c.first_name + ' ' + c.last_name AS client, " +
                                                        "a.date, a.reason, a.status FROM Appointments a JOIN Doctors d " +
                                                        "ON a.doctor_id = d.doctor_id JOIN Clients c ON a.client_id = c.client_id"))
        {
            while (res.next()) {
                model.addRow(new Object[] {
                        res.getString("doctor"),
                        res.getString("client"),
                        res.getTimestamp("date"),
                        res.getString("reason"),
                        res.getString("status")
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to load appointments.", e);
        }
    }

    private void openAddAppointmentDialog() {
        JTextField txtDate = new JTextField(10);
        JTextField txtReason = new JTextField(20);
        JComboBox<String> cmbDoctor = new JComboBox<>();
        JComboBox<String> cmbClient = new JComboBox<>();

        loadDoctors(cmbDoctor);
        loadClients(cmbClient);

        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(new JLabel("Doctor:"));
        panel.add(cmbDoctor);
        panel.add(new JLabel("Client:"));
        panel.add(cmbClient);
        panel.add(new JLabel("Date:"));
        panel.add(txtDate);
        panel.add(new JLabel("Reason:"));
        panel.add(txtReason);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add Appointment", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            addAppointment((String) cmbDoctor.getSelectedItem(), (String) cmbClient.getSelectedItem(), txtDate.getText().trim(), txtReason.getText().trim());
        }
    }

    private void addAppointment(String doctor, String client, String date, String reason) {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO Appointments (doctor_id, client_id, date, reason, status) VALUES ((SELECT doctor_id FROM Doctors WHERE first_name + ' ' + last_name = ?), (SELECT client_id FROM Clients WHERE first_name + ' ' + last_name = ?), ?, ?, 'scheduled')")) {
            stmt.setString(1, doctor);
            stmt.setString(2, client);
            stmt.setString(3, date);
            stmt.setString(4, reason);
            stmt.executeUpdate();
            loadAppointments();
        } catch (SQLException e) {
            showErrorDialog("Failed to add appointment.", e);
        }
    }

    private void deleteAppointment() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(frame, "Select an appointment to delete.");
            return;
        }

        String doctor = (String) model.getValueAt(row, 0);
        String client = (String) model.getValueAt(row, 1);
        String date = model.getValueAt(row, 2).toString();

        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM Appointments WHERE doctor_id = (SELECT doctor_id FROM Doctors WHERE first_name + ' ' + last_name = ?) AND client_id = (SELECT client_id FROM Clients WHERE first_name + ' ' + last_name = ?) AND date = ?")) {
            stmt.setString(1, doctor);
            stmt.setString(2, client);
            stmt.setString(3, date);
            stmt.executeUpdate();
            loadAppointments();
        } catch (SQLException e) {
            showErrorDialog("Failed to delete appointment.", e);
        }
    }

    private void searchAppointments() {
        String doctor = (String) cmbDoctors.getSelectedItem();
        String client = (String) cmbClients.getSelectedItem();
        String date = dateInput.getText().trim();

        boolean hasDocFilter = doctor != null && !doctor.equals("Any");
        boolean hasClientFilter = client != null && !client.equals("Any");
        boolean hasDateFilter = !date.isEmpty();

        if (!hasDocFilter && !hasClientFilter && !hasDateFilter) {
            JOptionPane.showMessageDialog(frame, "Enter at least one search criteria!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String queryStr = "SELECT d.first_name + ' ' + d.last_name AS doctor, " +
                          "c.first_name + ' ' + c.last_name AS client, " +
                          "a.date, a.reason, a.status FROM Appointments a " +
                          "JOIN Doctors d ON a.doctor_id = d.doctor_id " +
                          "JOIN Clients c ON a.client_id = c.client_id WHERE 1=1 ";

        if (hasDocFilter) queryStr += "AND (d.first_name + ' ' + d.last_name) LIKE ? ";
        if (hasClientFilter) queryStr += "AND (c.first_name + ' ' + c.last_name) LIKE ? ";
        if (hasDateFilter) queryStr += "AND CAST(a.date AS DATE) = ? ";
        queryStr += "ORDER BY a.date DESC";

        try (PreparedStatement statement = conn.prepareStatement(queryStr)) {
            int index = 1;
            if (hasDocFilter) statement.setString(index++, "%" + doctor + "%");
            if (hasClientFilter) statement.setString(index++, "%" + client + "%");
            if (hasDateFilter) statement.setString(index, date);

            ResultSet res = statement.executeQuery();
            model.setRowCount(0);
            while (res.next()) {
                model.addRow(new Object[] {
                        res.getString("doctor"),
                        res.getString("client"),
                        res.getString("date"),
                        res.getString("reason"),
                        res.getString("status")
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to search appointments.", e);
        }
    }

    private void loadDoctors(JComboBox<String> cmbDoctor) {
        try (Statement statement = conn.createStatement();
             ResultSet rs = statement.executeQuery("SELECT first_name + ' ' + last_name AS name FROM Doctors")) {
            while (rs.next()) {
                cmbDoctor.addItem(rs.getString("name"));
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to load doctors.", e);
        }
    }

    private void loadClients(JComboBox<String> cmbClient) {
        try (Statement statement = conn.createStatement();
             ResultSet res = statement.executeQuery("SELECT first_name + ' ' + last_name AS name FROM Clients")) {
            while (res.next()) {
                cmbClient.addItem(res.getString("name"));
            }
        } catch (SQLException e) {
            showErrorDialog("Failed to load clients.", e);
        }
    }

    private void showErrorDialog(String message, Exception e) {
        JOptionPane.showMessageDialog(null, message + "\nError: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}
