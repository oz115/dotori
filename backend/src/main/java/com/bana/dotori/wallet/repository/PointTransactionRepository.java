package com.bana.dotori.wallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.wallet.domain.PointTransaction;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Integer> {

	Optional<PointTransaction> findByIdempotencyKey(String idempotencyKey);

	List<PointTransaction> findAllByWalletIdOrderByPointTransactionIdAsc(Integer walletId);
}
