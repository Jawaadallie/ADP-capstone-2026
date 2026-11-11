package za.ca.cput.commerce.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.ca.cput.commerce.domain.Order;
import za.ca.cput.commerce.domain.OrderItem;
import za.ca.cput.commerce.domain.Role;
import za.ca.cput.commerce.repository.CustomerRepository;
import za.ca.cput.commerce.repository.InventoryRepository;
import za.ca.cput.commerce.repository.OrderItemRepository;
import za.ca.cput.commerce.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final int LOW_STOCK_LIMIT = 10;   // this many or fewer in stock counts as "low"
    private static final int BEST_SELLERS = 5;
    private static final int RECENT_ORDERS = 8;

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final CustomerRepository customers;
    private final InventoryRepository inventory;

    public DashboardController(OrderRepository orders,
                               OrderItemRepository orderItems,
                               CustomerRepository customers,
                               InventoryRepository inventory) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.customers = customers;
        this.inventory = inventory;
    }

    public record Summary(double totalSales, int orderCount, double averageOrderValue, long customerCount) {}

    public record BestSeller(String productName, int unitsSold, double revenue) {}

    public record RecentOrder(String orderId, String customerName, LocalDateTime orderDate, double totalAmount) {}

    public record LowStock(String productName, int stock, String warehouse) {}

    public record DashboardData(Summary summary,
                                List<BestSeller> bestSellers,
                                List<RecentOrder> recentOrders,
                                List<LowStock> lowStock) {}

    @GetMapping
    public DashboardData dashboard() {

        // 1. Summary numbers
        // Canceled orders do not count as sales
        List<Order> allOrders = orders.findAll().stream()
                .filter(o -> !isCancelled(o))
                .toList();
        double totalSales = allOrders.stream().mapToDouble(Order::getTotalAmount).sum();
        int orderCount = allOrders.size();
        double average = orderCount == 0 ? 0 : totalSales / orderCount;
        long customerCount = customers.findAll().stream()
                .filter(c -> c.getRole() == Role.CUSTOMER)
                .count();

        Summary summary = new Summary(round(totalSales), orderCount, round(average), customerCount);

        // 2. Best sellers: group every order line by product, then rank by units sold
        Map<String, List<OrderItem>> linesByProduct = orderItems.findAll().stream()
                .filter(line -> !isCancelled(line.getOrder()))
                .collect(Collectors.groupingBy(line -> line.getProduct().getProductId()));

        List<BestSeller> bestSellers = linesByProduct.values().stream()
                .map(lines -> new BestSeller(
                        lines.get(0).getProduct().getProductName(),
                        lines.stream().mapToInt(OrderItem::getQuantity).sum(),
                        round(lines.stream().mapToDouble(OrderItem::getSubtotal).sum())))
                .sorted(Comparator.comparingInt(BestSeller::unitsSold).reversed())
                .limit(BEST_SELLERS)
                .toList();

        // 3. Recent orders: newest first
        List<RecentOrder> recentOrders = allOrders.stream()
                .filter(o -> o.getOrderDate() != null)
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())
                .limit(RECENT_ORDERS)
                .map(o -> new RecentOrder(
                        o.getOrderId(),
                        o.getCustomer().getName(),
                        o.getOrderDate(),
                        round(o.getTotalAmount())))
                .toList();

        // 4. Low stock: fewest first
        List<LowStock> lowStock = inventory.findAll().stream()
                .filter(i -> i.getStockQuantity() <= LOW_STOCK_LIMIT)
                .sorted(Comparator.comparingInt(i -> i.getStockQuantity()))
                .map(i -> new LowStock(
                        i.getProduct().getProductName(),
                        i.getStockQuantity(),
                        i.getWarehouseLocation()))
                .toList();

        return new DashboardData(summary, bestSellers, recentOrders, lowStock);
    }

    private static boolean isCancelled(Order order) {
        return order.getShipment() != null
                && "CANCELLED".equalsIgnoreCase(order.getShipment().getStatus());
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}