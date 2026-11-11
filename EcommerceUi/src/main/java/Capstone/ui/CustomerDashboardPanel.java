package Capstone.ui;

import Capstone.app.Session;
import Capstone.model.Address;
import Capstone.model.Customer;
import Capstone.model.Notification;
import Capstone.model.Order;
import Capstone.net.Api;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CustomerDashboardPanel extends JPanel {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm");

    private final JLabel ordersStat = Theme.label("-", Font.BOLD, 26f, Theme.PRIMARY_DARK);
    private final JLabel spentStat = Theme.label("-", Font.BOLD, 26f, Theme.PRIMARY_DARK);
    private final JLabel unreadStat = Theme.label("-", Font.BOLD, 26f, Theme.PRIMARY_DARK);

    private final DefaultTableModel addressModel =
            Theme.readOnlyModel("Street", "City", "Province", "Postal code", "Country");
    private final DefaultTableModel notificationModel =
            Theme.readOnlyModel("When", "Message", "Status");
    private final JLabel problem = Theme.label(" ", Font.PLAIN, 13f, Theme.DANGER);

    public CustomerDashboardPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 24, 14, 24));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(Theme.title("My Account"), BorderLayout.NORTH);
        top.add(createProfileRow(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel tables = new JPanel(new GridLayout(1, 2, 16, 0));
        tables.setOpaque(false);
        tables.add(tableCard("Saved addresses", addressModel));
        tables.add(tableCard("Notifications", notificationModel));
        add(tables, BorderLayout.CENTER);
        add(problem, BorderLayout.SOUTH);

        load();
    }

    private JPanel createProfileRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);

        Customer customer = Session.customer();

        Theme.RoundedPanel profile = new Theme.RoundedPanel(16);
        profile.setLayout(new BorderLayout(14, 0));
        String initial = customer == null || customer.getName() == null || customer.getName().isBlank()
                ? "?" : customer.getName().substring(0, 1).toUpperCase();
        JLabel avatar = new JLabel(initial, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setForeground(Color.WHITE);
        avatar.setFont(Theme.font(Font.BOLD, 24f));
        avatar.setPreferredSize(new Dimension(56, 56));
        profile.add(avatar, BorderLayout.WEST);

        JPanel details = new JPanel(new GridLayout(0, 1));
        details.setOpaque(false);
        details.add(Theme.heading(customer == null ? "" : customer.getName()));
        details.add(Theme.muted(customer == null ? "" : customer.getEmail()));
        details.add(Theme.muted(customer == null ? "" : customer.getPhone()));
        profile.add(details, BorderLayout.CENTER);
        row.add(profile);

        row.add(statCard("Orders placed", ordersStat));
        row.add(statCard("Total spent", spentStat));
        row.add(statCard("Unread notifications", unreadStat));
        return row;
    }

    private JPanel statCard(String caption, JLabel value) {
        Theme.RoundedPanel card = new Theme.RoundedPanel(16);
        card.setLayout(new GridLayout(0, 1, 0, 2));
        card.add(Theme.muted(caption));
        card.add(value);
        return card;
    }

    private JPanel tableCard(String heading, DefaultTableModel model) {
        Theme.RoundedPanel card = new Theme.RoundedPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.add(Theme.heading(heading), BorderLayout.NORTH);
        JTable table = Theme.table(model);
        if (model.getColumnCount() == 3) {
            table.getColumnModel().getColumn(0).setPreferredWidth(130);
            table.getColumnModel().getColumn(1).setPreferredWidth(330);
            table.getColumnModel().getColumn(2).setPreferredWidth(70);
        }
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        return card;
    }


    private void load() {
        String id = Session.customerId();

        Api.async(() -> Api.get("/orders/customer/" + id, new TypeReference<List<Order>>() {}),
                orders -> {
                    List<Order> list = orders == null ? new ArrayList<>() : orders;
                    double spent = 0;
                    int placed = 0;
                    for (Order order : list) {
                        boolean cancelled = order.getShipment() != null
                                && "CANCELLED".equalsIgnoreCase(order.getShipment().getStatus());
                        if (!cancelled) {
                            spent += order.getTotalAmount();
                            placed++;
                        }
                    }
                    ordersStat.setText(String.valueOf(placed));
                    spentStat.setText(Theme.money(spent));
                }, this::fail);

        Api.async(() -> Api.get("/addresses/customer/" + id, new TypeReference<List<Address>>() {}),
                addresses -> {
                    addressModel.setRowCount(0);
                    if (addresses != null) {
                        for (Address a : addresses) {
                            addressModel.addRow(new Object[]{a.getStreetAddress(), a.getCity(),
                                    a.getState(), a.getPostalCode(), a.getCountry()});
                        }
                    }
                }, this::fail);

        Api.async(() -> Api.get("/notifications/customer/" + id, new TypeReference<List<Notification>>() {}),
                notes -> {
                    notificationModel.setRowCount(0);
                    int unread = 0;
                    if (notes != null) {
                        List<Notification> sorted = new ArrayList<>(notes);
                        sorted.sort(Comparator.comparing(Notification::getNotificationDate,
                                Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                        for (Notification n : sorted) {
                            if ("UNREAD".equalsIgnoreCase(n.getStatus())) {
                                unread++;
                            }
                            notificationModel.addRow(new Object[]{
                                    n.getNotificationDate() == null ? "" : n.getNotificationDate().format(STAMP),
                                    n.getMessage(), n.getStatus()});
                        }
                    }
                    unreadStat.setText(String.valueOf(unread));
                }, this::fail);
    }

    private void fail(Throwable error) {
        problem.setText("Some account details could not be loaded: " + Theme.friendly(error));
    }
}
