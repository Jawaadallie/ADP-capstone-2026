package za.ca.cput.commerce.service.impl;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import za.ca.cput.commerce.domain.Customer;
import za.ca.cput.commerce.repository.CustomerRepository;


@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository repository;

    public CustomerUserDetailsService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        String lookup = email == null ? "" : email.trim();

        Customer customer = repository.findByEmailIgnoreCase(lookup)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

        String hash = customer.getPasswordHash();
        if (hash == null || hash.isBlank()) {
            // Account from before passwords existed: it cannot sign in
            throw new UsernameNotFoundException("Invalid email or password");
        }

        return User.withUsername(customer.getEmail())
                .password(hash)
                .roles(customer.getRole().name())
                .build();
    }
}
