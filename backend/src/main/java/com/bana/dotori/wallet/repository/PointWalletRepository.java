package com.bana.dotori.wallet.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bana.dotori.wallet.entity.PointWallet;

public interface PointWalletRepository extends JpaRepository<PointWallet, Integer> {

	Optional<PointWallet> findByUserId(Integer userId);

	/**
	 * 잔액을 바꾸기 전에 지갑 행을 {@code SELECT ... FOR UPDATE} 로 잠근다.
	 *
	 * <p>같은 지갑에 대한 동시 요청이 이 지점에서 한 줄로 세워지기 때문에
	 * 잔액이 음수가 되거나 중복 반영되는 것을 막을 수 있다.
	 * 멱등성 확인도 반드시 이 락을 잡은 뒤에 해야 한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from PointWallet w where w.userId = :userId")
	Optional<PointWallet> findByUserIdForUpdate(@Param("userId") Integer userId);

	boolean existsByUserId(Integer userId);
}
