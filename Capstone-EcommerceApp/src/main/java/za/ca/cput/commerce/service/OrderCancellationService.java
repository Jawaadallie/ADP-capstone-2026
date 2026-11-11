package za.ca.cput.commerce.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ca.cput.commerce.domain.Invoice;
import za.ca.cput.commerce.domain.Notification;
import za.ca.cput.commerce.domain.Order;
import za.ca.cput.commerce.domain.OrderItem;
import za.ca.cput.commerce.domain.Shipment;
import za.ca.cput.commerce.repository.InventoryRepository;
import za.ca.cput.commerce.repository.InvoiceRepository;
import za.ca.cput.commerce.repository.NotificationRepository;
import za.ca.cput.commerce.repository.OrderRepository;
import za.ca.cput.commerce.repository.ShipmentRepository;

import java.time.LocalDateTime;


@Service
public class OrderCancellationService {

    public enum Result { CANCELLED, NOT_FOUND, NOT_YOURS, TOO_LATE }

    private final OrderRepository orders;
    private final ShipmentRepository shipments;
    private final InvoiceRepository invoices;
    private final InventoryRepository inventory;
    private final NotificationRepository notifications;

    public OrderCancellationService(OrderRepository orders,
                                    ShipmentRepository shipments,
                                    InvoiceRepository invoices,
                                    InventoryRepository inventory,
                                    NotificationRepository notifications) {
        this.orders = orders;
        this.shipments = shipments;
        this.invoices = invoices;
        this.inventory = inventory;
        this.notifications = notifications;
    }

    @Transactional
    public Result cancel(String orderId, String customerEmail) {

        Order order = orders.findById(orderId).orElse(null);
        if (order == null) {
            return Result.NOT_FOUND;
        }

        // Customers may only cancel their own orders
        if (!order.getCustomer().getEmail().equalsIgnoreCase(customerEmail)) {
            return Result.NOT_YOURS;
        }

        // Only an order that has not been shipped yet can still be cancelled
        Shipment shipment = shipments.findByOrderOrderId(orderId).orElse(null);
        String status = (shipment == null || shipment.getStatus() == null)
                ? "PROCESSING"
                : shipment.getStatus().trim().toUpperCase();
        if (!status.equals("PROCESSING")) {
            return Result.TOO_LATE;
        }

        // 1. Put the items back in stock
        for (OrderItem item : order.getOrderItems()) {
            inventory.addStock(item.getProduct().getProductId(), item.getQuantity());
        }

        // 2. Mark the shipment as cancelled
        if (shipment != null) {
            shipments.save(new Shipment.Builder().copy(shipment).setStatus("CANCELLED").build());
        }

        // 3. Mark the invoice as refunded
        Invoice invoice = invoices.findByOrderOrderId(orderId).orElse(null);
        if (invoice != null) {
            invoices.save(new Invoice.Builder().copy(invoice).setInvoiceStatus("REFUNDED").build());
        }

        // 4. Tell the customer
        String shortId = orderId.length() > 8 ? orderId.substring(0, 8).toUpperCase() : orderId.toUpperCase();
        notifications.save(new Notification.Builder()
                .setCustomer(order.getCustomer())
                .setMessage("Order " + shortId + " was cancelled. Your items were returned to stock "
                        + "and your payment will be refunded.")
                .setNotificationDate(LocalDateTime.now().withNano(0))
                .setStatus("UNREAD")
                .build());

        return Result.CANCELLED;
    }
}