package za.ca.cput.commerce.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import za.ca.cput.commerce.domain.Customer;
import za.ca.cput.commerce.domain.Product;
import za.ca.cput.commerce.domain.Role;
import za.ca.cput.commerce.factory.CustomerFactory;
import za.ca.cput.commerce.factory.InventoryFactory;
import za.ca.cput.commerce.repository.CustomerRepository;
import za.ca.cput.commerce.repository.InventoryRepository;
import za.ca.cput.commerce.repository.ProductRepository;

import java.util.List;
import java.util.Optional;
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_EMAIL = "demo@legendarysprays.co.za";
    private static final String DEMO_PASSWORD = "Demo@1234";

    private static final String OWNER_EMAIL = "owner@legendarysprays.co.za";
    private static final String OWNER_PASSWORD = "Owner@1234";

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CustomerRepository customerRepository,
                      ProductRepository productRepository,
                      InventoryRepository inventoryRepository,
                      PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // name, description, price (rand), stock
    private static final Object[][] PRODUCTS = {
            {"All-Purpose Power Spray", "Cuts through grease and grime on any hard surface. 750 ml.", 49.99, 40},
            {"Glass & Mirror Shine", "Streak-free shine for windows, mirrors and screens. 500 ml.", 39.99, 35},
            {"Kitchen Degreaser", "Heavy-duty spray that lifts baked-on grease from stoves and extractor fans. 750 ml.", 59.99, 30},
            {"Bathroom Limescale Remover", "Dissolves limescale and soap scum on taps, tiles and shower doors. 500 ml.", 54.99, 25},
            {"Hospital-Grade Disinfectant", "Kills 99.9% of germs and bacteria on contact. 500 ml.", 69.99, 50},
            {"Fabric & Room Freshener", "Fresh linen scent that removes odours from couches, curtains and carpets. 400 ml.", 44.99, 45},
            {"Leather Care Conditioner", "Cleans, softens and protects leather seats and furniture. 300 ml.", 79.99, 20},
            {"Car Interior Detailer", "Safe on dashboards, door panels and plastics, with a matt finish. 500 ml.", 64.99, 28}
    };

    @Override
    public void run(String... args) {

        // 1. A demo customer so the shop can be used without registering first
        Optional<Customer> existingDemo = customerRepository.findByEmailIgnoreCase(DEMO_EMAIL);
        if (existingDemo.isEmpty()) {
            Customer demo = CustomerFactory.createCustomer(
                    "Demo Customer", DEMO_EMAIL, "0821234567",
                    passwordEncoder.encode(DEMO_PASSWORD));
            if (demo != null) {
                customerRepository.save(demo);
            }
        } else if (existingDemo.get().getPasswordHash() == null) {
            // Demo account created before passwords existed
            customerRepository.updatePasswordHash(
                    existingDemo.get().getCustomerId(), passwordEncoder.encode(DEMO_PASSWORD));
        }

        // 1b. The store owner (can manage products, staff and see sales)
        if (customerRepository.findByEmailIgnoreCase(OWNER_EMAIL).isEmpty()) {
            Customer owner = CustomerFactory.createCustomer("Store Owner", OWNER_EMAIL, "0829876543",
                    passwordEncoder.encode(OWNER_PASSWORD), Role.OWNER);

            if (owner != null){
                customerRepository.save(owner);
            }
        }

        // 2. The product range
        for (Object[] row : PRODUCTS) {
            String name = (String) row[0];

            if (!productRepository.existsByProductNameIgnoreCase(name)) {
                Product product = new Product.Builder()
                        .setProductName(name)
                        .setDescription((String) row[1])
                        .setCurrentPrice((Double) row[2])
                        .build();
                productRepository.save(product);
            }
        }

        // 3. Every product needs a stock record (the shop reads stock per product)
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            if (!inventoryRepository.existsByProduct(product)) {
                inventoryRepository.save(
                        InventoryFactory.createInventory(product, 30, "Cape Town Warehouse"));
            }
        }
    }
}
