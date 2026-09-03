package com.bana.dotori.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import com.bana.dotori.account.entity.Account;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;

@DataJpaTest
class AccountRepositoryTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private AccountRepository accountRepository;

	private User createAndPersistUser(String loginId, String email) {
		User user = User.builder()
			.loginId(loginId)
			.password("encoded-password")
			.name("테스트유저")
			.email(email)
			.role(UserRole.PARENT)
			.build();
		return em.persist(user);
	}

	private Account createAndPersistAccount(User user, String accountNumber) {
		Account account = Account.builder()
			.user(user)
			.bankCode("001")
			.bankName("싸피은행")
			.accountNumber(accountNumber)
			.financeUserKey("finance-user-key-123")
			.build();
		return em.persist(account);
	}

	@Test
	@DisplayName("findByUserId_해당_유저의_연동_계좌를_모두_조회한다")
	void findByUserId_유저계좌조회() {
		// given
		User user = createAndPersistUser("user1", "user1@example.com");
		createAndPersistAccount(user, "001-1234-5678");
		createAndPersistAccount(user, "001-9999-0000");
		User other = createAndPersistUser("user9", "user9@example.com");
		createAndPersistAccount(other, "001-0000-0000");
		em.flush();
		em.clear();

		// when
		List<Account> results = accountRepository.findByUserId(user.getId());

		// then
		assertThat(results).hasSize(2);
		assertThat(results).extracting(Account::getAccountNumber)
			.containsExactlyInAnyOrder("001-1234-5678", "001-9999-0000");
	}

	@Test
	@DisplayName("existsByUserIdAndAccountNumber_연동된_계좌가_존재하면_true를_반환한다")
	void existsByUserIdAndAccountNumber_존재하면_true() {
		// given
		User user = createAndPersistUser("user2", "user2@example.com");
		createAndPersistAccount(user, "001-1234-5678");
		em.flush();
		em.clear();

		// when
		boolean result = accountRepository.existsByUserIdAndAccountNumber(user.getId(), "001-1234-5678");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("existsByUserIdAndAccountNumber_연동되지_않은_계좌번호면_false를_반환한다")
	void existsByUserIdAndAccountNumber_없으면_false() {
		// given
		User user = createAndPersistUser("user3", "user3@example.com");
		createAndPersistAccount(user, "001-1234-5678");
		em.flush();
		em.clear();

		// when
		boolean result = accountRepository.existsByUserIdAndAccountNumber(user.getId(), "001-0000-9999");

		// then
		assertThat(result).isFalse();
	}
}
