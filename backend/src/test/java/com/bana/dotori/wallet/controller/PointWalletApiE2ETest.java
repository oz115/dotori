package com.bana.dotori.wallet.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

import com.bana.dotori.wallet.client.MockBankTransferClient;
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
 * <p>인증 필터는 꺼 둔다. SecurityConfig 가 회원가입 담당 브랜치에 있어 이 브랜치에는 없고,
 * 스프링 시큐리티 기본 설정이 모든 요청을 401 로 막기 때문이다.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("포인트 지갑 API E2E")
class PointWalletApiE2ETest {

	private static final String CHARGE_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/charge";
	private static final String GRANT_URL = "/api/v1/families/{familyId}/children/{childId}/wallet/grant";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PointWalletRepository pointWalletRepository;

	@Autowired
	private MockBankTransferClient mockBankTransferClient;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Integer familyId;
	private Integer childId;

	@BeforeEach
	void setUp() {
		WalletTestFixture.cleanUp(jdbcTemplate);
		mockBankTransferClient.clear();
		Integer parentId = WalletTestFixture.insertParent(jdbcTemplate, "parent-e2e");
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

			mockMvc.perform(post(CHARGE_URL, familyId, childId)
							.header("Idempotency-Key", "e2e-charge-1")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"amount\": 10000}"))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.transactionType").value("CHARGE"))
					.andExpect(jsonPath("$.amount").value(10_000))
					.andExpect(jsonPath("$.balanceAfter").value(15_000));

			assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(15_000);
			assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
			assertThat(mockBankTransferClient.findAccount(childId).balance())
					.as("포인트가 늘어난 만큼 실제 계좌에서 빠져야 한다")
					.isEqualTo(490_000);
		}

		@Test
		@DisplayName("계좌 잔액이 부족하면 400 이 오고 DB 에 아무것도 남지 않는다")
		void 계좌_잔액_부족() throws Exception {
			mockBankTransferClient.registerAccount(childId, "김도토", 1_000);

			mockMvc.perform(post(CHARGE_URL, familyId, childId)
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
				mockMvc.perform(post(CHARGE_URL, familyId, childId)
								.header("Idempotency-Key", "e2e-charge-dup")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"amount\": 10000}"))
						.andExpect(status().isCreated())
						.andExpect(jsonPath("$.balanceAfter").value(15_000));
			}

			assertThat(WalletTestFixture.selectBalance(jdbcTemplate, childId)).isEqualTo(15_000);
			assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isEqualTo(1);
			assertThat(mockBankTransferClient.findAccount(childId).balance())
					.as("중복 요청으로 돈이 두 번 빠지면 안 된다")
					.isEqualTo(490_000);
		}
	}

	@Nested
	@DisplayName("지급")
	class Grant {

		@Test
		@DisplayName("요청 한 번으로 포인트와 원장이 함께 남는다")
		void 지급_전_구간() throws Exception {
			mockMvc.perform(post(GRANT_URL, familyId, childId)
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

			mockMvc.perform(post(GRANT_URL, familyId, strangerId)
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
			mockMvc.perform(post(GRANT_URL, familyId, childId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"childId\": 99999, \"amount\": 3000}"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

			assertThat(WalletTestFixture.countTransactions(jdbcTemplate)).isZero();
		}
	}

	@Nested
	@DisplayName("인증·인가 (인증 기능 완성 후 활성화)")
	class Authorization {

		@Test
		@Disabled("SecurityConfig 와 JWT 필터가 아직 없다. 회원가입·로그인(S15P21A505-19 계열)이 dev 에 들어온 뒤 활성화한다.")
		@DisplayName("토큰 없이 호출하면 401 이어야 한다")
		void 미인증은_401이다() {
			// 인증 필터가 붙으면 addFilters = false 를 떼고 토큰 없는 요청이 401 인지 확인한다.
		}

		@Test
		@Disabled("역할 검증은 인증된 사용자를 알아야 가능하다. 인증 도입 후 활성화한다.")
		@DisplayName("아이가 지급 API 를 호출하면 403 이어야 한다")
		void 아이의_지급_호출은_403이다() {
			// 지급은 부모 전용이다. 지금은 호출자가 누구인지 알 수 없어 검증할 수 없다.
		}

		@Test
		@Disabled("본인 확인은 인증 도입 후 가능하다.")
		@DisplayName("다른 아이의 지갑에 충전하면 403 이어야 한다")
		void 남의_지갑_충전은_403이다() {
			// 충전은 경로의 childId 가 로그인한 본인인지 확인해야 한다.
		}
	}
}
