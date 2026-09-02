package com.bana.dotori.account.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bana.dotori.account.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {

	List<Account> findByUserIdAndDeletedAtIsNull(Integer userId);

	boolean existsByUserIdAndAccountNoAndDeletedAtIsNull(Integer userId, String accountNo);
}
