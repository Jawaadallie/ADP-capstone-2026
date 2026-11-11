package Capstone.ui;

import Capstone.model.Inventory;
import Capstone.model.Product;
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

public class AdminProductsPanel extends JPanel {

    private static class Row {
        Product product;
        Inventory inventory;   // null when the product has no stock record yet
    }

    private final List<Row> rows = new ArrayList<>();
    private final DefaultTableModel model = Theme.readOnlyModel("Product", "Price", "Stock", "Warehouse");
    private final JTable table = Theme.table(model);

    private final JTextField nameField = Theme.field(18);
    private final JTextField descriptionField = Theme.field(18);
    private final JTextField priceField = Theme.field(18);
    private final JTextField stockField = Theme.field(18);
    private final JTextField warehouseField = Theme.field(18);

    private final JLabel formTitle = Theme.heading("New product");
    private final JLabel message = Theme.muted(" ");
    private final JButton saveButton = Theme.primary("Add product");
    private final JButton clearButton = Theme.ghost("Clear form");
    private final JButton deleteButton = Theme.ghost("Delete");

    private Row selected;   // null means we are adding a new product

    public AdminProductsPanel() {
        setLayout(new BorderLayout(18, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 24, 18, 24));

        add(createTablePanel(), BorderLayout.CENTER);
        add(createFormPanel(), BorderLayout.EAST);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedRow();
            }
        });
        saveButton.addActionListener(e -> save());
        clearButton.addActionListener(e -> clearForm());
        deleteButton.addActionListener(e -> delete());

        clearForm();
        load();
    }

    // ---------- layout ----------

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(Theme.title("Manage Products"), BorderLayout.NORTH);
        panel.add(Theme.scroll(table), BorderLayout.CENTER);
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
        panel.add(formTitle, gbc);

        row = addField(panel, gbc, row, "Name", nameField);
        row = addField(panel, gbc, row, "Description", descriptionField);
        row = addField(panel, gbc, row, "Price (R)", priceField);
        row = addField(panel, gbc, row, "Stock quantity", stockField);
        row = addField(panel, gbc, row, "Warehouse", warehouseField);

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 8));
        buttons.setOpaque(false);
        buttons.add(saveButton);
        buttons.add(clearButton);
        buttons.add(deleteButton);

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

    private int addField(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridy = row;
        gbc.insets = new Insets(8, 0, 0, 0);
        panel.add(Theme.label(label, Font.PLAIN, 13f, Theme.MUTED), gbc);
        gbc.gridy = row + 1;
        gbc.insets = new Insets(2, 0, 3, 0);
        panel.add(field, gbc);
        return row + 2;
    }


    private void load() {
        showMessage("Loading products...", false);
        Api.async(this::fetch, result -> {
            rows.clear();
            rows.addAll(result);
            fillTable();
            showMessage(" ", false);
        }, error -> showError(error));
    }

    private List<Row> fetch() throws Exception {
        List<Product> products = Api.get("/products", new TypeReference<List<Product>>() {});
        List<Row> result = new ArrayList<>();
        if (products == null) {
            return result;
        }
        for (Product product : products) {
            Row row = new Row();
            row.product = product;
            try {
                row.inventory = Api.get("/inventory/product/" + product.getProductId(), Inventory.class);
            } catch (Exception e) {
                row.inventory = null;   // no stock record yet
            }
            result.add(row);
        }
        return result;
    }

    private void fillTable() {
        model.setRowCount(0);
        for (Row row : rows) {
            model.addRow(new Object[]{
                    row.product.getProductName(),
                    Theme.money(row.product.getCurrentPrice()),
                    row.inventory == null ? 0 : row.inventory.getStockQuantity(),
                    row.inventory == null ? "-" : row.inventory.getWarehouseLocation()
            });
        }
    }


    private void showSelectedRow() {
        int index = table.getSelectedRow();
        if (index < 0 || index >= rows.size()) {
            return;
        }
        selected = rows.get(index);
        Product product = selected.product;

        nameField.setText(product.getProductName());
        descriptionField.setText(product.getDescription());
        priceField.setText(String.valueOf(product.getCurrentPrice()));
        stockField.setText(selected.inventory == null ? "0" : String.valueOf(selected.inventory.getStockQuantity()));
        warehouseField.setText(selected.inventory == null ? "Cape Town Warehouse" : selected.inventory.getWarehouseLocation());

        formTitle.setText("Edit product");
        saveButton.setText("Save changes");
        deleteButton.setEnabled(true);
        showMessage(" ", false);
    }

    private void clearForm() {
        selected = null;
        table.clearSelection();
        nameField.setText("");
        descriptionField.setText("");
        priceField.setText("");
        stockField.setText("0");
        warehouseField.setText("Cape Town Warehouse");

        formTitle.setText("New product");
        saveButton.setText("Add product");
        deleteButton.setEnabled(false);
    }


    private void save() {
        String name = nameField.getText().trim();
        String description = descriptionField.getText().trim();
        String warehouse = warehouseField.getText().trim();

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceField.getText().trim().replace(',', '.'));
            stock = Integer.parseInt(stockField.getText().trim());
        } catch (NumberFormatException e) {
            showMessage("Price must be a number (like 49.99) and stock a whole number.", true);
            return;
        }

        if (name.isEmpty() || description.isEmpty() || warehouse.isEmpty()) {
            showMessage("Please fill in the name, description and warehouse.", true);
            return;
        }
        if (price <= 0) {
            showMessage("The price must be more than zero.", true);
            return;
        }
        if (stock < 0) {
            showMessage("Stock cannot be negative.", true);
            return;
        }

        final double finalPrice = price;
        final int finalStock = stock;
        final Row editing = selected;

        setBusy(true);
        showMessage("Saving...", false);

        Api.async(() -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("productName", name);
            body.put("description", description);
            body.put("currentPrice", finalPrice);

            String productId;
            if (editing == null) {
                Product created = Api.post("/products", body, Product.class);
                productId = created.getProductId();
            } else {
                productId = editing.product.getProductId();
                Api.put("/products/" + productId, body);
            }

            // Saving a product can remove its stock record, so always write the stock again.
            saveStock(productId, finalStock, warehouse);
            return Boolean.TRUE;
        }, done -> {
            setBusy(false);
            clearForm();
            showMessage("Saved.", false);
            load();
        }, error -> {
            setBusy(false);
            showError(error);
        });
    }

    private static void saveStock(String productId, int stock, String warehouse) throws Exception {
        Inventory existing = null;
        try {
            existing = Api.get("/inventory/product/" + productId, Inventory.class);
        } catch (Exception ignored) {
            // no stock record yet
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("product", Map.of("productId", productId));
        body.put("stockQuantity", stock);
        body.put("warehouseLocation", warehouse);

        if (existing == null || existing.getInventoryId() == null) {
            Api.post("/inventory", body);
        } else {
            Api.put("/inventory/" + existing.getInventoryId(), body);
        }
    }


    private void delete() {
        if (selected == null) {
            return;
        }
        final String productId = selected.product.getProductId();
        String name = selected.product.getProductName();

        int answer = JOptionPane.showConfirmDialog(this,
                "Delete \"" + name + "\"? This cannot be undone.",
                "Delete product", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }

        setBusy(true);
        showMessage("Deleting...", false);

        Api.async(() -> {
            Api.delete("/products/" + productId);
            return Boolean.TRUE;
        }, done -> {
            setBusy(false);
            clearForm();
            showMessage("Product deleted.", false);
            load();
        }, error -> {
            setBusy(false);
            showError(error);
        });
    }


    private void setBusy(boolean busy) {
        saveButton.setEnabled(!busy);
        clearButton.setEnabled(!busy);
        deleteButton.setEnabled(!busy && selected != null);
    }

    private void showMessage(String text, boolean isError) {
        message.setForeground(isError ? Theme.DANGER : Theme.MUTED);
        message.setText("<html>" + text + "</html>");
    }

    private void showError(Throwable error) {
        showMessage(Theme.friendly(error), true);
    }
}