package Capstone.ui;

import Capstone.app.Cart;
import Capstone.app.Session;
import Capstone.model.Order;
import Capstone.model.OrderItem;
import Capstone.model.Shipment;
import Capstone.net.Api;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderShipmentPanel extends JPanel {

    private static final String[] STAGES = {"PROCESSING", "SHIPPED", "DELIVERED"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final DefaultTableModel ordersModel =
            Theme.readOnlyModel("Order", "Date", "Items", "Total", "Status");
    private final JTable ordersTable = Theme.table(ordersModel);
    private final DefaultTableModel itemsModel =
            Theme.readOnlyModel("Product", "Qty", "Unit price", "Subtotal");
    private final JTable itemsTable = Theme.table(itemsModel);

    private final JLabel orderTitle = Theme.title("Select an order");
    private final JLabel orderSubtitle = Theme.muted(" ");
    private final JPanel statusSlot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    private final TrackerPanel tracker = new TrackerPanel();

    private final JLabel itemsTotal = valueLabel();
    private final JLabel vatTotal = valueLabel();
    private final JLabel deliveryTotal = valueLabel();
    private final JLabel grandTotal = valueLabel();
    private final JLabel payment = valueLabel();

    private final JLabel shipAddress = valueLabel();
    private final JLabel shipDate = valueLabel();
    private final JLabel shipEta = valueLabel();
    private final JButton advance = Theme.primary("Advance shipment (demo)");
    private final JButton cancelOrder = Theme.ghost("Cancel order");

    private final JLabel emptyState = Theme.muted(" ");

    private List<Order> orders = new ArrayList<>();
    private Order selected;

    public OrderShipmentPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 24, 14, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(0, 1));
        titles.setOpaque(false);
        titles.add(Theme.title("My Orders"));
        titles.add(emptyState);
        top.add(titles, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createListCard(), createDetailCard());
        split.setResizeWeight(0.38);
        split.setBorder(null);
        split.setOpaque(false);
        split.setDividerSize(12);
        add(split, BorderLayout.CENTER);

        ordersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
        advance.addActionListener(e -> advanceShipment());
        advance.setVisible(false);
        cancelOrder.addActionListener(e -> cancelSelected());
        cancelOrder.setVisible(false);

        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(260);
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        ordersTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        ordersTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        ordersTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        ordersTable.getColumnModel().getColumn(3).setPreferredWidth(90);

        load(null);
    }

    private static JLabel valueLabel() {
        JLabel label = Theme.label(" ", Font.PLAIN, 14f, Theme.TEXT);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }


    private JComponent createListCard() {
        Theme.RoundedPanel card = new Theme.RoundedPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.add(Theme.heading("Order history"), BorderLayout.NORTH);
        card.add(Theme.scroll(ordersTable), BorderLayout.CENTER);
        card.setMinimumSize(new Dimension(300, 100));
        return card;
    }

    private JComponent createDetailCard() {
        Theme.RoundedPanel card = new Theme.RoundedPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel head = new JPanel(new BorderLayout(0, 10));
        head.setOpaque(false);
        JPanel headTop = new JPanel(new BorderLayout());
        headTop.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(0, 1));
        titles.setOpaque(false);
        titles.add(orderTitle);
        titles.add(orderSubtitle);
        headTop.add(titles, BorderLayout.WEST);
        statusSlot.setOpaque(false);
        headTop.add(statusSlot, BorderLayout.EAST);
        head.add(headTop, BorderLayout.NORTH);
        tracker.setPreferredSize(new Dimension(10, 64));
        head.add(tracker, BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);

        card.add(Theme.scroll(itemsTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 16, 0));
        bottom.setOpaque(false);
        bottom.add(infoBox("Payment",
                new String[]{"Items", "VAT (15%)", "Delivery", "Total paid", "Status"},
                new JLabel[]{itemsTotal, vatTotal, deliveryTotal, grandTotal, payment}));

        JPanel shipBox = infoBox("Shipment",
                new String[]{"Shipped", "Expected"},
                new JLabel[]{shipDate, shipEta});
        shipAddress.setHorizontalAlignment(SwingConstants.LEFT);
        shipBox.add(shipAddress, BorderLayout.SOUTH);
        JPanel shipWrap = new JPanel(new BorderLayout(0, 8));
        shipWrap.setOpaque(false);
        shipWrap.add(shipBox, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
        buttons.setOpaque(false);
        buttons.add(advance);
        buttons.add(cancelOrder);
        shipWrap.add(buttons, BorderLayout.SOUTH);
        bottom.add(shipWrap);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JPanel infoBox(String heading, String[] names, JLabel[] values) {
        Theme.RoundedPanel box = new Theme.RoundedPanel(12, new Color(0xF8FAFB));
        box.setLayout(new BorderLayout(0, 8));
        box.add(Theme.heading(heading), BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(0, 2, 8, 6));
        rows.setOpaque(false);
        for (int i = 0; i < names.length; i++) {
            rows.add(Theme.muted(names[i]));
            rows.add(values[i]);
        }
        box.add(rows, BorderLayout.CENTER);
        return box;
    }

    private void load(String reselectId) {
        emptyState.setText("Loading your orders...");
        Api.async(() -> Api.get("/orders/customer/" + Session.customerId(),
                        new TypeReference<List<Order>>() {}),
                loaded -> {
                    orders = loaded == null ? new ArrayList<>() : new ArrayList<>(loaded);
                    orders.sort(Comparator.comparing(Order::getOrderDate,
                            Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                    fillOrders(reselectId);
                },
                error -> emptyState.setText("Could not load orders: " + Theme.friendly(error)));
    }

    private void fillOrders(String reselectId) {
        ordersModel.setRowCount(0);
        for (Order order : orders) {
            ordersModel.addRow(new Object[]{
                    shortId(order),
                    order.getOrderDate() == null ? "" : order.getOrderDate().format(DATE),
                    itemCount(order),
                    Theme.money(order.getTotalAmount()),
                    prettyStatus(statusOf(order))
            });
        }

        if (orders.isEmpty()) {
            emptyState.setText("No orders yet. Add something from the Shop tab and check out.");
            clearDetails();
            return;
        }

        emptyState.setText(orders.size() + (orders.size() == 1 ? " order" : " orders"));
        int row = 0;
        if (reselectId != null) {
            for (int i = 0; i < orders.size(); i++) {
                if (reselectId.equals(orders.get(i).getOrderId())) {
                    row = i;
                    break;
                }
            }
        }
        ordersTable.setRowSelectionInterval(row, row);
    }


    private void showSelected() {
        int row = ordersTable.getSelectedRow();
        if (row < 0 || row >= orders.size()) {
            return;
        }
        selected = orders.get(row);
        Order order = selected;

        orderTitle.setText("Order " + shortId(order));
        orderSubtitle.setText(order.getOrderDate() == null ? " " : "Placed on " + order.getOrderDate().format(DATE));

        String status = statusOf(order);
        statusSlot.removeAll();
        statusSlot.add(new Theme.Badge(prettyStatus(status), statusColour(status)));
        statusSlot.revalidate();
        statusSlot.repaint();
        tracker.setStatus(status);

        itemsModel.setRowCount(0);
        double itemsSum = 0;
        for (OrderItem item : order.getOrderItems()) {
            double subtotal = item.getQuantity() * item.getPriceAtPurchase();
            itemsSum += subtotal;
            itemsModel.addRow(new Object[]{
                    item.getProduct() == null || item.getProduct().getProductName() == null
                            ? "Product" : item.getProduct().getProductName(),
                    item.getQuantity(),
                    Theme.money(item.getPriceAtPurchase()),
                    Theme.money(subtotal)
            });
        }
        itemsSum = Cart.round(itemsSum);

        double vat = order.getInvoice() == null ? 0 : order.getInvoice().getTaxAmount();
        double delivery = Math.max(0, Cart.round(order.getTotalAmount() - itemsSum - vat));
        itemsTotal.setText(Theme.money(itemsSum));
        vatTotal.setText(Theme.money(vat));
        deliveryTotal.setText(delivery == 0 ? "Free" : Theme.money(delivery));
        grandTotal.setText(Theme.money(order.getTotalAmount()));
        payment.setText(order.getInvoice() == null || order.getInvoice().getInvoiceStatus() == null
                ? "No invoice" : order.getInvoice().getInvoiceStatus());

        Shipment shipment = order.getShipment();
        if (shipment == null) {
            shipAddress.setText("No shipment yet");
            shipDate.setText("-");
            shipEta.setText("-");
            advance.setVisible(false);
            cancelOrder.setVisible(false);
        } else {
            shipAddress.setText(Theme.html("Deliver to: " + shipment.getAddress(), 230));
            shipDate.setText(shipment.getShipmentDate() == null ? "-" : shipment.getShipmentDate().format(DATE));
            shipEta.setText(shipment.getDeliveryDate() == null ? "-" : shipment.getDeliveryDate().format(DATE));
            boolean done = "DELIVERED".equals(status);
            advance.setVisible(!"CANCELLED".equals(status));
            cancelOrder.setVisible("PROCESSING".equals(status));
            advance.setEnabled(!done);
            advance.setText(done ? "Delivered" : "Advance shipment (demo)");
        }
    }

    private void clearDetails() {
        selected = null;
        orderTitle.setText("No orders yet");
        orderSubtitle.setText(" ");
        statusSlot.removeAll();
        statusSlot.repaint();
        tracker.setStatus(null);
        itemsModel.setRowCount(0);
        for (JLabel label : new JLabel[]{itemsTotal, vatTotal, deliveryTotal, grandTotal, payment,
                shipAddress, shipDate, shipEta}) {
            label.setText(" ");
        }
        advance.setVisible(false);
        cancelOrder.setVisible(false);
    }


    private void advanceShipment() {
        if (selected == null || selected.getShipment() == null) {
            return;
        }
        Order order = selected;
        Shipment shipment = order.getShipment();
        String next = nextStatus(statusOf(order));
        if (next == null) {
            return;
        }

        advance.setEnabled(false);
        Api.async(() -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("order", Map.of("orderId", order.getOrderId()));
            body.put("address", shipment.getAddress());
            body.put("shipmentDate", shipment.getShipmentDate() == null
                    ? LocalDateTime.now().withNano(0) : shipment.getShipmentDate());
            body.put("deliveryDate", shipment.getDeliveryDate());
            body.put("status", next);
            Api.put("/shipments/" + shipment.getShipmentId(), body);
            return Boolean.TRUE;
        }, ok -> load(order.getOrderId()),
                error -> {
                    advance.setEnabled(true);
                    Theme.error(this, Theme.friendly(error));
                });
    }


    private void cancelSelected() {
        if (selected == null) {
            return;
        }
        Order order = selected;

        int answer = JOptionPane.showConfirmDialog(this,
                "Cancel order " + shortId(order) + "?\nYour items go back into stock and your payment is refunded.",
                "Cancel order", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        cancelOrder.setEnabled(false);
        Api.async(() -> {
            Api.put("/orders/" + order.getOrderId() + "/cancel", null);
            return Boolean.TRUE;
        }, ok -> {
            cancelOrder.setEnabled(true);
            load(order.getOrderId());
        }, error -> {
            cancelOrder.setEnabled(true);
            Theme.error(this, Theme.friendly(error));
        });
    }


    private static String shortId(Order order) {
        String id = order.getOrderId() == null ? "" : order.getOrderId();
        return id.length() > 8 ? id.substring(0, 8).toUpperCase() : id.toUpperCase();
    }

    private static int itemCount(Order order) {
        int count = 0;
        for (OrderItem item : order.getOrderItems()) {
            count += item.getQuantity();
        }
        return count;
    }

    private static String statusOf(Order order) {
        if (order.getShipment() == null || order.getShipment().getStatus() == null) {
            return "PROCESSING";
        }
        return order.getShipment().getStatus().trim().toUpperCase();
    }

    private static String nextStatus(String current) {
        for (int i = 0; i < STAGES.length - 1; i++) {
            if (STAGES[i].equals(current)) {
                return STAGES[i + 1];
            }
        }
        return null;
    }

    private static String prettyStatus(String status) {
        return switch (status) {
            case "SHIPPED" -> "Shipped";
            case "DELIVERED" -> "Delivered";
            case "CANCELLED" -> "Cancelled";
            default -> "Processing";
        };
    }

    private static Color statusColour(String status) {
        return switch (status) {
            case "SHIPPED" -> Theme.PRIMARY;
            case "DELIVERED" -> Theme.SUCCESS;
            case "CANCELLED" -> Theme.DANGER;
            default -> Theme.WARN;
        };
    }


    private static final class TrackerPanel extends JPanel {
        private int stage = -1;

        TrackerPanel() {
            setOpaque(false);
        }

        void setStatus(String status) {
            stage = -1;
            if (status != null) {
                for (int i = 0; i < STAGES.length; i++) {
                    if (STAGES[i].equals(status)) {
                        stage = i;
                    }
                }
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            String[] names = {"Processing", "Shipped", "Delivered"};
            int margin = 60;
            int usable = getWidth() - margin * 2;
            int y = 18;

            for (int i = 0; i < 3; i++) {
                int x = margin + usable * i / 2;

                if (i < 2) {
                    int nextX = margin + usable * (i + 1) / 2;
                    g2.setColor(i < stage ? Theme.PRIMARY : Theme.BORDER);
                    g2.setStroke(new BasicStroke(4f));
                    g2.drawLine(x, y, nextX, y);
                }
            }
            for (int i = 0; i < 3; i++) {
                int x = margin + usable * i / 2;
                boolean reached = i <= stage;
                g2.setColor(reached ? Theme.PRIMARY : Color.WHITE);
                g2.fillOval(x - 11, y - 11, 22, 22);
                g2.setColor(reached ? Theme.PRIMARY : Theme.BORDER);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(x - 11, y - 11, 22, 22);
                if (reached) {
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x - 4, y, x - 1, y + 3);
                    g2.drawLine(x - 1, y + 3, x + 5, y - 4);
                }
                g2.setFont(Theme.font(i == stage ? Font.BOLD : Font.PLAIN, 13f));
                g2.setColor(reached ? Theme.TEXT : Theme.MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(names[i], x - fm.stringWidth(names[i]) / 2, y + 34);
            }
            g2.dispose();
        }
    }
}
