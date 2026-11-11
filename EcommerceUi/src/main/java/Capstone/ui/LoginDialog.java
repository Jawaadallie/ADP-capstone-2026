package Capstone.ui;

import Capstone.app.Session;
import Capstone.model.Customer;
import Capstone.net.Api;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;


public class LoginDialog extends JDialog {

    private static final String DEMO_EMAIL = "demo@legendarysprays.co.za";

    private boolean signedIn = false;

    private final JTextField signInEmail = Theme.field(22);
    private final JPasswordField signInPassword = passwordField();
    private final JTextField newName = Theme.field(22);
    private final JTextField newEmail = Theme.field(22);
    private final JTextField newPhone = Theme.field(22);
    private final JPasswordField newPassword = passwordField();
    private final JPasswordField newConfirm = passwordField();
    private final JLabel message = Theme.label(" ", Font.PLAIN, 13f, Theme.DANGER);
    private final JButton signInButton = Theme.primary("Sign in");
    private final JButton createButton = Theme.primary("Create account");

    public LoginDialog() {
        super((Frame) null, "Legendary Sprays", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.add(createBrandPanel(), BorderLayout.WEST);
        root.add(createFormPanel(), BorderLayout.CENTER);

        setContentPane(root);
        setSize(860, 640);
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(null);
        setResizable(true);
        getRootPane().setDefaultButton(signInButton);
    }

    public boolean isSignedIn() {
        return signedIn;
    }

    private JPanel createBrandPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.DARK, 0, getHeight(), Theme.PRIMARY));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(50, 36, 36, 36));
        panel.setPreferredSize(new Dimension(340, 0));

        JLabel brand = Theme.label("Legendary", Font.BOLD, 34f, Color.WHITE);
        JLabel brand2 = Theme.label("Sprays", Font.BOLD, 34f, new Color(0xFFC27A));
        JLabel tagline = Theme.label(
                "<html>Cleaning sprays for the home, the kitchen and the car.</html>",
                Font.PLAIN, 15f, new Color(0xD3E7EA));

        panel.add(brand);
        panel.add(brand2);
        panel.add(Box.createVerticalStrut(18));
        panel.add(tagline);
        panel.add(Box.createVerticalGlue());
        panel.add(Theme.label("Browse. Order. Track your delivery.", Font.BOLD, 13f, Color.WHITE));
        panel.add(Box.createVerticalStrut(4));
        panel.add(Theme.label("Demo store - no real payments are taken.", Font.PLAIN, 12f, new Color(0xB5D3D8)));
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(new EmptyBorder(30, 36, 24, 36));

        JPanel header = new JPanel(new GridLayout(0, 1, 0, 4));
        header.setOpaque(false);
        header.add(Theme.title("Welcome"));
        header.add(Theme.muted("Sign in to start shopping."));
        panel.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sign in", scrollable(createSignInTab()));
        tabs.addTab("Create account", scrollable(createRegisterTab()));
        panel.add(tabs, BorderLayout.CENTER);

        panel.add(message, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSignInTab() {
        JPanel panel = new Theme.ViewportPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(18, 4, 4, 4));
        GridBagConstraints gbc = baseConstraints();

        signInEmail.setText(DEMO_EMAIL);

        addRow(panel, gbc, 0, "Email address", signInEmail);
        addRow(panel, gbc, 1, "Password", signInPassword);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;

        gbc.gridy = 3;
        gbc.insets = new Insets(18, 4, 4, 4);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(signInButton, gbc);

        gbc.gridy = 4;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);

        signInButton.addActionListener(e -> signIn());
        return panel;
    }

    private JPanel createRegisterTab() {
        JPanel panel = new Theme.ViewportPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(18, 4, 4, 4));
        GridBagConstraints gbc = baseConstraints();

        addRow(panel, gbc, 0, "Full name", newName);
        addRow(panel, gbc, 1, "Email address", newEmail);
        addRow(panel, gbc, 2, "Phone number", newPhone);
        addRow(panel, gbc, 3, "Password", newPassword);
        addRow(panel, gbc, 4, "Confirm password", newConfirm);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(Theme.muted("At least 8 characters, with a letter and a number."), gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(18, 4, 4, 4);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(createButton, gbc);

        gbc.gridy = 7;
        gbc.weighty = 1;
        panel.add(Box.createVerticalGlue(), gbc);

        createButton.addActionListener(e -> register());
        return panel;
    }

    private static JScrollPane scrollable(JPanel panel) {
        JScrollPane scroll = new JScrollPane(panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(panel.getBackground());
        return scroll;
    }

    private static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField(22);
        field.setFont(Theme.font(Font.PLAIN, 14f));
        field.setMargin(new Insets(6, 8, 6, 8));
        return field;
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        return gbc;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        field.setMinimumSize(new Dimension(60, field.getPreferredSize().height));
        gbc.gridwidth = 1;
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(Theme.label(label, Font.PLAIN, 13f, Theme.MUTED), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void signIn() {
        String email = signInEmail.getText().trim();
        String password = new String(signInPassword.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email address and password.");
            return;
        }

        setBusy(true, "Signing in...");

        // The backend checks the email + password; /auth/me answers with the customer if they are right.
        Api.setCredentials(email, password);
        Api.async(
                () -> Api.get("/auth/me", Customer.class),
                customer -> {
                    if (customer == null || customer.getCustomerId() == null) {
                        Api.clearCredentials();
                        setBusy(false, null);
                        showError("Could not sign in. Please try again.");
                        return;
                    }
                    signInPassword.setText("");
                    finish(customer);
                },
                error -> {
                    Api.clearCredentials();
                    setBusy(false, null);
                    if (error instanceof Api.ApiException api && api.getStatus() == 401) {
                        showError("Incorrect email or password.");
                    } else {
                        showError(Theme.friendly(error));
                    }
                });
    }

    private void register() {
        String name = newName.getText().trim();
        String email = newEmail.getText().trim();
        String phone = newPhone.getText().trim();
        String password = new String(newPassword.getPassword());
        String confirm = new String(newConfirm.getPassword());

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            showError("Please fill in your name, email, phone number and password.");
            return;
        }
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            showError("That email address does not look right.");
            return;
        }
        if (password.length() < 8) {
            showError("Your password must be at least 8 characters.");
            return;
        }
        if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
            showError("Your password must contain at least one letter and one number.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("The two passwords do not match.");
            return;
        }

        setBusy(true, "Creating your account...");

        // Registration is open to everyone, so make sure no old login is attached to the request.
        Api.clearCredentials();
        Api.async(
                () -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("name", name);
                    body.put("email", email);
                    body.put("phone", phone);
                    body.put("password", password);
                    return Api.post("/auth/register", body, Customer.class);
                },
                customer -> {
                    if (customer == null || customer.getCustomerId() == null) {
                        setBusy(false, null);
                        showError("The account could not be created.");
                        return;
                    }
                    Api.setCredentials(email, password);
                    newPassword.setText("");
                    newConfirm.setText("");
                    finish(customer);
                },
                error -> {
                    setBusy(false, null);
                    showError(Theme.friendly(error));
                });
    }

    private void finish(Customer customer) {
        Session.signIn(customer);
        signedIn = true;
        dispose();
    }

    private void setBusy(boolean busy, String text) {
        signInButton.setEnabled(!busy);
        createButton.setEnabled(!busy);
        message.setForeground(Theme.MUTED);
        message.setText(text == null ? " " : text);
    }

    private void showError(String text) {
        message.setForeground(Theme.DANGER);
        message.setText("<html>" + text + "</html>");
    }
}
