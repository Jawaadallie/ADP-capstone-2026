package Capstone.app;

import Capstone.model.Product;

import java.util.ArrayList;
import java.util.List;

public final class Cart {

    public static final double VAT_RATE = 0.15;
    public static final double FREE_SHIPPING_FROM = 500.00;
    public static final double SHIPPING_FEE = 50.00;

    public static final class Line {
        private final Product product;
        private int quantity;
        private int maxStock;

        private Line(Product product, int quantity, int maxStock) {
            this.product = product;
            this.quantity = quantity;
            this.maxStock = maxStock;
        }

        public Product getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public int getMaxStock() { return maxStock; }
        public double getLineTotal() { return round(product.getCurrentPrice() * quantity); }
    }

    private static final Cart INSTANCE = new Cart();

    private final List<Line> lines = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private Cart() {}

    public static Cart get() {
        return INSTANCE;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void changed() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    public boolean add(Product product, int quantity, int stock) {
        for (Line line : lines) {
            if (line.product.getProductId().equals(product.getProductId())) {
                line.maxStock = stock;
                if (line.quantity + quantity > stock) {
                    return false;
                }
                line.quantity += quantity;
                changed();
                return true;
            }
        }
        if (quantity > stock) {
            return false;
        }
        lines.add(new Line(product, quantity, stock));
        changed();
        return true;
    }

    public boolean setQuantity(Line line, int quantity) {
        if (quantity <= 0) {
            remove(line);
            return true;
        }
        if (quantity > line.maxStock) {
            return false;
        }
        line.quantity = quantity;
        changed();
        return true;
    }

    public void remove(Line line) {
        lines.remove(line);
        changed();
    }

    public void clear() {
        lines.clear();
        changed();
    }

    public List<Line> getLines() {
        return new ArrayList<>(lines);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public int itemCount() {
        int count = 0;
        for (Line line : lines) {
            count += line.quantity;
        }
        return count;
    }

    public double subtotal() {
        double sum = 0;
        for (Line line : lines) {
            sum += line.getLineTotal();
        }
        return round(sum);
    }

    public double vat() {
        return round(subtotal() * VAT_RATE);
    }

    public double shipping() {
        double sub = subtotal();
        if (sub == 0 || sub >= FREE_SHIPPING_FROM) {
            return 0;
        }
        return SHIPPING_FEE;
    }

    public double total() {
        return round(subtotal() + vat() + shipping());
    }

    public static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
