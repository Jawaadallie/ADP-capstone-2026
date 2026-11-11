package Capstone.ui;

import Capstone.app.Cart;
import Capstone.app.CheckoutService;
import Capstone.app.Session;
import Capstone.model.Address;
import Capstone.net.Api;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class CheckoutDialog extends JDialog {

    private static final String[] PROVINCES = {
            "Western Cape", "Eastern Cape", "Northern Cape", "Gauteng", "KwaZulu-Natal",
            "Free State", "Limpopo", "Mpumalanga", "North West"};

    // delivery fields
    private final JTextField street = new JTextField(20);
    private final JTextField city = new JTextField(20);
    private final JComboBox<String> province = new JComboBox<String>(PROVINCES);
    private final JTextField postal = new JTextField(20);
    private final JTextField country = new JTextField(20);

    // card fields
    private final JTextField holder = new JTextField(20);
    private final JTextField number = new JTextField(20);
    private final JTextField expiry = new JTextField(20);
    private final JPasswordField cvv = new JPasswordField(20);

    private final JLabel problem = new JLabel(" ");
    private final JLabel progress = new JLabel(" ");
    private final JButton pay = new JButton("Place order");
    private final JButton cancel = new JButton("Cancel");

    private boolean orderPlaced = false;

    public CheckoutDialog(JFrame owner) {
        super(owner, "Checkout", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));

        JLabel title = new JLabel("Checkout");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        root.add(title, BorderLayout.NORTH);

        // left side: delivery and card forms, right side: order summary
        JPanel forms = new JPanel(new GridLayout(0, 1, 0, 14));
        forms.add(createDeliveryPanel());
        forms.add(createCardPanel());

        JPanel centre = new JPanel(new GridLayout(1, 2, 16, 0));
        centre.add(forms);
        centre.add(createSummaryPanel());
        root.add(centre, BorderLayout.CENTER);

        // bottom: messages and buttons
        problem.setForeground(Color.RED);

        JPanel messages = new JPanel(new GridLayout(0, 1));
        messages.add(problem);
        messages.add(progress);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.add(cancel);
        buttons.add(pay);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(messages, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pay.setText("Place order  -  " + money(Cart.get().total()));
        pay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placeOrder();
            }
        });

        setContentPane(root);
        setSize(960, 700);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(pay);

        country.setText("South Africa");
        prefillAddress();
    }

    public boolean isOrderPlaced() {
        return orderPlaced;
    }


    private JPanel createDeliveryPanel() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Street"));
        fields.add(street);
        fields.add(new JLabel("City"));
        fields.add(city);
        fields.add(new JLabel("Province"));
        fields.add(province);
        fields.add(new JLabel("Postal code"));
        fields.add(postal);
        fields.add(new JLabel("Country"));
        fields.add(country);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Delivery address"));
        panel.add(fields, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createCardPanel() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Name on card"));
        fields.add(holder);
        fields.add(new JLabel("Card number"));
        fields.add(number);
        fields.add(new JLabel("Expiry (MM/YY)"));
        fields.add(expiry);
        fields.add(new JLabel("CVV"));
        fields.add(cvv);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Payment (demo - no real charge)"));
        panel.add(fields, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createSummaryPanel() {
        Cart cart = Cart.get();

        // one line of text for every item in the cart
        String items = "";
        for (Cart.Line line : cart.getLines()) {
            items = items + line.getQuantity() + " x " + line.getProduct().getProductName()
                    + "   " + money(line.getLineTotal()) + "\n";
        }
        JTextArea list = new JTextArea(items);
        list.setEditable(false);

        String deliveryText = "Free";
        if (cart.shipping() != 0) {
            deliveryText = money(cart.shipping());
        }

        JPanel totals = new JPanel(new GridLayout(0, 2, 6, 6));
        totals.add(new JLabel("Subtotal"));
        totals.add(new JLabel(money(cart.subtotal())));
        totals.add(new JLabel("VAT (15%)"));
        totals.add(new JLabel(money(cart.vat())));
        totals.add(new JLabel("Delivery"));
        totals.add(new JLabel(deliveryText));
        totals.add(new JLabel("Total"));
        totals.add(new JLabel(money(cart.total())));
        totals.add(new JLabel("Estimated delivery"));
        totals.add(new JLabel("3-5 business days"));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Order summary"));
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(totals, BorderLayout.SOUTH);
        return panel;
    }

    private String money(double value) {
        return "R " + String.format("%.2f", value);
    }


    private void prefillAddress() {
        SwingWorker<List<Address>, Void> worker = new SwingWorker<List<Address>, Void>() {
            @Override
            protected List<Address> doInBackground() throws Exception {
                return Api.get("/addresses/customer/" + Session.customerId(),
                        new TypeReference<List<Address>>() {});
            }

            @Override
            protected void done() {
                try {
                    List<Address> addresses = get();
                    if (addresses != null && addresses.size() > 0 && street.getText().isEmpty()) {
                        // use the most recently saved address
                        Address a = addresses.get(addresses.size() - 1);
                        street.setText(a.getStreetAddress());
                        city.setText(a.getCity());
                        postal.setText(a.getPostalCode());
                        if (a.getCountry() != null) {
                            country.setText(a.getCountry());
                        }
                        if (a.getState() != null) {
                            province.setSelectedItem(a.getState());
                        }
                    }
                } catch (Exception e) {
                    // a missing saved address is fine, the customer just types it in
                }
            }
        };
        worker.execute();
    }

    private String checkInputs() {
        if (street.getText().trim().isEmpty() || city.getText().trim().isEmpty()
                || postal.getText().trim().isEmpty() || country.getText().trim().isEmpty()) {
            return "Please complete your delivery address.";
        }

        // \\d{4} means exactly 4 digits
        if (!postal.getText().trim().matches("\\d{4}")) {
            return "Postal code must be 4 digits.";
        }

        if (holder.getText().trim().isEmpty()) {
            return "Please enter the name on the card.";
        }

        // remove spaces and dashes, then the rest must be 13 to 19 digits
        String digits = number.getText().replace(" ", "").replace("-", "");
        if (!digits.matches("\\d{13,19}")) {
            return "Card number must be 13 to 19 digits.";
        }

        // the expiry must look like 08/28 (month 01-12, a slash, two digits)
        String exp = expiry.getText().trim();
        if (!exp.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return "Expiry must look like 08/28.";
        }
        int month = Integer.parseInt(exp.substring(0, 2));
        int year = 2000 + Integer.parseInt(exp.substring(3, 5));
        if (YearMonth.of(year, month).isBefore(YearMonth.now())) {
            return "That card has expired.";
        }

        String cvvText = new String(cvv.getPassword());
        if (!cvvText.matches("\\d{3,4}")) {
            return "CVV must be 3 or 4 digits.";
        }

        return null;
    }


    private void placeOrder() {
        if (Cart.get().isEmpty()) {
            problem.setText("Your cart is empty.");
            return;
        }

        String error = checkInputs();
        if (error != null) {
            problem.setText(error);
            return;
        }
        problem.setText(" ");

        final CheckoutService.Delivery delivery = new CheckoutService.Delivery(
                street.getText().trim(),
                city.getText().trim(),
                String.valueOf(province.getSelectedItem()),
                postal.getText().trim(),
                country.getText().trim());

        // only the digits are kept, the CVV is never passed on
        final CheckoutService.CardInfo card = new CheckoutService.CardInfo(
                holder.getText().trim(),
                number.getText().replace(" ", "").replace("-", ""),
                expiry.getText().trim());

        final List<Cart.Line> lines = Cart.get().getLines();

        pay.setEnabled(false);
        cancel.setEnabled(false);

        SwingWorker<CheckoutService.Receipt, String> worker =
                new SwingWorker<CheckoutService.Receipt, String>() {

                    @Override
                    protected CheckoutService.Receipt doInBackground() throws Exception {
                        // CheckoutService calls this each time it starts a new step
                        Consumer<String> reporter = new Consumer<String>() {
                            @Override
                            public void accept(String text) {
                                publish(text);
                            }
                        };
                        return CheckoutService.place(Session.customer(), lines, delivery, card, reporter);
                    }

                    @Override
                    protected void process(List<String> messages) {
                        // show the newest progress message
                        progress.setText(messages.get(messages.size() - 1));
                    }

                    @Override
                    protected void done() {
                        try {
                            CheckoutService.Receipt receipt = get();
                            orderPlaced = true;
                            Cart.get().clear();
                            dispose();
                            JOptionPane.showMessageDialog(getOwner(),
                                    "Thank you! Your order " + receipt.shortId() + " is confirmed.\n\n"
                                            + "Total paid: " + money(receipt.total()) + "\n"
                                            + "Estimated delivery: "
                                            + receipt.estimatedDelivery().toLocalDate() + "\n\n"
                                            + "Track it on the My Orders tab.",
                                    "Order confirmed", JOptionPane.INFORMATION_MESSAGE);
                        } catch (ExecutionException e) {
                            // e.getCause() is the real error thrown by CheckoutService
                            showFailure(e.getCause().getMessage());
                        } catch (InterruptedException e) {
                            showFailure("The order was interrupted. Please try again.");
                        }
                    }
                };
        worker.execute();
    }

    private void showFailure(String text) {
        problem.setText(text);
        progress.setText(" ");
        pay.setEnabled(true);
        cancel.setEnabled(true);
    }
}