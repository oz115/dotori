package com.bana.dotori.account.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.account.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

	List<Account> findByUserId(Integer userId);

	boolean existsByUserIdAndAccountNumber(Integer userId, String accountNumber);

	Optional<Account> findFirstByUserId(Integer userId);
}
