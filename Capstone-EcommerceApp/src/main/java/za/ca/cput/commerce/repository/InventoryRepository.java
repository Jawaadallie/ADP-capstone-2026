package za.ca.cput.commerce.repository;

/*
Author: Plamedie 230082629

 */

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
import za.ca.cput.commerce.domain.Inventory;
import za.ca.cput.commerce.domain.Product;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

    Optional<Inventory> findByProduct_ProductId(String productId);

    boolean existsByProduct(Product product);

    @Transactional
    @Modifying
    @Query("update Inventory i set i.stockQuantity = i.stockQuantity + :quantity where i.product.productId = :productId")
    int addStock(@Param("productId") String productId, @Param("quantity") int quantity);

    @Transactional
    @Modifying
    @Query("update Inventory i set i.stockQuantity = i.stockQuantity - :quantity where i.product.productId = :productId and i.stockQuantity >= :quantity")
    int reduceStock(@Param("productId") String productId, @Param("quantity") int quantity);

}
