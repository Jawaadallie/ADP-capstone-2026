package za.ca.cput.commerce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.ca.cput.commerce.service.OrderCancellationService;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderCancelController {

    private final OrderCancellationService cancellation;

    public OrderCancelController(OrderCancellationService cancellation) {
        this.cancellation = cancellation;
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String orderId, Principal principal) {

        OrderCancellationService.Result result = cancellation.cancel(orderId, principal.getName());

        if (result == OrderCancellationService.Result.CANCELLED) {
            return ResponseEntity.noContent().build();
        }
        if (result == OrderCancellationService.Result.NOT_FOUND) {
            return error(HttpStatus.NOT_FOUND, "That order no longer exists.");
        }
        if (result == OrderCancellationService.Result.NOT_YOURS) {
            return error(HttpStatus.FORBIDDEN, "You can only cancel your own orders.");
        }
        return error(HttpStatus.CONFLICT,
                "This order can no longer be cancelled. Only orders that are still processing can be cancelled.");
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}