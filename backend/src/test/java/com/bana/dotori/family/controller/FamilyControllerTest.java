package com.bana.dotori.family.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.dto.FamilyMemberResponse;
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;
import com.bana.dotori.family.service.FamilyService;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.global.config.SecurityConfig;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.global.security.JwtAccessDeniedHandler;
import com.bana.dotori.global.security.JwtAuthenticationEntryPoint;
import com.bana.dotori.global.security.JwtAuthenticationFilter;
import com.bana.dotori.global.security.JwtTokenProvider;

@WebMvcTest(FamilyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
	JwtAccessDeniedHandler.class})
class FamilyControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FamilyService familyService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	private static final String FAMILIES_URL = "/api/v1/families";
	private static final String PARENT_TOKEN = "parent-access-token";
	private static final String BEARER_PARENT = "Bearer " + PARENT_TOKEN;

	private static final String CREATE_BODY = """
		{
			"name": "도토리네 가족"
		}
		""";

	private void givenParentAuthenticated(Integer userId) {
		given(jwtTokenProvider.getAuthentication(PARENT_TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
	}

	@Test
	@DisplayName("가족 생성 성공 시 201과 생성된 가족 정보를 반환한다")
	void create_success() throws Exception {
		givenParentAuthenticated(1);
		FamilyCreateResponse response = new FamilyCreateResponse(1, "도토리네 가족", 1,
			OffsetDateTime.of(2026, 8, 28, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
		given(familyService.create(eq(1), any(FamilyCreateRequest.class))).willReturn(response);

		mockMvc.perform(post(FAMILIES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CREATE_BODY))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.familyId").value(1))
			.andExpect(jsonPath("$.name").value("도토리네 가족"))
			.andExpect(jsonPath("$.createdBy").value(1))
			.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	@DisplayName("부모 권한이 아니면 403과 FORBIDDEN 에러를 반환한다")
	void create_notParent() throws Exception {
		givenParentAuthenticated(2);
		given(familyService.create(eq(2), any(FamilyCreateRequest.class)))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(post(FAMILIES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CREATE_BODY))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("이미 가족에 소속되어 있으면 409와 ALREADY_IN_FAMILY 에러를 반환한다")
	void create_alreadyInFamily() throws Exception {
		givenParentAuthenticated(1);
		given(familyService.create(eq(1), any(FamilyCreateRequest.class)))
			.willThrow(new BusinessException(ErrorCode.ALREADY_IN_FAMILY));

		mockMvc.perform(post(FAMILIES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CREATE_BODY))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_IN_FAMILY"));
	}

	@Test
	@DisplayName("name이 비어 있으면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void create_invalidRequest() throws Exception {
		givenParentAuthenticated(1);

		mockMvc.perform(post(FAMILIES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(familyService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("가족 조회 성공 시 200과 가족 정보를 반환한다")
	void getFamily_success() throws Exception {
		givenParentAuthenticated(1);
		FamilyResponse response = new FamilyResponse(1, "도토리네 가족", 1, 3,
			OffsetDateTime.of(2026, 8, 28, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
		given(familyService.getFamily(1, 1)).willReturn(response);

		mockMvc.perform(get(FAMILIES_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.familyId").value(1))
			.andExpect(jsonPath("$.name").value("도토리네 가족"))
			.andExpect(jsonPath("$.createdBy").value(1))
			.andExpect(jsonPath("$.memberCount").value(3))
			.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	@DisplayName("소속된 가족이 없으면 404와 FAMILY_NOT_FOUND 에러를 반환한다")
	void getFamily_notFound() throws Exception {
		givenParentAuthenticated(1);
		given(familyService.getFamily(1, 99))
			.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		mockMvc.perform(get(FAMILIES_URL + "/99")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FAMILY_NOT_FOUND"));
	}

	@Test
	@DisplayName("구성원 조회 성공 시 200과 구성원 목록을 반환한다")
	void getMembers_success() throws Exception {
		givenParentAuthenticated(1);
		FamilyMembersResponse response = new FamilyMembersResponse(List.of(
			new FamilyMemberResponse(1, "김도토", UserRole.PARENT, null,
				OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.ofHours(9))),
			new FamilyMemberResponse(2, "김도랑", UserRole.CHILD, null,
				OffsetDateTime.of(2026, 8, 28, 11, 0, 0, 0, ZoneOffset.ofHours(9)))));
		given(familyService.getMembers(1, 1)).willReturn(response);

		mockMvc.perform(get(FAMILIES_URL + "/1/members")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.members.length()").value(2))
			.andExpect(jsonPath("$.members[0].userId").value(1))
			.andExpect(jsonPath("$.members[0].name").value("김도토"))
			.andExpect(jsonPath("$.members[0].role").value("PARENT"))
			.andExpect(jsonPath("$.members[0].joinedAt").exists())
			.andExpect(jsonPath("$.members[1].role").value("CHILD"));
	}

	@Test
	@DisplayName("소속된 가족이 없으면 구성원 조회 시 404와 FAMILY_NOT_FOUND 에러를 반환한다")
	void getMembers_notFound() throws Exception {
		givenParentAuthenticated(1);
		given(familyService.getMembers(1, 99))
			.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		mockMvc.perform(get(FAMILIES_URL + "/99/members")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FAMILY_NOT_FOUND"));
	}

	@Test
	@DisplayName("Access Token 없이 요청하면 401과 UNAUTHORIZED 에러를 반환한다")
	void create_withoutAccessToken() throws Exception {
		mockMvc.perform(post(FAMILIES_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(CREATE_BODY))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		then(familyService).shouldHaveNoInteractions();
	}
}
