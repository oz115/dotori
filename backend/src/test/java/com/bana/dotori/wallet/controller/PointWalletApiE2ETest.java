package com.bana.dotori.wallet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.global.security.JwtTokenProvider;
import com.bana.dotori.mock.bank.MockBankTransferClient;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.wallet.entity.PointWallet;
import com.bana.dotori.wallet.repository.PointWalletRepository;
import com.bana.dotori.wallet.support.WalletTestFixture;

/**
 * E2E - HTTP 요청 하나가 컨트롤러부터 실제 DB 까지 끝까지 도는지 확인한다.
 *
 * <p>다른 테스트들은 계층을 하나씩 떼어 보지만, 여기서는 아무것도 가짜로 바꾸지 않는다
 * (은행만 목업인데, 그건 계좌 연동이 아직 없어서지 테스트 편의 때문이 아니다).
 * 계층 테스트가 모두 통과해도 배선이 어긋나 있으면 여기서만 깨진다.
 * 예를 들어 컨트롤러가 서비스를 잘못 호출하거나, 응답 필드명이 명세와 다른 경우다.
 *
 * <p>인증 필터를 켜고 실제 발급한 Access Token 으로 호출한다.
 * 요청 하나가 인증부터 DB 까지 실제로 도는지 보려는 것이 이 테스트의 목적이기 때문이다.
 * 권한 위반 케이스는 {@link PointWalletApiSecurityTest} 가 따로 검증한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("포인트 지갑 API E2E")
class PointWalletApiE2ETest {

	private static final String CHARGE_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/charge";
	private static final String GRANT_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/grant";
	private static final String BALANCE_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/balance";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private MockBankTransferClient mockBankTransferClient;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer parentId;
	private Integer familyId;
	private Integer childId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		mockBankTransferClient.clear();
		parentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-e2e");
		childId = WalletTestFixture.insertChild(jdbcTemplate, "child-e2e");
		familyId = WalletTestFixture.insertFamilyWithChild(jdbcTemplate, parentId, childId);
		pointWalletRepository.save(PointWallet.openWithBalance(childId, 5_000));
	}

	@AfterEach
	void tearDown() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		mockBankTransferClient.clear();
	}

	@Nested
	@DisplayName("충전")
	class Charge {

		@Test
		@DisplayName("요청 한 번으로 포인트·원장·계좌가 모두 맞물려 움직인다")
		void 충전_전_구간() throws Exception {
			mockBankTransferClient.registerAccount(childId, "김도토", 500_000);

			mockMvc.perform(post(CHARGE_URL, familyId, childId).header("Authorization", bearer(childId, UserRole.CHILD))
							.header("Idempotency-Key", "e2e-charge-1")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": 10000}"))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.transactionType").value("CHARGE"))
					// 응답의 amount 는 원장에 남는 값, 즉 발행된 포인트다. 요청한 현금이 아니다.
					.andExpect(jsonPath("$.amount").value(1_000))
					.andExpect(jsonPath("$.balanceAfter").value(6_000));

			assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(6_000);
			assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
			assertThat(mockBankTransferClient.findAccount(childId).balance())
					.as("포인트는 10분의 1만 늘지만 계좌에서는 요청한 현금 전액이 빠져야 한다")
					.isEqualTo(490_000);
		}

		@Test
		@DisplayName("10원 단위가 아니면 400 INVALID_CHARGE_UNIT 이다")
		void 십원_단위가_아니면_400이다() throws Exception {
			mockBankTransferClient.registerAccount(childId, "김도토", 500_000);

			mockMvc.perform(post(CHARGE_URL, familyId, childId).header("Authorization", bearer(childId, UserRole.CHILD))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": 1005}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_CHARGE_UNIT"));

			assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(5_000);
			assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
		}

		@Test
		@DisplayName("계좌 잔액이 부족하면 400 이 오고 DB 에 아무것도 남지 않는다")
		void 계좌_잔액_부족() throws Exception {
			mockBankTransferClient.registerAccount(childId, "김도토", 1_000);

			mockMvc.perform(post(CHARGE_URL, familyId, childId).header("Authorization", bearer(childId, UserRole.CHILD))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": 10000}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INSUFFICIENT_ACCOUNT_BALANCE"))
					.andExpect(jsonPath("$.timestamp").exists());

			assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(5_000);
			assertThat(WalletTestFixture.countTransactions(jdbcTemplate))
					.as("HTTP 로 들어와도 송금 실패 시 원장은 비어 있어야 한다")
					.isZero();
		}

		@Test
		@DisplayName("같은 멱등키로 두 번 호출해도 한 번만 반영된다")
		void 중복_요청() throws Exception {
			mockBankTransferClient.registerAccount(childId, "김도토", 500_000);

			for (int i = 0; i < 2; i++) {
				mockMvc.perform(post(CHARGE_URL, familyId, childId).header("Authorization", bearer(childId, UserRole.CHILD))
								.header("Idempotency-Key", "e2e-charge-dup")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"amount\": 10000}"))
						.andExpect(status().isCreated())
						.andExpect(jsonPath("$.balanceAfter").value(6_000));
			}

			assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(6_000);
			assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
			assertThat(mockBankTransferClient.findAccount(childId).balance())
					.as("중복 요청으로 돈이 두 번 빠지면 안 된다")
					.isEqualTo(490_000);
		}
	}

	@Nested
	@DisplayName("보유 포인트 조회")
	class Balance {

		@Test
		@DisplayName("충전·지급으로 움직인 잔액이 조회 결과와 일치한다")
		void 잔액이_거래와_일치한다() throws Exception {
			mockBankTransferClient.registerAccount(childId, "김도토", 500_000);

			// 10,000원 충전 → 1,000포인트, 그 뒤 3,000포인트 지급.
			// 조회가 원장과 어긋나면 여기서만 깨진다.
			mockMvc.perform(post(CHARGE_URL, familyId, childId)
							.header("Authorization", bearer(childId, UserRole.CHILD))
							.header("Idempotency-Key", "e2e-balance-charge")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": 10000}"))
					.andExpect(status().isCreated());

			mockMvc.perform(post(GRANT_URL, familyId, childId)
							.header("Authorization", bearer(parentId, UserRole.PARENT))
							.header("Idempotency-Key", "e2e-balance-grant")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": 3000}"))
					.andExpect(status().isCreated());

			int expected = 5_000 + 1_000 + 3_000;

			mockMvc.perform(get(BALANCE_URL, familyId, childId)
							.header("Authorization", bearer(childId, UserRole.CHILD)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.childId").value(childId))
					.andExpect(jsonPath("$.balance").value(expected))
					.andExpect(jsonPath("$.updatedAt").exists());

			assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(expected);
		}

		@Test
		@DisplayName("부모가 조회해도 아이가 조회한 것과 같은 잔액이 나온다")
		void 부모_조회와_본인_조회가_같다() throws Exception {
			mockMvc.perform(get(BALANCE_URL, familyId, childId)
							.header("Authorization", bearer(parentId, UserRole.PARENT)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.balance").value(5_000));
		}

		@Test
		@DisplayName("지갑이 없는 아이도 200 과 잔액 0 을 받는다")
		void 지갑이_없으면_0이다() throws Exception {
			Integer freshChildId = WalletTestFixture.insertChild(jdbcTemplate, "child-no-wallet-e2e");
			WalletTestFixture.addFamilyMember(jdbcTemplate, familyId, freshChildId);

			mockMvc.perform(get(BALANCE_URL, familyId, freshChildId)
							.header("Authorization", bearer(freshChildId, UserRole.CHILD)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.balance").value(0))
					.andExpect(jsonPath("$.updatedAt").doesNotExist());
		}
	}

	@Nested
	@DisplayName("지급")
	class Grant {

		@Test
		@DisplayName("요청 한 번으로 포인트와 원장이 함께 남는다")
		void 지급_전_구간() throws Exception {
			mockMvc.perform(post(GRANT_URL, familyId, childId).header("Authorization", bearer(parentId, UserRole.PARENT))
							.header("Idempotency-Key", "e2e-grant-1")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"childId\": " + childId + ", \"amount\": 3000, \"description\": \"용돈\"}"))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.transactionType").value("GRANT"))
					.andExpect(jsonPath("$.balanceAfter").value(8_000));

			assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(8_000);
			assertThat(jdbcTemplate.queryForObject(
					"SELECT description FROM point_transactions", String.class))
					.isEqualTo("용돈");
		}

		@Test
		@DisplayName("다른 가족의 아이를 지정하면 403 이 오고 잔액이 그대로다")
		void 남의_집_아이() throws Exception {
			Integer strangerId = WalletTestFixture.insertChild(jdbcTemplate, "child-stranger-e2e");
			pointWalletRepository.save(PointWallet.openWithBalance(strangerId, 0));

			mockMvc.perform(post(GRANT_URL, familyId, strangerId).header("Authorization", bearer(parentId, UserRole.PARENT))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": 3000}"))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("NOT_FAMILY_CHILD"));

			assertThat(WalletTestFixture.selectBalance(jdbcTemplate, strangerId)).isZero();
			assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
		}

		@Test
		@DisplayName("경로와 본문의 childId 가 다르면 400 이다")
		void childId_불일치() throws Exception {
			mockMvc.perform(post(GRANT_URL, familyId, childId).header("Authorization", bearer(parentId, UserRole.PARENT))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"childId\": 99999, \"amount\": 3000}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

			assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
		}
	}


	/** 실제 발급된 Access Token 으로 호출한다. 인증 경로까지 함께 검증하기 위해서다. */
	private String bearer(Integer userId, UserRole role) {
		return "Bearer " + jwtTokenProvider.createAccessToken(userId, role);
	}
}
