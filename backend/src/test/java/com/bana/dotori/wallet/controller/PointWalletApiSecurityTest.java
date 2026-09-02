package com.bana.dotori.wallet.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 인증 필터를 켠 채로 도는 유일한 테스트.
 *
 * <p>다른 지갑 테스트는 {@code addFilters = false} 로 필터를 끄기 때문에
 * "지갑 API 가 정말 보호되고 있는가"를 확인할 수 없다. 그래서 이 클래스만 따로 두었다.
 * {@code @Nested} 로 두면 바깥 클래스의 {@code addFilters = false} 를 물려받아
 * 필터가 꺼진 채로 돌아 아무것도 검증하지 못한다.
 *
 * <p>역할 검증(부모만 지급, 본인만 충전)은 아직 못 한다. SecurityConfig 는 들어왔지만
 * 로그인·토큰 발급(AUTH-02)이 없어 인증된 사용자를 만들 수단이 없기 때문이다.
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

	@Test
	@DisplayName("인증 없이 충전을 호출하면 막힌다")
	void 미인증_충전은_막힌다() throws Exception {
		// SecurityConfig 가 formLogin·httpBasic 을 모두 끄고 AuthenticationEntryPoint 를 두지 않아
		// 미인증 요청이 401 이 아니라 403 으로 나간다. API 명세는 401 UNAUTHORIZED 를 정의하고 있어
		// 인증 기능을 완성할 때 엔트리포인트를 붙이고 이 기대값을 401 로 바꿔야 한다.
		mockMvc.perform(post(CHARGE_URL, 1, 2)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 10000}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("인증 없이 지급을 호출하면 막힌다")
	void 미인증_지급은_막힌다() throws Exception {
		mockMvc.perform(post(GRANT_URL, 1, 2)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\": 5000}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@Disabled("역할 검증은 인증된 사용자를 만들 수 있어야 가능하다. 로그인·토큰 발급(AUTH-02) 도입 후 활성화한다.")
	@DisplayName("아이가 지급 API 를 호출하면 403 이어야 한다")
	void 아이의_지급_호출은_403이다() {
		// 지급은 부모 전용이다. 지금은 호출자가 누구인지 알 수 없어 검증할 수 없다.
	}

	@Test
	@Disabled("본인 확인은 인증된 사용자를 만들 수 있어야 가능하다. 로그인·토큰 발급(AUTH-02) 도입 후 활성화한다.")
	@DisplayName("다른 아이의 지갑에 충전하면 403 이어야 한다")
	void 남의_지갑_충전은_403이다() {
		// 충전은 경로의 childId 가 로그인한 본인인지 확인해야 한다.
	}
}
