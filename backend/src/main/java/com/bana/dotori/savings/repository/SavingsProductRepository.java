package com.bana.dotori.savings.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.savings.entity.SavingsProduct;
import com.bana.dotori.savings.entity.SavingsProductType;

public interface SavingsProductRepository extends JpaRepository<SavingsProduct, Integer> {

	Optional<SavingsProduct> findByProductType(SavingsProductType productType);

	List<SavingsProduct> findByActiveIsTrue();
}
