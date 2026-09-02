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

	private Account createAndPersistAccount(User user, String accountNo) {
		Account account = Account.builder()
			.user(user)
			.bankCode("001")
			.bankName("싸피은행")
			.accountNo(accountNo)
			.financeUserKey("finance-user-key-123")
			.build();
		return em.persist(account);
	}

	@Test
	@DisplayName("findByUserIdAndDeletedAtIsNull_삭제되지않은_계좌만_조회된다")
	void findByUserIdAndDeletedAtIsNull_활성계좌만조회() {
		// given
		User user = createAndPersistUser("user1", "user1@example.com");
		Account active = createAndPersistAccount(user, "001-1234-5678");
		Account deleted = createAndPersistAccount(user, "001-9999-0000");
		deleted.markDeleted();
		em.flush();
		em.clear();

		// when
		List<Account> results = accountRepository.findByUserIdAndDeletedAtIsNull(user.getId());

		// then
		assertThat(results).hasSize(1);
		assertThat(results.get(0).getAccountNo()).isEqualTo("001-1234-5678");
	}

	@Test
	@DisplayName("existsByUserIdAndAccountNoAndDeletedAtIsNull_연동된_계좌가_존재하면_true를_반환한다")
	void existsByUserIdAndAccountNoAndDeletedAtIsNull_존재하면_true() {
		// given
		User user = createAndPersistUser("user2", "user2@example.com");
		createAndPersistAccount(user, "001-1234-5678");
		em.flush();
		em.clear();

		// when
		boolean result = accountRepository.existsByUserIdAndAccountNoAndDeletedAtIsNull(
			user.getId(), "001-1234-5678");

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("existsByUserIdAndAccountNoAndDeletedAtIsNull_삭제된_계좌는_false를_반환한다")
	void existsByUserIdAndAccountNoAndDeletedAtIsNull_삭제된계좌_false() {
		// given
		User user = createAndPersistUser("user3", "user3@example.com");
		Account account = createAndPersistAccount(user, "001-1234-5678");
		account.markDeleted();
		em.flush();
		em.clear();

		// when
		boolean result = accountRepository.existsByUserIdAndAccountNoAndDeletedAtIsNull(
			user.getId(), "001-1234-5678");

		// then
		assertThat(result).isFalse();
	}
}
