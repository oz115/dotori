package com.bana.dotori.investment.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.investment.entity.InvestmentNewsProduct;

public interface InvestmentNewsProductRepository extends JpaRepository<InvestmentNewsProduct, Integer> {

	/**
	 * 여러 뉴스의 관련 종목 연결을 종목까지 fetch join 으로 한 번에 가져온다.
	 * 뉴스 목록에서 뉴스마다 따로 조회하면 N+1 이라 id 묶음으로 조회해 서비스가 그룹핑한다.
	 */
	@Query("select np from InvestmentNewsProduct np join fetch np.product "
		+ "where np.news.id in :newsIds")
	List<InvestmentNewsProduct> findAllWithProductByNewsIds(@Param("newsIds") Collection<Integer> newsIds);
}
