package Capstone.ui;

import Capstone.model.Customer;
import Capstone.net.Api;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StaffPanel extends JPanel {

    private final List<Customer> staff = new ArrayList<>();
    private final DefaultTableModel model = Theme.readOnlyModel("Name", "Email", "Phone");
    private final JTable table = Theme.table(model);

    private final JTextField nameField = Theme.field(18);
    private final JTextField emailField = Theme.field(18);
    private final JTextField phoneField = Theme.field(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    private final JLabel message = Theme.muted(" ");
    private final JButton createButton = Theme.primary("Create staff account");
    private final JButton removeButton = Theme.ghost("Remove selected staff");

    public StaffPanel() {
        setLayout(new BorderLayout(18, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 24, 18, 24));

        passwordField.setFont(Theme.font(Font.PLAIN, 14f));
        passwordField.setMargin(new Insets(6, 8, 6, 8));

        add(createTablePanel(), BorderLayout.CENTER);
        add(createFormPanel(), BorderLayout.EAST);

        table.getSelectionModel().addListSelectionListener(e ->
                removeButton.setEnabled(table.getSelectedRow() >= 0));
        createButton.addActionListener(e -> create());
        removeButton.addActionListener(e -> remove());

        removeButton.setEnabled(false);
        load();
    }



    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(Theme.title("Staff accounts"), BorderLayout.NORTH);
        panel.add(Theme.scroll(table), BorderLayout.CENTER);
        panel.add(Theme.muted("Staff can add and edit products and stock. Only the owner can manage staff."),
                BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(18, 18, 18, 18)));
        panel.setPreferredSize(new Dimension(340, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 0, 3, 0);

        int row = 0;
        gbc.gridy = row++;
        panel.add(Theme.heading("New staff member"), gbc);

        row = addField(panel, gbc, row, "Full name", nameField);
        row = addField(panel, gbc, row, "Email address", emailField);
        row = addField(panel, gbc, row, "Phone number", phoneField);
        row = addField(panel, gbc, row, "Password (8+ characters, a letter and a number)", passwordField);

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));
        buttons.setOpaque(false);
        buttons.add(createButton);
        buttons.add(removeButton);

        gbc.gridy = row++;
        gbc.insets = new Insets(14, 0, 3, 0);
        panel.add(buttons, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(8, 0, 3, 0);
        panel.add(message, gbc);

        gbc.gridy = row;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);
        return panel;
    }

    private int addField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy = row;
        gbc.insets = new Insets(8, 0, 0, 0);
        panel.add(Theme.label(label, Font.PLAIN, 13f, Theme.MUTED), gbc);
        gbc.gridy = row + 1;
        gbc.insets = new Insets(2, 0, 3, 0);
        panel.add(field, gbc);
        return row + 2;
    }


    private void load() {
        showMessage("Loading staff...", false);
        Api.async(() -> Api.get("/staff", new TypeReference<List<Customer>>() {}), result -> {
            staff.clear();
            if (result != null) {
                staff.addAll(result);
            }
            model.setRowCount(0);
            for (Customer member : staff) {
                model.addRow(new Object[]{member.getName(), member.getEmail(), member.getPhone()});
            }
            showMessage(staff.isEmpty() ? "No staff accounts yet." : " ", false);
        }, error -> showError(error));
    }

    private void create() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in the name, email, phone number and password.", true);
            return;
        }

        setBusy(true);
        showMessage("Creating the account...", false);

        Api.async(() -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", name);
            body.put("email", email);
            body.put("phone", phone);
            body.put("password", password);
            Api.post("/staff", body);
            return Boolean.TRUE;
        }, done -> {
            setBusy(false);
            nameField.setText("");
            emailField.setText("");
            phoneField.setText("");
            passwordField.setText("");
            showMessage("Staff account created.", false);
            load();
        }, error -> {
            setBusy(false);
            showError(error);
        });
    }


    private void remove() {
        int index = table.getSelectedRow();
        if (index < 0 || index >= staff.size()) {
            return;
        }
        Customer member = staff.get(index);

        int answer = JOptionPane.showConfirmDialog(this,
                member.getName() + " will lose staff access and become a normal customer.\n"
                        + "Their account and orders are kept.",
                "Remove staff access", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        setBusy(true);
        showMessage("Removing staff access...", false);

        Api.async(() -> {
            Api.delete("/staff/" + member.getCustomerId());
            return Boolean.TRUE;
        }, done -> {
            setBusy(false);
            showMessage("Staff access removed.", false);
            load();
        }, error -> {
            setBusy(false);
            showError(error);
        });
    }


    private void setBusy(boolean busy) {
        createButton.setEnabled(!busy);
        removeButton.setEnabled(!busy && table.getSelectedRow() >= 0);
    }

    private void showMessage(String text, boolean isError) {
        message.setForeground(isError ? Theme.DANGER : Theme.MUTED);
        message.setText("<html>" + text + "</html>");
    }

    private void showError(Throwable error) {
        showMessage(Theme.friendly(error), true);
    }
}