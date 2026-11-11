package za.ca.cput.commerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/error").permitAll()

                        // Owner only
                        .requestMatchers("/api/staff", "/api/staff/**").hasRole("OWNER")
                        .requestMatchers("/api/dashboard", "/api/dashboard/**").hasRole("OWNER")
                        .requestMatchers("/api/customers", "/api/customers/**").hasRole("OWNER")

                        // A customer cancels their own order (the service checks it is theirs)
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/cancel").authenticated()

                        // Staff and owner only: products and stock
                        .requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products", "/api/products/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.POST, "/api/inventory", "/api/inventory/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.PUT, "/api/inventory", "/api/inventory/**").hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/inventory", "/api/inventory/**").hasAnyRole("STAFF", "OWNER")

                        // Staff and owner only: listing everyone's records
                        .requestMatchers(HttpMethod.GET, "/api/orders", "/api/order-items", "/api/cards",
                                "/api/payments", "/api/invoices", "/api/shipments",
                                "/api/notifications", "/api/addresses", "/api/reviews")
                        .hasAnyRole("STAFF", "OWNER")

                        // Staff and owner only: changing or deleting existing records
                        .requestMatchers(HttpMethod.PUT, "/api/orders/**", "/api/order-items/**", "/api/cards/**",
                                "/api/payments/**", "/api/invoices/**", "/api/notifications/**",
                                "/api/addresses/**", "/api/reviews/**")
                        .hasAnyRole("STAFF", "OWNER")
                        .requestMatchers(HttpMethod.DELETE, "/api/order-items/**", "/api/cards/**",
                                "/api/payments/**", "/api/invoices/**", "/api/shipments/**",
                                "/api/notifications/**", "/api/addresses/**", "/api/reviews/**")
                        .hasAnyRole("STAFF", "OWNER")

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
