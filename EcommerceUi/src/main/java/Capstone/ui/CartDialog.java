package Capstone.ui;

import Capstone.app.Cart;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CartDialog extends JDialog {

    private final Cart cart = Cart.get();
    private final JPanel rows = new JPanel();
    private final JPanel totals = new JPanel(new GridLayout(0, 2, 6, 6));
    private final JLabel notice = new JLabel(" ");
    private final JButton checkout = new JButton("Checkout");
    private boolean checkoutRequested = false;

    public CartDialog(JFrame owner) {
        super(owner, "Your cart", true);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        JLabel title = new JLabel("Your cart");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        root.add(title, BorderLayout.NORTH);

        // the wrapper keeps the rows at the top instead of stretching them
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        JPanel rowsWrapper = new JPanel(new BorderLayout());
        rowsWrapper.add(rows, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(rowsWrapper);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        root.add(scroll, BorderLayout.CENTER);

        notice.setForeground(Color.ORANGE.darker());

        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.add(totals, BorderLayout.NORTH);
        south.add(notice, BorderLayout.CENTER);

        JButton keepShopping = new JButton("Continue shopping");
        keepShopping.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        JButton clear = new JButton("Empty cart");
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cart.clear();
                refresh();
            }
        });

        checkout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkoutRequested = true;
                dispose();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.add(clear);
        buttons.add(keepShopping);
        buttons.add(checkout);
        south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(640, 560);
        setLocationRelativeTo(owner);
        refresh();
    }

    public boolean isCheckoutRequested() {
        return checkoutRequested;
    }


    private String money(double value) {
        return "R " + String.format("%.2f", value);
    }

    private void refresh() {
        rows.removeAll();

        if (cart.isEmpty()) {
            JLabel empty = new JLabel("Your cart is empty. Add some sprays from the Shop tab.");
            rows.add(empty);
        }

        for (Cart.Line line : cart.getLines()) {
            rows.add(createRow(line));
            rows.add(Box.createVerticalStrut(8));
        }

        String deliveryText = money(cart.shipping());
        if (cart.shipping() == 0 && !cart.isEmpty()) {
            deliveryText = "Free";
        }

        totals.removeAll();
        addTotal("Subtotal", money(cart.subtotal()));
        addTotal("VAT (15%)", money(cart.vat()));
        addTotal("Delivery", deliveryText);
        addTotal("Total", money(cart.total()));

        double toFree = Cart.FREE_SHIPPING_FROM - cart.subtotal();
        if (!cart.isEmpty() && toFree > 0) {
            notice.setText("Spend " + money(toFree) + " more for free delivery.");
        } else {
            notice.setText(" ");
        }

        checkout.setEnabled(!cart.isEmpty());

        rows.revalidate();
        rows.repaint();
        totals.revalidate();
        totals.repaint();
    }

    private void addTotal(String label, String value) {
        JLabel right = new JLabel(value);
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        totals.add(new JLabel(label));
        totals.add(right);
    }


    private JPanel createRow(final Cart.Line line) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // name and price each (middle)
        JPanel info = new JPanel(new GridLayout(0, 1));
        info.add(new JLabel(line.getProduct().getProductName()));
        info.add(new JLabel(money(line.getProduct().getCurrentPrice()) + " each"));
        row.add(info, BorderLayout.CENTER);

        // - 2 + buttons (left)
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cart.setQuantity(line, line.getQuantity() - 1);
                refresh();
            }
        });

        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean worked = cart.setQuantity(line, line.getQuantity() + 1);
                if (worked) {
                    refresh();
                } else {
                    notice.setText("Only " + line.getMaxStock() + " of "
                            + line.getProduct().getProductName() + " in stock.");
                }
            }
        });

        JPanel qty = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        qty.add(minus);
        qty.add(new JLabel(String.valueOf(line.getQuantity())));
        qty.add(plus);
        row.add(qty, BorderLayout.WEST);

        // line total and remove button (right)
        JButton remove = new JButton("Remove");
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cart.remove(line);
                refresh();
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.add(new JLabel(money(line.getLineTotal())));
        right.add(remove);
        row.add(right, BorderLayout.EAST);

        return row;
    }
}