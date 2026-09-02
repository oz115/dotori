package com.bana.dotori.wallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.wallet.entity.PointTransaction;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Integer> {

	/**
	 * 이 지갑에서 같은 멱등키로 이미 처리된 거래를 찾는다.
	 *
	 * <p>지갑으로 범위를 좁혀 조회해야 한다. 멱등키만으로 찾으면 다른 사용자가 우연히 같은
	 * {@code Idempotency-Key} 를 보냈을 때 남의 거래가 걸려 나와, 이번 요청이 반영되지 않은 채
	 * 남의 거래 정보를 성공 응답으로 돌려주게 된다.
	 */
	Optional<PointTransaction> findByWalletIdAndIdempotencyKey(Integer walletId, String idempotencyKey);

	List<PointTransaction> findAllByWalletIdOrderByPointTransactionIdAsc(Integer walletId);
}
