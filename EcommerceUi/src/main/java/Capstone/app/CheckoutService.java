package Capstone.app;

import Capstone.model.Customer;
import Capstone.model.Inventory;
import Capstone.model.Order;
import Capstone.net.Api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CheckoutService {

    public record Delivery(String street, String city, String province,
                           String postalCode, String country) {
        public String oneLine() {
            return street + ", " + city + ", " + province + ", " + postalCode + ", " + country;
        }
    }

    public record CardInfo(String holder, String number, String expiry) {
        public String last4() {
            return number.substring(number.length() - 4);
        }

        public String type() {
            char first = number.charAt(0);
            if (first == '4') return "VISA";
            if (first == '5' || first == '2') return "MASTERCARD";
            if (first == '3') return "AMEX";
            return "CARD";
        }
    }

    public record Receipt(String orderId, double total, LocalDateTime estimatedDelivery) {
        public String shortId() {
            return orderId.length() > 8 ? orderId.substring(0, 8).toUpperCase() : orderId.toUpperCase();
        }
    }

    private CheckoutService() {}

    public static Receipt place(Customer customer, List<Cart.Line> lines, Delivery delivery,
                                CardInfo card, Consumer<String> progress) throws Exception {

        Cart cart = Cart.get();
        double subtotal = cart.subtotal();
        double vat = cart.vat();
        double total = cart.total();
        String customerId = customer.getCustomerId();

        // 1. Make sure everything is still in stock
        progress.accept("Checking stock...");
        Map<String, Inventory> stock = new LinkedHashMap<>();
        for (Cart.Line line : lines) {
            String productId = line.getProduct().getProductId();
            Inventory inventory = Api.get("/inventory/product/" + productId, Inventory.class);
            int available = inventory == null ? 0 : inventory.getStockQuantity();
            if (available < line.getQuantity()) {
                throw new Exception("Sorry, only " + available + " of \""
                        + line.getProduct().getProductName() + "\" left in stock.");
            }
            stock.put(productId, inventory);
        }

        // 2. The order itself
        progress.accept("Creating your order...");
        LocalDateTime now = LocalDateTime.now().withNano(0);

        Map<String, Object> orderBody = new LinkedHashMap<>();
        orderBody.put("customer", Map.of("customerId", customerId));
        orderBody.put("orderDate", now);
        orderBody.put("totalAmount", total);

        Order order = Api.post("/orders", orderBody, Order.class);
        if (order == null || order.getOrderId() == null) {
            throw new Exception("The server did not return an order number.");
        }
        String orderId = order.getOrderId();

        LocalDateTime delivery5 = now.plusDays(5);

        try {
            // 3. The items, with the price paid at the time
            progress.accept("Adding your items...");
            for (Cart.Line line : lines) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("order", Map.of("orderId", orderId));
                item.put("product", Map.of("productId", line.getProduct().getProductId()));
                item.put("quantity", line.getQuantity());
                item.put("priceAtPurchase", line.getProduct().getCurrentPrice());
                Api.post("/order-items", item);
            }

            // 4. Payment. Only the last 4 digits are stored and the CVV is never sent.
            progress.accept("Processing payment...");
            Map<String, Object> cardBody = new LinkedHashMap<>();
            cardBody.put("cardHolderName", card.holder());
            cardBody.put("cardType", card.type());
            cardBody.put("cardNumber", "**** **** **** " + card.last4());
            cardBody.put("cardExpiry", card.expiry());
            Capstone.model.Card savedCard = Api.post("/cards", cardBody, Capstone.model.Card.class);
            if (savedCard == null || savedCard.getCardId() == null) {
                throw new Exception("The card could not be saved.");
            }

            Map<String, Object> payment = new LinkedHashMap<>();
            payment.put("order", Map.of("orderId", orderId));
            payment.put("card", Map.of("cardId", savedCard.getCardId()));
            payment.put("paymentAmount", total);
            payment.put("paymentMethod", "CARD");
            Api.post("/payments", payment);

            // 5. Invoice
            progress.accept("Creating your invoice...");
            Map<String, Object> invoice = new LinkedHashMap<>();
            invoice.put("order", Map.of("orderId", orderId));
            invoice.put("totalAmount", total);
            invoice.put("taxAmount", vat);
            invoice.put("invoiceStatus", "PAID");
            Api.post("/invoices", invoice);

            // 6. Shipment
            progress.accept("Arranging delivery...");
            Map<String, Object> shipment = new LinkedHashMap<>();
            shipment.put("order", Map.of("orderId", orderId));
            shipment.put("address", delivery.oneLine());
            shipment.put("shipmentDate", now);
            shipment.put("deliveryDate", delivery5);
            shipment.put("status", "PROCESSING");
            Api.post("/shipments", shipment);



        } catch (Exception e) {
            progress.accept("Something went wrong, undoing the order...");

            try {
                Api.delete("/orders/" + orderId);
            } catch (Exception ignored) {
                // best effort
            }
            throw e;
        }

        // 8. Nice-to-haves: they must never make a paid order fail
        try {
            saveAddressIfNew(customerId, delivery);
        } catch (Exception ignored) {
        }
        try {
            Map<String, Object> note = new LinkedHashMap<>();
            note.put("customer", Map.of("customerId", customerId));
            note.put("message", "Order " + new Receipt(orderId, total, delivery5).shortId()
                    + " confirmed. Estimated delivery in 3-5 business days.");
            note.put("notificationDate", now);
            note.put("status", "UNREAD");
            Api.post("/notifications", note);
        } catch (Exception ignored) {
        }

        return new Receipt(orderId, total, delivery5);
    }
    

    private static void saveAddressIfNew(String customerId, Delivery d) throws Exception {
        List<Capstone.model.Address> saved = Api.get("/addresses/customer/" + customerId,
                new com.fasterxml.jackson.core.type.TypeReference<List<Capstone.model.Address>>() {});
        if (saved != null) {
            for (Capstone.model.Address a : saved) {
                if (d.street().equalsIgnoreCase(a.getStreetAddress())
                        && d.city().equalsIgnoreCase(a.getCity())) {
                    return;
                }
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customer", Map.of("customerId", customerId));
        body.put("streetAddress", d.street());
        body.put("city", d.city());
        body.put("state", d.province());
        body.put("postalCode", d.postalCode());
        body.put("country", d.country());
        body.put("addressType", "SHIPPING");
        Api.post("/addresses", body);
    }
}
