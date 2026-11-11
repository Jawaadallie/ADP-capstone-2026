package za.ca.cput.commerce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import za.ca.cput.commerce.domain.Customer;
import za.ca.cput.commerce.domain.Role;
import za.ca.cput.commerce.factory.CustomerFactory;
import za.ca.cput.commerce.repository.CustomerRepository;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CustomerRepository customers;
    private final PasswordEncoder encoder;

    public StaffController(CustomerRepository customers, PasswordEncoder encoder) {
        this.customers = customers;
        this.encoder = encoder;
    }

    public record StaffRequest(String name, String email, String phone, String password) {}

    public record StaffInfo(String customerId, String name, String email, String phone) {}

    @GetMapping
    public List<StaffInfo> list() {
        return customers.findByRole(Role.STAFF).stream()
                .map(StaffController::toInfo)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StaffRequest request) {

        String name = clean(request.name());
        String email = clean(request.email()).toLowerCase();
        String phone = clean(request.phone());
        String password = request.password() == null ? "" : request.password();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Please fill in the name, email and phone number.");
        }
        if (!EMAIL.matcher(email).matches()) {
            return error(HttpStatus.BAD_REQUEST, "That email address does not look right.");
        }

        String passwordProblem = AuthController.checkPassword(password);
        if (passwordProblem != null) {
            return error(HttpStatus.BAD_REQUEST, passwordProblem);
        }

        if (customers.existsByEmailIgnoreCase(email)) {
            return error(HttpStatus.CONFLICT, "An account with that email already exists.");
        }

        Customer staff = CustomerFactory.createCustomer(
                name, email, phone, encoder.encode(password), Role.STAFF);
        if (staff == null) {
            return error(HttpStatus.BAD_REQUEST, "The account details are not valid.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toInfo(customers.save(staff)));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<?> remove(@PathVariable String customerId) {

        Customer account = customers.findById(customerId).orElse(null);
        if (account == null) {
            return error(HttpStatus.NOT_FOUND, "That account no longer exists.");
        }
        if (account.getRole() != Role.STAFF) {
            return error(HttpStatus.BAD_REQUEST, "Only staff accounts can be removed here.");
        }

        customers.updateRole(customerId, Role.CUSTOMER);
        return ResponseEntity.noContent().build();
    }

    private static StaffInfo toInfo(Customer customer) {
        return new StaffInfo(customer.getCustomerId(), customer.getName(),
                customer.getEmail(), customer.getPhone());
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}