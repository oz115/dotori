package com.bana.dotori.wallet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * 인증 필터를 켠 채로 도는 유일한 테스트.
 *
 * <p>다른 지갑 테스트는 {@code addFilters = false} 로 필터를 끄기 때문에
 * "지갑 API 가 정말 보호되고 있는가"를 확인할 수 없다. 그래서 이 클래스만 따로 두었다.
 * {@code @Nested} 로 두면 바깥 클래스의 설정을 물려받아 필터가 꺼진 채로 돌아
 * 아무것도 검증하지 못한다.
 *
 * <p>토큰은 {@link JwtTokenProvider} 로 직접 발급한다. 로그인 API 를 거치면
 * 비밀번호 인코딩까지 얽혀 이 테스트가 보려는 "권한 검증"과 무관한 실패가 섞인다.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("포인트 지갑 API 보호")
class PointWalletApiSecurityTest {

	private static final String CHARGE_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/charge";
	private static final String GRANT_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/grant";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer parentId;
	private Integer childId;
	private Integer familyId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		parentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-sec");
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-sec");
		familyId = WalletTestFixture.insertFamilyWithChild(jdbcTemplate, parentId, childId);
		pointWalletRepository.save(PointWallet.openWithBalance(childId, 5_000));
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
	}

	@Test
	@DisplayName("보호_미인증_충전은_401이다")
	void 보호_미인증_충전은_401이다() throws Exception {
		mockMvc.perform(post(CHARGE_URL, familyId, childId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 1000}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("보호_미인증_지급은_401이다")
	void 보호_미인증_지급은_401이다() throws Exception {
		mockMvc.perform(post(GRANT_URL, familyId, childId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 1000}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("보호_남의_지갑에_충전하면_403이다")
	void 보호_남의_지갑에_충전하면_403이다() throws Exception {
		Integer otherChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-other-sec");
		pointWalletRepository.save(PointWallet.openWithBalance(otherChildId, 0));

		// 충전은 아이 본인 계좌에서 돈이 빠져나간다. 남이 대신 부를 수 있으면
		// 다른 사람 계좌에서 출금시키는 셈이 된다.
		mockMvc.perform(post(CHARGE_URL, familyId, otherChildId)
						.header("Authorization", bearer(childId, UserRole.CHILD))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 1000}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, otherChildId)).isZero();
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
	}

	@Test
	@DisplayName("보호_아이가_지급을_호출하면_403이다")
	void 보호_아이가_지급을_호출하면_403이다() throws Exception {
		// 지급은 부모 전용이다. 아이가 스스로에게 포인트를 발행할 수 있으면 안 된다.
		mockMvc.perform(post(GRANT_URL, familyId, childId)
						.header("Authorization", bearer(childId, UserRole.CHILD))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 5000}"))
				.andExpect(status().isForbidden());

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId))
				.as("아이가 스스로 발행하면 잔액이 늘어난다. 막혔으면 그대로여야 한다")
				.isEqualTo(5_000);
		assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
	}

	@Test
	@DisplayName("보호_다른_가족_부모가_지급하면_403이다")
	void 보호_다른_가족_부모가_지급하면_403이다() throws Exception {
		Integer otherParentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-other-sec");

		mockMvc.perform(post(GRANT_URL, familyId, childId)
						.header("Authorization", bearer(otherParentId, UserRole.PARENT))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 5000}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(5_000);
	}

	@Test
	@DisplayName("보호_가족_부모가_지급하면_통과한다")
	void 보호_가족_부모가_지급하면_통과한다() throws Exception {
		mockMvc.perform(post(GRANT_URL, familyId, childId)
						.header("Authorization", bearer(parentId, UserRole.PARENT))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 5000, \"description\": \"용돈\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.transactionType").value("GRANT"));

		assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(10_000);
	}

	private String bearer(Integer userId, UserRole role) {
		return "Bearer " + jwtTokenProvider.createAccessToken(userId, role);
	}
}
