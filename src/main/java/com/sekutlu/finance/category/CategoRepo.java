package com.sekutlu.finance.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoRepo extends JpaRepository<Category, Long> {
    java.util.Optional<Category> findByName(String name);
}
