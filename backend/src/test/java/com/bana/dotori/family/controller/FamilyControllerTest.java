package com.bana.dotori.family.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.bana.dotori.family.dto.FamilyConnectRequest;
import com.bana.dotori.family.dto.FamilyConnectResponse;
import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.dto.FamilyMemberResponse;
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;
import com.bana.dotori.family.dto.FamilyStatusResponse;
import com.bana.dotori.family.dto.FamilyUpdateRequest;
import com.bana.dotori.family.dto.FamilyUpdateResponse;
import com.bana.dotori.family.dto.InvitationCodeResponse;
import com.bana.dotori.family.entity.FinancialKnowledgeLevel;
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
	private static final String CHILD_TOKEN = "child-access-token";
	private static final String BEARER_CHILD = "Bearer " + CHILD_TOKEN;

	private static final String CREATE_BODY = """
		{
			"name": "도토리네 가족",
			"weeklyAllowance": 10000,
			"weeklyRewardLimit": 5000,
			"financialKnowledgeLevel": "BEGINNER"
		}
		""";

	private void givenParentAuthenticated(Integer userId) {
		given(jwtTokenProvider.getAuthentication(PARENT_TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_PARENT"))));
	}

	private void givenChildAuthenticated(Integer userId) {
		given(jwtTokenProvider.getAuthentication(CHILD_TOKEN))
			.willReturn(new UsernamePasswordAuthenticationToken(userId, null,
				List.of(new SimpleGrantedAuthority("ROLE_CHILD"))));
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
	@DisplayName("가족에 소속되어 있으면 200과 hasFamily=true, familyId를 반환한다")
	void getStatus_hasFamily() throws Exception {
		givenParentAuthenticated(1);
		given(familyService.getStatus(1)).willReturn(new FamilyStatusResponse(true, 3));

		mockMvc.perform(get(FAMILIES_URL + "/status")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hasFamily").value(true))
			.andExpect(jsonPath("$.familyId").value(3));
	}

	@Test
	@DisplayName("가족에 소속되어 있지 않으면 200과 hasFamily=false, familyId=null을 반환한다")
	void getStatus_noFamily() throws Exception {
		givenParentAuthenticated(1);
		given(familyService.getStatus(1)).willReturn(new FamilyStatusResponse(false, null));

		mockMvc.perform(get(FAMILIES_URL + "/status")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hasFamily").value(false))
			.andExpect(jsonPath("$.familyId").value(nullValue()));
	}

	@Test
	@DisplayName("name이 비어 있으면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void create_invalidRequest() throws Exception {
		givenParentAuthenticated(1);

		mockMvc.perform(post(FAMILIES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name": "", "weeklyAllowance": 10000, "weeklyRewardLimit": 5000,
						"financialKnowledgeLevel": "BEGINNER"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(familyService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("온보딩 값이 누락되면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void create_missingOnboardingSettings() throws Exception {
		givenParentAuthenticated(1);

		mockMvc.perform(post(FAMILIES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"도토리네 가족\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(familyService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("온보딩 금액이 정책(1000원 이상·1000원 단위)에 어긋나면 400과 INVALID_REQUEST 에러를 반환한다")
	void create_invalidAmountPolicy() throws Exception {
		givenParentAuthenticated(1);
		// 금액 정책 검증은 DTO 가 아니라 서비스(toPoints)가 전담한다
		given(familyService.create(eq(1), any(FamilyCreateRequest.class)))
			.willThrow(new BusinessException(ErrorCode.INVALID_REQUEST));

		mockMvc.perform(post(FAMILIES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name": "도토리네 가족", "weeklyAllowance": 0, "weeklyRewardLimit": 5000,
						"financialKnowledgeLevel": "BEGINNER"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	@DisplayName("금융 지식 수준에 허용되지 않은 값이 오면 400과 INVALID_REQUEST 에러를 반환한다")
	void create_invalidKnowledgeLevel() throws Exception {
		givenParentAuthenticated(1);

		mockMvc.perform(post(FAMILIES_URL)
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name": "도토리네 가족", "weeklyAllowance": 10000, "weeklyRewardLimit": 5000,
						"financialKnowledgeLevel": "EXPERT"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(familyService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("가족 설정 수정 성공 시 200과 수정된 설정(원 단위)을 반환한다")
	void updateFamily_success() throws Exception {
		givenParentAuthenticated(1);
		FamilyUpdateResponse response = new FamilyUpdateResponse(1, "새 가족 이름", 20000, 5000,
			FinancialKnowledgeLevel.ADVANCED, OffsetDateTime.of(2026, 9, 9, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
		given(familyService.updateFamily(eq(1), eq(1), any(FamilyUpdateRequest.class))).willReturn(response);

		mockMvc.perform(patch(FAMILIES_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name": "새 가족 이름", "weeklyAllowance": 20000, "financialKnowledgeLevel": "ADVANCED"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.familyId").value(1))
			.andExpect(jsonPath("$.name").value("새 가족 이름"))
			.andExpect(jsonPath("$.weeklyAllowance").value(20000))
			.andExpect(jsonPath("$.weeklyRewardLimit").value(5000))
			.andExpect(jsonPath("$.financialKnowledgeLevel").value("ADVANCED"))
			.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	@DisplayName("가족 설정 수정: 이름이 공백뿐이면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void updateFamily_blankName() throws Exception {
		givenParentAuthenticated(1);

		mockMvc.perform(patch(FAMILIES_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"   \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(familyService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("가족 설정 수정: 부모 권한이 아니면 403과 FORBIDDEN 에러를 반환한다")
	void updateFamily_notParent() throws Exception {
		givenParentAuthenticated(2);
		given(familyService.updateFamily(eq(2), eq(1), any(FamilyUpdateRequest.class)))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(patch(FAMILIES_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"새 이름\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("가족 설정 수정: 구성원이 아니면 404와 FAMILY_NOT_FOUND 에러를 반환한다")
	void updateFamily_notMember() throws Exception {
		givenParentAuthenticated(1);
		given(familyService.updateFamily(eq(1), eq(9), any(FamilyUpdateRequest.class)))
			.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		mockMvc.perform(patch(FAMILIES_URL + "/9")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"새 이름\"}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FAMILY_NOT_FOUND"));
	}

	@Test
	@DisplayName("가족 조회 성공 시 200과 가족 정보를 반환한다")
	void getFamily_success() throws Exception {
		givenParentAuthenticated(1);
		FamilyResponse response = new FamilyResponse(1, "도토리네 가족", 1, 3, 10000, 5000,
			FinancialKnowledgeLevel.BEGINNER, OffsetDateTime.of(2026, 8, 28, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
		given(familyService.getFamily(1, 1)).willReturn(response);

		mockMvc.perform(get(FAMILIES_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.familyId").value(1))
			.andExpect(jsonPath("$.name").value("도토리네 가족"))
			.andExpect(jsonPath("$.createdBy").value(1))
			.andExpect(jsonPath("$.memberCount").value(3))
			.andExpect(jsonPath("$.weeklyAllowance").value(10000))
			.andExpect(jsonPath("$.weeklyRewardLimit").value(5000))
			.andExpect(jsonPath("$.financialKnowledgeLevel").value("BEGINNER"))
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
	@DisplayName("가족 ID가 숫자가 아니면 400과 INVALID_REQUEST 에러를 반환한다")
	void getFamily_invalidFamilyId() throws Exception {
		givenParentAuthenticated(1);

		mockMvc.perform(get(FAMILIES_URL + "/not-a-number")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(familyService).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("구성원 조회 성공 시 200과 구성원 목록을 반환한다")
	void getMembers_success() throws Exception {
		givenParentAuthenticated(1);
		FamilyMembersResponse response = new FamilyMembersResponse(List.of(
			new FamilyMemberResponse(1, "김도토", UserRole.PARENT,
				OffsetDateTime.of(2026, 8, 28, 10, 0, 0, 0, ZoneOffset.ofHours(9))),
			new FamilyMemberResponse(2, "김도랑", UserRole.CHILD,
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
	@DisplayName("가족 해체 성공 시 204를 반환한다")
	void deleteFamily_success() throws Exception {
		givenParentAuthenticated(1);

		mockMvc.perform(delete(FAMILIES_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isNoContent());

		then(familyService).should().deleteFamily(1, 1);
	}

	@Test
	@DisplayName("부모 권한이 아니면 가족 해체 시 403과 FORBIDDEN 에러를 반환한다")
	void deleteFamily_notParent() throws Exception {
		givenParentAuthenticated(2);
		doThrow(new BusinessException(ErrorCode.FORBIDDEN)).when(familyService).deleteFamily(2, 1);

		mockMvc.perform(delete(FAMILIES_URL + "/1")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("자녀 제외 성공 시 204를 반환한다")
	void removeChild_success() throws Exception {
		givenParentAuthenticated(1);

		mockMvc.perform(delete(FAMILIES_URL + "/1/children/2")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isNoContent());

		then(familyService).should().removeChild(1, 1, 2);
	}

	@Test
	@DisplayName("부모 권한이 아니면 자녀 제외 시 403과 FORBIDDEN 에러를 반환한다")
	void removeChild_notParent() throws Exception {
		givenParentAuthenticated(2);
		doThrow(new BusinessException(ErrorCode.FORBIDDEN)).when(familyService).removeChild(2, 1, 3);

		mockMvc.perform(delete(FAMILIES_URL + "/1/children/3")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("해당 자녀가 가족에 없으면 404와 MEMBER_NOT_FOUND 에러를 반환한다")
	void removeChild_memberNotFound() throws Exception {
		givenParentAuthenticated(1);
		doThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND)).when(familyService).removeChild(1, 1, 99);

		mockMvc.perform(delete(FAMILIES_URL + "/1/children/99")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
	}

	@Test
	@DisplayName("초대 코드 생성 성공 시 201과 코드·만료 시각을 반환한다")
	void createInvitationCode_success() throws Exception {
		givenParentAuthenticated(1);
		InvitationCodeResponse response = new InvitationCodeResponse(1, "A3B7K9",
			OffsetDateTime.of(2026, 9, 2, 15, 10, 0, 0, ZoneOffset.ofHours(9)));
		given(familyService.createInvitationCode(1, 1)).willReturn(response);

		mockMvc.perform(post(FAMILIES_URL + "/1/invitations")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.familyId").value(1))
			.andExpect(jsonPath("$.code").value("A3B7K9"))
			.andExpect(jsonPath("$.expiresAt").exists());
	}

	@Test
	@DisplayName("부모 권한이 아니면 초대 코드 생성 시 403과 FORBIDDEN 에러를 반환한다")
	void createInvitationCode_notParent() throws Exception {
		givenParentAuthenticated(2);
		given(familyService.createInvitationCode(2, 1))
			.willThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(post(FAMILIES_URL + "/1/invitations")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@DisplayName("소속된 가족이 없으면 초대 코드 생성 시 404와 FAMILY_NOT_FOUND 에러를 반환한다")
	void createInvitationCode_familyNotFound() throws Exception {
		givenParentAuthenticated(1);
		given(familyService.createInvitationCode(1, 99))
			.willThrow(new BusinessException(ErrorCode.FAMILY_NOT_FOUND));

		mockMvc.perform(post(FAMILIES_URL + "/99/invitations")
				.header(HttpHeaders.AUTHORIZATION, BEARER_PARENT))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FAMILY_NOT_FOUND"));
	}

	@Test
	@DisplayName("가족 연결 성공 시 200과 가족 정보를 반환한다")
	void connect_success() throws Exception {
		givenChildAuthenticated(2);
		FamilyConnectResponse response = new FamilyConnectResponse(1, "도토리네 가족",
			OffsetDateTime.of(2026, 9, 2, 15, 0, 0, 0, ZoneOffset.ofHours(9)));
		given(familyService.connect(eq(2), any(FamilyConnectRequest.class))).willReturn(response);

		mockMvc.perform(post(FAMILIES_URL + "/connect")
				.header(HttpHeaders.AUTHORIZATION, BEARER_CHILD)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\": \"A3B7K9\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.familyId").value(1))
			.andExpect(jsonPath("$.familyName").value("도토리네 가족"))
			.andExpect(jsonPath("$.joinedAt").exists());
	}

	@Test
	@DisplayName("유효하지 않은 코드로 연결하면 404와 INVALID_FAMILY_CODE 에러를 반환한다")
	void connect_invalidCode() throws Exception {
		givenChildAuthenticated(2);
		given(familyService.connect(eq(2), any(FamilyConnectRequest.class)))
			.willThrow(new BusinessException(ErrorCode.INVALID_FAMILY_CODE));

		mockMvc.perform(post(FAMILIES_URL + "/connect")
				.header(HttpHeaders.AUTHORIZATION, BEARER_CHILD)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\": \"WRONG9\"}"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("INVALID_FAMILY_CODE"));
	}

	@Test
	@DisplayName("이미 가족에 소속된 자녀가 연결하면 409와 ALREADY_IN_FAMILY 에러를 반환한다")
	void connect_alreadyInFamily() throws Exception {
		givenChildAuthenticated(2);
		given(familyService.connect(eq(2), any(FamilyConnectRequest.class)))
			.willThrow(new BusinessException(ErrorCode.ALREADY_IN_FAMILY));

		mockMvc.perform(post(FAMILIES_URL + "/connect")
				.header(HttpHeaders.AUTHORIZATION, BEARER_CHILD)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\": \"A3B7K9\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_IN_FAMILY"));
	}

	@Test
	@DisplayName("code가 비어 있으면 400과 INVALID_REQUEST 에러를 반환하고 서비스는 호출되지 않는다")
	void connect_blankCode() throws Exception {
		givenChildAuthenticated(2);

		mockMvc.perform(post(FAMILIES_URL + "/connect")
				.header(HttpHeaders.AUTHORIZATION, BEARER_CHILD)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\": \"\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		then(familyService).shouldHaveNoInteractions();
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
