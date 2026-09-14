package com.bana.dotori.investment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.investment.entity.InvestmentProduct;
import com.bana.dotori.investment.entity.InvestmentProductCategory;

public interface InvestmentProductRepository extends JpaRepository<InvestmentProduct, Integer> {

	List<InvestmentProduct> findAllByOrderByIdAsc();

	List<InvestmentProduct> findAllByCategoryOrderByIdAsc(InvestmentProductCategory category);
}
