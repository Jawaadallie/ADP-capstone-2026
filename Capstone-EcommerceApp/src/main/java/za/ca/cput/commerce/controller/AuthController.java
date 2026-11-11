package za.ca.cput.commerce.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import za.ca.cput.commerce.domain.Customer;
import za.ca.cput.commerce.factory.CustomerFactory;
import za.ca.cput.commerce.repository.CustomerRepository;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Pattern;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 64;

    private final CustomerRepository customers;
    private final PasswordEncoder encoder;

    public AuthController(CustomerRepository customers, PasswordEncoder encoder) {
        this.customers = customers;
        this.encoder = encoder;
    }

    public record RegisterRequest(String name, String email, String phone, String password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        String name = clean(request.name());
        String email = clean(request.email()).toLowerCase();
        String phone = clean(request.phone());
        String password = request.password() == null ? "" : request.password();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Please fill in your name, email and phone number.");
        }
        if (!EMAIL.matcher(email).matches()) {
            return error(HttpStatus.BAD_REQUEST, "That email address does not look right.");
        }

        String passwordProblem = checkPassword(password);
        if (passwordProblem != null) {
            return error(HttpStatus.BAD_REQUEST, passwordProblem);
        }

        if (customers.existsByEmailIgnoreCase(email)) {
            return error(HttpStatus.CONFLICT, "An account with that email already exists. Try signing in.");
        }

        Customer customer = CustomerFactory.createCustomer(name, email, phone, encoder.encode(password));
        if (customer == null) {
            return error(HttpStatus.BAD_REQUEST, "The account details are not valid.");
        }

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(customers.save(customer));
        } catch (DataIntegrityViolationException e) {
            // Two people registering the same email at the same moment
            return error(HttpStatus.CONFLICT, "An account with that email already exists. Try signing in.");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Customer> me(Principal principal) {
        return customers.findByEmailIgnoreCase(principal.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    static String checkPassword(String password) {
        if (password.length() < MIN_PASSWORD) {
            return "Your password must be at least " + MIN_PASSWORD + " characters.";
        }
        if (password.length() > MAX_PASSWORD) {
            return "Your password must be at most " + MAX_PASSWORD + " characters.";
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            return "Your password must contain at least one letter and one number.";
        }
        return null;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
