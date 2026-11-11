package Capstone.ui;

import Capstone.app.Cart;
import Capstone.model.Inventory;
import Capstone.model.Product;
import Capstone.model.Review;
import Capstone.net.Api;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CatalogPanel extends JPanel {

    private static final Color[][] PALETTE = {
            {new Color(0x0E7C86), new Color(0x35B3BC)},
            {new Color(0x2F6FDE), new Color(0x6FA2F5)},
            {new Color(0x7A4CC2), new Color(0xA98BE0)},
            {new Color(0xD9731A), new Color(0xF3A45E)},
            {new Color(0x2E9E5B), new Color(0x6CCB93)},
            {new Color(0xC2415C), new Color(0xE67F95)}
    };

    private static final class Item {
        Product product;
        int stock;
        double averageRating;
        int reviewCount;
    }

    private final JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
    private final JTextField search = Theme.field(18);
    private final JComboBox<String> sort = new JComboBox<>(new String[]{
            "Name (A-Z)", "Price (low to high)", "Price (high to low)", "Best rated"});
    private final JLabel status = Theme.muted("Loading products...");
    private final JScrollPane scroll;

    private List<Item> items = new ArrayList<>();
    private int columns = 3;

    public CatalogPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 24, 12, 24));

        add(createToolbar(), BorderLayout.NORTH);

        grid.setOpaque(false);
        JPanel wrapper = new Theme.ViewportPanel(new BorderLayout());
        wrapper.add(grid, BorderLayout.NORTH);

        scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);

        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int wanted = Math.max(1, scroll.getViewport().getWidth() / 300);
                if (wanted != columns) {
                    columns = wanted;
                    grid.setLayout(new GridLayout(0, columns, 18, 18));
                    render();
                }
            }
        });

        load();
    }

    private JPanel createToolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        JPanel left = new JPanel(new GridLayout(0, 1));
        left.setOpaque(false);
        left.add(Theme.title("Shop"));
        left.add(status);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        search.putClientProperty("JTextField.placeholderText", "Search products");
        search.setToolTipText("Search by name or description");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { render(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { render(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { render(); }
        });
        sort.addActionListener(e -> render());
        right.add(Theme.muted("Search"));
        right.add(search);
        right.add(Theme.muted("Sort"));
        right.add(sort);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }


    public void load() {
        status.setText("Loading products...");
        Api.async(this::fetch, loaded -> {
            grid.setLayout(new GridLayout(0, columns, 18, 18));
            items = loaded;
            status.setText(items.size() + " products available");
            render();
        }, error -> {
            items = new ArrayList<>();
            grid.removeAll();
            status.setText("Could not load products");
            showLoadError(error);
        });
    }

    private List<Item> fetch() throws Exception {
        List<Product> products = Api.get("/products", new TypeReference<List<Product>>() {});
        List<Item> result = new ArrayList<>();
        if (products == null) {
            return result;
        }
        for (Product product : products) {
            Item item = new Item();
            item.product = product;

            try {
                Inventory inventory = Api.get("/inventory/product/" + product.getProductId(), Inventory.class);
                item.stock = inventory == null ? 0 : inventory.getStockQuantity();
            } catch (Exception e) {
                item.stock = 0;
            }

            try {
                List<Review> reviews = Api.get("/reviews/product/" + product.getProductId(),
                        new TypeReference<List<Review>>() {});
                if (reviews != null && !reviews.isEmpty()) {
                    double sum = 0;
                    for (Review review : reviews) {
                        sum += review.getRating();
                    }
                    item.reviewCount = reviews.size();
                    item.averageRating = sum / reviews.size();
                }
            } catch (Exception ignored) {
                // a product without reviews is fine
            }
            result.add(item);
        }
        return result;
    }

    private void showLoadError(Throwable error) {
        grid.removeAll();
        grid.setLayout(new BorderLayout());
        JPanel box = new Theme.RoundedPanel(16);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(Theme.heading("We could not load the shop"));
        box.add(Box.createVerticalStrut(6));
        box.add(Theme.muted(Theme.friendly(error)));
        box.add(Box.createVerticalStrut(12));
        JButton retry = Theme.primary("Try again");
        retry.setAlignmentX(Component.LEFT_ALIGNMENT);
        retry.addActionListener(e -> {
            grid.setLayout(new GridLayout(0, columns, 18, 18));
            load();
        });
        box.add(retry);
        grid.add(box, BorderLayout.NORTH);
        grid.revalidate();
        grid.repaint();
    }


    private void render() {
        if (grid.getLayout() instanceof BorderLayout) {
            return;
        }

        String query = search.getText().trim().toLowerCase();
        List<Item> shown = new ArrayList<>();
        for (Item item : items) {
            String name = item.product.getProductName() == null ? "" : item.product.getProductName().toLowerCase();
            String description = item.product.getDescription() == null ? "" : item.product.getDescription().toLowerCase();
            if (query.isEmpty() || name.contains(query) || description.contains(query)) {
                shown.add(item);
            }
        }

        switch (sort.getSelectedIndex()) {
            case 1 -> shown.sort(Comparator.comparingDouble(i -> i.product.getCurrentPrice()));
            case 2 -> shown.sort(Comparator.comparingDouble((Item i) -> i.product.getCurrentPrice()).reversed());
            case 3 -> shown.sort(Comparator.comparingDouble((Item i) -> i.averageRating).reversed());
            default -> shown.sort(Comparator.comparing(i -> String.valueOf(i.product.getProductName()).toLowerCase()));
        }

        grid.removeAll();
        for (Item item : shown) {
            grid.add(createCard(item));
        }
        if (shown.isEmpty() && !items.isEmpty()) {
            grid.add(Theme.muted("No products match your search."));
        }
        grid.revalidate();
        grid.repaint();
    }

    private JPanel createCard(Item item) {
        Product product = item.product;

        Theme.RoundedPanel card = new Theme.RoundedPanel(18);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 12, 14, 12));

        card.add(createBanner(product), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel name = Theme.label(product.getProductName(), Font.BOLD, 16f, Theme.TEXT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(name);
        body.add(Box.createVerticalStrut(4));

        JLabel description = Theme.label(Theme.html(product.getDescription(), 215), Font.PLAIN, 13f, Theme.MUTED);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(description);
        body.add(Box.createVerticalStrut(8));

        String ratingText = item.reviewCount == 0
                ? "No reviews yet"
                : Theme.stars((int) Math.round(item.averageRating))
                + String.format("  %.1f (%d)", item.averageRating, item.reviewCount);
        JLabel rating = Theme.label(ratingText, Font.PLAIN, 13f, item.reviewCount == 0 ? Theme.MUTED : Theme.WARN);
        if (item.reviewCount > 0) {
            rating.setFont(Theme.starFont(Font.PLAIN, 13f));
        }
        rating.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(rating);
        card.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);

        JPanel priceRow = new JPanel(new BorderLayout());
        priceRow.setOpaque(false);
        priceRow.add(Theme.label(Theme.money(product.getCurrentPrice()), Font.BOLD, 20f, Theme.PRIMARY_DARK),
                BorderLayout.WEST);
        priceRow.add(stockBadge(item.stock), BorderLayout.EAST);
        bottom.add(priceRow, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        JButton add = Theme.primary("Add to cart");
        add.setEnabled(item.stock > 0);
        add.addActionListener(e -> addToCart(item, add));
        JButton reviews = Theme.ghost("Reviews");
        reviews.addActionListener(e -> showReviews(product));
        buttons.add(add);
        buttons.add(reviews);
        bottom.add(buttons, BorderLayout.SOUTH);

        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private JComponent stockBadge(int stock) {
        if (stock <= 0) {
            return new Theme.Badge("Out of stock", Theme.DANGER);
        }
        if (stock < 10) {
            return new Theme.Badge("Only " + stock + " left", Theme.WARN);
        }
        return new Theme.Badge("In stock", Theme.SUCCESS);
    }

    private JComponent createBanner(Product product) {
        String text = product.getProductName() == null || product.getProductName().isBlank()
                ? "?" : product.getProductName().substring(0, 1).toUpperCase();
        Color[] colours = PALETTE[Math.abs(String.valueOf(product.getProductName()).hashCode()) % PALETTE.length];

        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setPaint(new GradientPaint(0, 0, colours[0], getWidth(), getHeight(), colours[1]));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 55));
                g2.fillOval(getWidth() - 70, -30, 120, 120);
                g2.setClip(null);
                g2.setColor(Color.WHITE);
                g2.setFont(Theme.font(Font.BOLD, 48f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setPreferredSize(new Dimension(10, 100));
        return banner;
    }


    private void addToCart(Item item, JButton button) {
        boolean added = Cart.get().add(item.product, 1, item.stock);
        if (added) {
            button.setText("Added");
            Timer timer = new Timer(900, e -> button.setText("Add to cart"));
            timer.setRepeats(false);
            timer.start();
        } else {
            status.setText("You already have all " + item.stock + " in stock in your cart.");
        }
    }

    private void showReviews(Product product) {
        Api.async(() -> Api.get("/reviews/product/" + product.getProductId(),
                        new TypeReference<List<Review>>() {}),
                reviews -> {
                    JPanel list = new JPanel();
                    list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
                    list.setBackground(Theme.SURFACE);
                    list.setBorder(new EmptyBorder(8, 8, 8, 8));

                    if (reviews == null || reviews.isEmpty()) {
                        list.add(Theme.muted("No reviews yet. Be the first on the Reviews tab."));
                    } else {
                        for (Review review : reviews) {
                            list.add(Theme.starLabel(Theme.stars(review.getRating()), 15f, Theme.WARN));
                            list.add(Theme.label(Theme.html(review.getComment(), 340), Font.PLAIN, 13f, Theme.TEXT));
                            list.add(Box.createVerticalStrut(12));
                        }
                    }
                    JScrollPane pane = new JScrollPane(list);
                    pane.setPreferredSize(new Dimension(400, 280));
                    JOptionPane.showMessageDialog(this, pane,
                            "Reviews: " + product.getProductName(), JOptionPane.PLAIN_MESSAGE);
                },
                error -> Theme.error(this, Theme.friendly(error)));
    }
}
