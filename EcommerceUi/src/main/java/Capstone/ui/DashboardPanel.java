package Capstone.ui;

import Capstone.net.Api;
import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardPanel extends JPanel {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm");

    private final JLabel salesStat = statValue();
    private final JLabel ordersStat = statValue();
    private final JLabel averageStat = statValue();
    private final JLabel customersStat = statValue();

    private final DefaultTableModel bestModel = Theme.readOnlyModel("Product", "Units sold", "Revenue");
    private final DefaultTableModel recentModel = Theme.readOnlyModel("Customer", "Date", "Total");
    private final DefaultTableModel lowModel = Theme.readOnlyModel("Product", "In stock", "Warehouse");

    private final JLabel message = Theme.muted(" ");
    private final JButton refreshButton = Theme.ghost("Refresh");

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 24, 18, 24));

        add(createHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(createStatRow(), BorderLayout.NORTH);
        body.add(createTableRow(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        refreshButton.addActionListener(e -> load());
        load();
    }



    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.add(Theme.title("Sales Dashboard"), BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(message);
        right.add(refreshButton);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createStatRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.add(statCard("Total sales", salesStat));
        row.add(statCard("Orders", ordersStat));
        row.add(statCard("Average order", averageStat));
        row.add(statCard("Customers", customersStat));
        return row;
    }

    private JPanel createTableRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 18, 0));
        row.setOpaque(false);
        row.add(tableCard("Best sellers", bestModel, 190, 70, 90));
        row.add(tableCard("Recent orders", recentModel, 100, 130, 90));
        row.add(tableCard("Low stock (10 or fewer)", lowModel, 170, 60, 130));
        return row;
    }

    private JPanel statCard(String caption, JLabel value) {
        JPanel card = new Theme.RoundedPanel(16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Theme.muted(caption));
        card.add(Box.createVerticalStrut(4));
        card.add(value);
        return card;
    }

    private static JLabel statValue() {
        return Theme.label("-", Font.BOLD, 26f, Theme.PRIMARY_DARK);
    }

    private JPanel tableCard(String title, DefaultTableModel model, int... widths) {
        JTable table = Theme.table(model);
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setOpaque(false);
        card.add(Theme.heading(title), BorderLayout.NORTH);
        card.add(Theme.scroll(table), BorderLayout.CENTER);
        return card;
    }


    private void load() {
        refreshButton.setEnabled(false);
        showMessage("Loading...", false);

        Api.async(() -> Api.get("/dashboard", JsonNode.class), data -> {
            refreshButton.setEnabled(true);
            show(data);
            showMessage(" ", false);
        }, error -> {
            refreshButton.setEnabled(true);
            showMessage(Theme.friendly(error), true);
        });
    }

    void show(JsonNode data) {
        JsonNode summary = data.path("summary");
        salesStat.setText(Theme.money(summary.path("totalSales").asDouble()));
        ordersStat.setText(String.valueOf(summary.path("orderCount").asInt()));
        averageStat.setText(Theme.money(summary.path("averageOrderValue").asDouble()));
        customersStat.setText(String.valueOf(summary.path("customerCount").asLong()));

        bestModel.setRowCount(0);
        for (JsonNode item : data.path("bestSellers")) {
            bestModel.addRow(new Object[]{
                    item.path("productName").asText(),
                    item.path("unitsSold").asInt(),
                    Theme.money(item.path("revenue").asDouble())
            });
        }

        recentModel.setRowCount(0);
        for (JsonNode item : data.path("recentOrders")) {
            recentModel.addRow(new Object[]{
                    item.path("customerName").asText(),
                    formatDate(item.path("orderDate").asText("")),
                    Theme.money(item.path("totalAmount").asDouble())
            });
        }

        lowModel.setRowCount(0);
        for (JsonNode item : data.path("lowStock")) {
            lowModel.addRow(new Object[]{
                    item.path("productName").asText(),
                    item.path("stock").asInt(),
                    item.path("warehouse").asText()
            });
        }
    }

    private static String formatDate(String text) {
        try {
            return LocalDateTime.parse(text).format(STAMP);
        } catch (Exception e) {
            return text;
        }
    }

    private void showMessage(String text, boolean isError) {
        message.setForeground(isError ? Theme.DANGER : Theme.MUTED);
        message.setText("<html>" + text + "</html>");
    }
}