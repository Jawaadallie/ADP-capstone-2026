package za.ca.cput.commerce.repository;

/*
Author: 222709006 Qhama dyushu

 */

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import za.ca.cput.commerce.domain.Customer;
import za.ca.cput.commerce.domain.Role;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Transactional
    @Modifying
    @Query("update Customer c set c.passwordHash = :hash where c.customerId = :id")
    int updatePasswordHash(@Param("id") String id, @Param("hash") String hash);

    List<Customer> findByRole(Role role);

    @Transactional
    @Modifying
    @Query("update Customer c set c.role = :role where c.customerId = :id")
    int updateRole(@Param("id") String id, @Param("role") Role role);

    List<Customer> findByNameContainingIgnoreCase(String name);
}
