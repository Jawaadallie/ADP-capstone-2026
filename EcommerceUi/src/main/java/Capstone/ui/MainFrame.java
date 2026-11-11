package Capstone.ui;

import Capstone.Main;
import Capstone.app.Cart;
import Capstone.app.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {

    private static final int TAB_SHOP = 0;
    private static final int TAB_ORDERS = 1;
    private static final int TAB_REVIEWS = 2;
    private static final int TAB_ACCOUNT = 3;

    private final JTabbedPane tabs = new JTabbedPane();
    private final CatalogPanel catalog = new CatalogPanel();
    private final JButton cartButton = Theme.accent("Cart (0)");

    public MainFrame() {
        super("Legendary Sprays");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);

        tabs.setFont(Theme.font(Font.BOLD, 14f));
        tabs.addTab("Shop", catalog);
        tabs.addTab("My Orders", new OrderShipmentPanel());
        tabs.addTab("Reviews", new ReviewPanel());
        tabs.addTab("My Account", new CustomerDashboardPanel());
        if (Session.isStaff()) {
            tabs.addTab("Manage Products", new AdminProductsPanel());
        }
        if (Session.isOwner()) {
            tabs.addTab("Staff", new StaffPanel());
            tabs.addTab("Dashboard", new DashboardPanel());
        }

        // Refresh a tab each time it is opened so it always shows current data
        tabs.addChangeListener(e -> {
            switch (tabs.getSelectedIndex()) {
                case TAB_SHOP -> catalog.load();
                case TAB_ORDERS -> tabs.setComponentAt(TAB_ORDERS, new OrderShipmentPanel());
                case TAB_REVIEWS -> tabs.setComponentAt(TAB_REVIEWS, new ReviewPanel());
                case TAB_ACCOUNT -> tabs.setComponentAt(TAB_ACCOUNT, new CustomerDashboardPanel());
                default -> { }
            }
        });

        add(tabs, BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        Cart.get().addListener(this::updateCartButton);
        updateCartButton();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.DARK);
        header.setBorder(new EmptyBorder(12, 24, 12, 24));

        JPanel brand = new JPanel(new GridLayout(0, 1));
        brand.setOpaque(false);
        brand.add(Theme.label("Legendary Sprays", Font.BOLD, 24f, Color.WHITE));
        brand.add(Theme.label("Cleaning sprays for home, kitchen and car",
                Font.PLAIN, 12f, new Color(0xA9C7CE)));
        header.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        right.setOpaque(false);

        String name = Session.customer() == null ? "" : Session.customer().getName();
        right.add(Theme.label("Hi, " + name, Font.PLAIN, 14f, Color.WHITE));
        if (Session.isStaff()) {
            right.add(Theme.label(Session.role(), Font.BOLD, 12f, new Color(0xFFC27A)));
        }

        JButton signOut = Theme.onDark("Sign out");
        signOut.addActionListener(e -> signOut());
        right.add(signOut);

        cartButton.addActionListener(e -> openCart());
        right.add(cartButton);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel placeholder(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Theme.BG);
        panel.add(Theme.title(text));
        return panel;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(Theme.BG);
        footer.add(Theme.muted("Legendary Sprays demo store - no real payments are processed."));
        return footer;
    }

    private void updateCartButton() {
        int count = Cart.get().itemCount();
        cartButton.setText("Cart (" + count + ")");
        cartButton.repaint();
    }

    private void openCart() {
        CartDialog dialog = new CartDialog(this);
        dialog.setVisible(true);

        if (dialog.isCheckoutRequested()) {
            CheckoutDialog checkout = new CheckoutDialog(this);
            checkout.setVisible(true);

            if (checkout.isOrderPlaced()) {
                catalog.load();
                if (tabs.getSelectedIndex() == TAB_ORDERS) {
                    tabs.setComponentAt(TAB_ORDERS, new OrderShipmentPanel());
                } else {
                    tabs.setSelectedIndex(TAB_ORDERS);
                }
            }
        }
    }

    private void signOut() {
        Session.signOut();
        dispose();
        SwingUtilities.invokeLater(Main::start);
    }
}
