package com.bana.dotori.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.bana.dotori.account.entity.Account;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;

/**
 * {@code @DataJpaTest} 는 기본적으로 데이터소스를 임베디드 DB 로 갈아끼운다.
 * 팀이 테스트를 실제 PostgreSQL 로 돌리기로 해 임베디드 드라이버가 클래스패스에 없으므로,
 * 갈아끼우기를 끄고 application.yaml 의 접속 정보를 그대로 쓴다.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
